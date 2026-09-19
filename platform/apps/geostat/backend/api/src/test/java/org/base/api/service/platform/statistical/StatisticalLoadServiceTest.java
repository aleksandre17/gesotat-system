package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter.CodeItem;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter;
import org.base.api.service.platform.statistical.ingest.StatisticalBindingService;
import org.base.api.service.platform.statistical.ingest.StatisticalLoadService;
import org.base.api.service.platform.statistical.ingest.StatisticalLoadService.Outcome;
import org.base.api.service.platform.statistical.ingest.StatisticalLoadService.Receipt;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Actor;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Authority;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.DraftRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Approved contract -> filled file -> governed PREPARED snapshot, through the existing dataset / metric / lineage model. */
class StatisticalLoadServiceTest {
    private static final Actor AUTHOR = new Actor("author"), APPROVER = new Actor("approver"), IMPORTER = new Actor("importer");
    private final InMemoryStatisticalRegistry registry = registry();
    private final Map<String, byte[]> objectStore = new HashMap<>();
    private JdbcTemplate jdbc;
    private ContractWorkflow workflow;
    private StatisticalLoadService loads;
    private String contractId;
    private SemanticPlan plan;
    @TempDir Path dir;

    @BeforeEach void wire() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer;CASE_INSENSITIVE_IDENTIFIERS=TRUE", true);
        jdbc = new JdbcTemplate(dataSource);
        for (String schema : new String[]{"platform", "ingest", "publication", "raw", "[statistics]"}) jdbc.execute("CREATE SCHEMA " + schema);
        for (String ddl : new String[]{
                "CREATE TABLE platform.data_product(product_id BIGINT IDENTITY PRIMARY KEY, product_code VARCHAR(120) UNIQUE)",
                "CREATE TABLE platform.contract_namespace(namespace_id BIGINT IDENTITY PRIMARY KEY, namespace_code VARCHAR(64) UNIQUE)",
                "CREATE TABLE platform.statistical_reference(reference_id BIGINT IDENTITY PRIMARY KEY, kind VARCHAR(16), namespace_id BIGINT, code VARCHAR(120), version_major INT, version_minor INT, version_patch INT, target_type VARCHAR(48), target_id BIGINT)",
                "CREATE TABLE platform.classification_item(classification_item_id BIGINT IDENTITY PRIMARY KEY, classification_version_id BIGINT, code VARCHAR(128))",
                "CREATE TABLE platform.dataset(dataset_id BIGINT IDENTITY PRIMARY KEY, product_id BIGINT NOT NULL, dataset_code VARCHAR(120) NOT NULL, dataset_family VARCHAR(24) NOT NULL, business_grain VARCHAR(500), lifecycle_status VARCHAR(24) NOT NULL, UNIQUE(product_id,dataset_code))",
                "CREATE TABLE platform.dataset_version(dataset_version_id BIGINT IDENTITY PRIMARY KEY, dataset_id BIGINT NOT NULL, version INT NOT NULL, status VARCHAR(24) NOT NULL, contract_checksum CHAR(64), UNIQUE(dataset_id,version))",
                "CREATE TABLE platform.source_system(source_system_id BIGINT IDENTITY PRIMARY KEY, source_code VARCHAR(120) UNIQUE, source_type VARCHAR(24) NOT NULL, title VARCHAR(255) NOT NULL, trust_level VARCHAR(24) NOT NULL, enabled BIT NOT NULL)",
                "CREATE TABLE platform.ingestion_contract(contract_id BIGINT IDENTITY PRIMARY KEY, source_system_id BIGINT NOT NULL, dataset_id BIGINT NOT NULL, format_profile VARCHAR(48) NOT NULL, ingestion_method VARCHAR(32) NOT NULL, auto_publish BIT NOT NULL, status VARCHAR(24) NOT NULL, contract_code VARCHAR(160) NOT NULL, UNIQUE(source_system_id,dataset_id))",
                "CREATE TABLE platform.dimension(dimension_id BIGINT IDENTITY PRIMARY KEY, dimension_code VARCHAR(120) NOT NULL UNIQUE, value_type VARCHAR(24) NOT NULL, title_ka VARCHAR(255) NOT NULL)",
                "CREATE TABLE platform.metric(metric_id BIGINT IDENTITY PRIMARY KEY, metric_code VARCHAR(120) NOT NULL UNIQUE, source_dataset_id BIGINT NOT NULL, measure_id BIGINT NOT NULL, aggregation VARCHAR(24) NOT NULL, status VARCHAR(24) NOT NULL)",
                "CREATE TABLE ingest.batch(batch_id BIGINT IDENTITY PRIMARY KEY, contract_id BIGINT NOT NULL, product_id BIGINT NOT NULL, status VARCHAR(24) NOT NULL, checksum CHAR(64), started_at TIMESTAMP, finished_at TIMESTAMP)",
                "CREATE TABLE ingest.artifact(artifact_id BIGINT IDENTITY PRIMARY KEY, batch_id BIGINT NOT NULL, original_name VARCHAR(512) NOT NULL, format VARCHAR(32) NOT NULL, object_uri VARCHAR(2048) NOT NULL, checksum CHAR(64) NOT NULL, byte_size BIGINT NOT NULL)",
                "CREATE TABLE ingest.dataset_load(dataset_load_id BIGINT IDENTITY PRIMARY KEY, batch_id BIGINT NOT NULL, dataset_version_id BIGINT NOT NULL, source_name VARCHAR(255) NOT NULL, source_row_count BIGINT, accepted_count BIGINT, rejected_count BIGINT, status VARCHAR(24) NOT NULL)",
                "CREATE TABLE publication.dataset_snapshot(dataset_snapshot_id BIGINT IDENTITY PRIMARY KEY, dataset_load_id BIGINT NOT NULL UNIQUE, dataset_version_id BIGINT NOT NULL, status VARCHAR(24) NOT NULL, row_count BIGINT NOT NULL, checksum CHAR(64) NOT NULL)",
                "CREATE TABLE raw.source_record(source_record_id BIGINT IDENTITY PRIMARY KEY, dataset_snapshot_id BIGINT NOT NULL, artifact_id BIGINT NOT NULL, source_row_number BIGINT NOT NULL, source_key VARCHAR(512), payload_json VARCHAR(4000) NOT NULL, payload_hash CHAR(64) NOT NULL)",
                "CREATE TABLE [statistics].series(series_id BIGINT IDENTITY PRIMARY KEY, dataset_snapshot_id BIGINT NOT NULL, metric_id BIGINT NOT NULL, series_key_hash CHAR(64) NOT NULL, unit_code VARCHAR(64), status VARCHAR(24) NOT NULL, source_record_id BIGINT, UNIQUE(dataset_snapshot_id,metric_id,series_key_hash))",
                "CREATE TABLE [statistics].observation(observation_id BIGINT IDENTITY PRIMARY KEY, series_id BIGINT NOT NULL, period_start DATE, period_end DATE, observation_status VARCHAR(24) DEFAULT 'VALID' NOT NULL, numeric_value DECIMAL(28,10), source_record_id BIGINT NOT NULL, is_current BIT DEFAULT 1 NOT NULL)",
                "CREATE TABLE [statistics].observation_dimension(observation_id BIGINT NOT NULL, dimension_id BIGINT NOT NULL, classification_item_id BIGINT, scalar_code VARCHAR(255), PRIMARY KEY(observation_id,dimension_id))",
                "CREATE TABLE [statistics].observation_attribute(observation_id BIGINT NOT NULL, attribute_code VARCHAR(128) NOT NULL, value_json VARCHAR(4000) NOT NULL, PRIMARY KEY(observation_id,attribute_code))"})
            jdbc.execute(ddl);
        jdbc.update("INSERT INTO platform.data_product(product_code) VALUES(?)", LABOUR.productCode());
        jdbc.update("INSERT INTO platform.contract_namespace(namespace_code) VALUES('SHARED')");
        long measureRow = 100;
        for (String code : new String[]{"EMPLOYED", "UNEMPLOYED"})
            jdbc.update("INSERT INTO platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch,target_type,target_id) VALUES('MEASURE',1,?,1,0,0,'MEASURE',?)", code, measureRow++);
        long version = 11;
        for (Map.Entry<String, Set<String>> list : Map.of("CL_AREA", Set.of("GE", "GE_TB", "GE_KA"), "CL_SEX", Set.of("F", "M", "_T"), "CL_OBS_STATUS", Set.of("A", "E", "M", "O", "P")).entrySet()) {
            jdbc.update("INSERT INTO platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch,target_type,target_id) VALUES('CODELIST',1,?,1,0,0,'CLASSIFICATION_VERSION',?)", list.getKey(), version);
            for (String code : list.getValue()) jdbc.update("INSERT INTO platform.classification_item(classification_version_id,code) VALUES(?,?)", version, code);
            version++;
        }

        Map<String, Set<Authority>> grants = Map.of("author", Set.of(Authority.AUTHOR), "approver", Set.of(Authority.APPROVE), "importer", Set.of(Authority.IMPORT));
        ContractWorkflow.AccessDecision access = (actor, product, authority) -> LABOUR.productCode().equals(product) && grants.getOrDefault(actor.subject(), Set.of()).contains(authority);
        workflow = new ContractWorkflow(new ContractWorkflowTest.InMemoryStore(), compiler(registry), access, new ContractWorkflow.Policy(false), () -> UUID.randomUUID().toString());
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        loads = new StatisticalLoadService(workflow, access, registry, new StatisticalBindingService(jdbc, transaction),
                new CanonicalObservationWriter(jdbc, transaction, new ObjectMapper()), store(), jdbc, transaction, new ObjectMapper(), "OBS_STATUS", 5_000_000);

        DraftRecord draft = workflow.create(AUTHOR, LABOUR.productCode(), labourDraft(), "k");
        DraftRecord review = workflow.submit(AUTHOR, draft.draftId(), draft.version());
        workflow.approve(APPROVER, draft.draftId(), review.version(), review.revisionDigest());
        contractId = draft.draftId();
        plan = compiler(registry).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
    }

    private ArtifactObjectStore store() {
        return new ArtifactObjectStore() {
            public String ingestBucket() { return "test-ingest"; }
            public Optional<ObjectStat> stat(ObjectLocation l) { return Optional.ofNullable(objectStore.get(l.key())).map(b -> new ObjectStat(b.length, "application/msaccess")); }
            public String sha256(ObjectLocation l) { throw new UnsupportedOperationException(); }
            public InputStream open(ObjectLocation l) { return new ByteArrayInputStream(objectStore.get(l.key())); }
            public byte[] read(ObjectLocation l, int max) { return objectStore.get(l.key()); }
            public ObjectLocation putContentAddressed(String prefix, String sha256, String extension, String mediaType, InputStream content, long byteSize) {
                String key = org.base.api.service.artifact.ArtifactKeys.contentKey(prefix, sha256, extension); // the real key rules, so a bad prefix fails here too
                try { objectStore.putIfAbsent(key, content.readAllBytes()); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
                return new ObjectLocation(ingestBucket(), key);
            }
            public URI presignGet(ObjectLocation l, Duration ttl, String name, String type) { throw new UnsupportedOperationException(); }
        };
    }

    private byte[] filled(Consumer<com.healthmarketscience.jackcess.Table> rows) throws Exception {
        Path file = dir.resolve(UUID.randomUUID() + ".accdb");
        Map<Ref, List<CodeItem>> codelists = new HashMap<>();
        for (Ref ref : List.of(CL_AREA, CL_SEX, CL_OBS_STATUS)) codelists.put(ref, registry.codelist(ref, LABOUR).orElseThrow().codes().stream().sorted().map(c -> new CodeItem(c, c)).toList());
        new AccessAuthoringAdapter().emit(plan, codelists, "ka", file.toFile());
        try (Database db = DatabaseBuilder.open(file.toFile())) { rows.accept(db.getTable(plan.physical().tables().get(0).name())); }
        return Files.readAllBytes(file);
    }

    private static void add(com.healthmarketscience.jackcess.Table table, Object... values) {
        try { table.addRow(values); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }

    private Receipt load(byte[] file) throws Exception { return loads.load(IMPORTER, contractId, "FULL_SNAPSHOT", "labour 2025.accdb", new ByteArrayInputStream(file)); }

    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }

    @Test void loadsThroughTheExistingGovernedModelAndNeverPublishes() throws Exception {
        byte[] file = filled(t -> { add(t, "2025", "GE_TB", "F", new BigDecimal("12000"), "P", new BigDecimal("1800"), null); add(t, "2025", "GE_TB", "M", new BigDecimal("13000"), null, null, "M"); });
        Receipt receipt = load(file);
        assertEquals(Outcome.LOADED, receipt.outcome());
        assertEquals(2, receipt.rows());
        assertEquals(4, receipt.observations());
        assertEquals("PREPARED", jdbc.queryForObject("SELECT status FROM publication.dataset_snapshot WHERE dataset_snapshot_id=?", String.class, receipt.datasetSnapshotId()), "a load never publishes");
        assertEquals("STATISTICAL", jdbc.queryForObject("SELECT dataset_family FROM platform.dataset", String.class));
        assertEquals(plan.revisionDigest(), jdbc.queryForObject("SELECT contract_checksum FROM platform.dataset_version", String.class), "the dataset version is the approved revision");
        assertEquals(2, count("platform.metric"));
        assertEquals(2, count("platform.dimension"), "time is not a stored dimension");
        assertEquals(2, count("raw.source_record"), "one lineage record per source row");
        assertEquals(4, count("[statistics].observation"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM [statistics].observation WHERE observation_status='M' AND numeric_value IS NULL", Integer.class));
        assertEquals(1, objectStore.size(), "the source bytes are kept for replay");
        assertTrue(jdbc.queryForObject("SELECT object_uri FROM ingest.artifact", String.class).startsWith("s3://test-ingest/statistical-authoring/" + receipt.fileSha256()));
    }

    @Test void sameFileIsLoadedOnceAndBindingIsStable() throws Exception {
        byte[] file = filled(t -> add(t, "2025", "GE", "F", new BigDecimal("1"), null, new BigDecimal("2"), null));
        Receipt first = load(file), again = load(file);
        assertEquals(Outcome.ALREADY_LOADED, again.outcome());
        assertEquals(first.datasetSnapshotId(), again.datasetSnapshotId());
        assertEquals(1, count("ingest.batch"));
        assertEquals(2, count("[statistics].observation"));

        Receipt next = load(filled(t -> add(t, "2026", "GE", "F", new BigDecimal("3"), null, new BigDecimal("4"), null)));
        assertEquals(Outcome.LOADED, next.outcome());
        assertNotEquals(first.datasetSnapshotId(), next.datasetSnapshotId(), "another file is another candidate snapshot");
        assertEquals(1, count("platform.dataset"));
        assertEquals(1, count("platform.dataset_version"));
        assertEquals(2, count("platform.metric"), "binding the same revision again adds nothing");
    }

    @Test void rejectedFileWritesNothingAnywhere() throws Exception {
        Receipt receipt = load(filled(t -> { add(t, "2025", "GE", "F", new BigDecimal("1"), null, new BigDecimal("2"), null); add(t, "2025-Q7", "XX", "F", new BigDecimal("1"), null, null, null); }));
        assertEquals(Outcome.REJECTED, receipt.outcome());
        assertEquals(3, receipt.rowIssues().size());
        for (String table : new String[]{"ingest.batch", "ingest.artifact", "publication.dataset_snapshot", "raw.source_record", "[statistics].observation", "platform.dataset"}) assertEquals(0, count(table), table);
        assertTrue(objectStore.isEmpty(), "a rejected file is not retained as a source");
        assertEquals(Outcome.REJECTED, loads.load(IMPORTER, contractId, "FULL_SNAPSHOT", "x", new ByteArrayInputStream("junk".getBytes())).outcome());
    }

    @Test void dutiesAndModesAreEnforced() throws Exception {
        byte[] file = filled(t -> add(t, "2025", "GE", "F", new BigDecimal("1"), null, new BigDecimal("2"), null));
        assertThrows(StatisticalLoadService.ImportForbidden.class, () -> loads.load(AUTHOR, contractId, "FULL_SNAPSHOT", "f", new ByteArrayInputStream(file)), "describing a contract is not loading data");
        for (String mode : new String[]{"UPSERT", "DELETE", "REPLACE_SCOPE", ""})
            assertThrows(StatisticalLoadService.UnsupportedMode.class, () -> loads.load(IMPORTER, contractId, mode, "f", new ByteArrayInputStream(file)), mode);
        DraftRecord pending = workflow.create(AUTHOR, LABOUR.productCode(), labourDraft(), "pending");
        assertThrows(ContractWorkflow.WorkflowException.class, () -> loads.load(IMPORTER, pending.draftId(), "FULL_SNAPSHOT", "f", new ByteArrayInputStream(file)), "only an approved contract admits data");
        assertEquals(0, count("ingest.batch"));
    }
}
