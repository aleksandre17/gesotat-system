package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.compiler.ContractIssue;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.JdbcStatisticalRegistry;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The JDBC adapter must be observationally identical to the reference registry: the same drafts compile to
 * the same digests through either port. The schema below is the portable subset of the Control-plane tables
 * the adapter reads (H2 cannot run the T-SQL migration; its structure is asserted separately).
 */
class JdbcStatisticalRegistryTest {
    private JdbcTemplate jdbc;
    private final Map<String, Long> namespaces = new HashMap<>();
    private final Map<String, Long> products = new HashMap<>();
    private final Map<String, Long> references = new HashMap<>();

    @BeforeEach void schema() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1"));
        jdbc.execute("CREATE SCHEMA platform");
        jdbc.execute("CREATE TABLE platform.contract_namespace(namespace_id BIGINT IDENTITY PRIMARY KEY, namespace_code VARCHAR(64) UNIQUE)");
        jdbc.execute("CREATE TABLE platform.data_product(product_id BIGINT IDENTITY PRIMARY KEY, product_code VARCHAR(120) UNIQUE)");
        jdbc.execute("""
                CREATE TABLE platform.statistical_reference(reference_id BIGINT IDENTITY PRIMARY KEY, kind VARCHAR(16), namespace_id BIGINT,
                  code VARCHAR(120), version_major INT, version_minor INT, version_patch INT, lifecycle_status VARCHAR(16),
                  owner_product_id BIGINT, target_type VARCHAR(48), target_id BIGINT)""");
        jdbc.execute("CREATE TABLE platform.statistical_unit(unit_id BIGINT IDENTITY PRIMARY KEY, unit_code VARCHAR(120))");
        jdbc.execute("""
                CREATE TABLE platform.measure(measure_id BIGINT IDENTITY PRIMARY KEY, measure_code VARCHAR(120), numeric_precision INT,
                  numeric_scale INT, approximate_numeric BIT DEFAULT 0, unit_id BIGINT, concept_reference_id BIGINT)""");
        jdbc.execute("CREATE TABLE platform.classification_item(classification_item_id BIGINT IDENTITY PRIMARY KEY, classification_version_id BIGINT, code VARCHAR(128), status VARCHAR(24))");
        jdbc.execute("""
                CREATE TABLE platform.statistical_component(component_id BIGINT IDENTITY PRIMARY KEY, dsd_id BIGINT, component_code VARCHAR(120),
                  component_role VARCHAR(24), component_order INT, required BIT, measure_id BIGINT, classification_version_id BIGINT,
                  attachment_level VARCHAR(24), constraint_json VARCHAR(4000), concept_reference_id BIGINT, representation_type VARCHAR(24))""");
        seed();
    }

    private long insert(String sql, Object... args) {
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    private long reference(String wire, Lifecycle lifecycle, String ownerProduct, String targetType, Long targetId) {
        Ref ref = Ref.parse(wire);
        long namespace = namespaces.computeIfAbsent(ref.namespace(), n -> insert("INSERT INTO platform.contract_namespace(namespace_code) VALUES(?)", n));
        Long owner = ownerProduct == null ? null : products.computeIfAbsent(ownerProduct, p -> insert("INSERT INTO platform.data_product(product_code) VALUES(?)", p));
        long id = insert("INSERT INTO platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch,lifecycle_status,owner_product_id,target_type,target_id) VALUES(?,?,?,?,?,?,?,?,?,?)",
                ref.kind().name(), namespace, ref.code(), ref.version().major(), ref.version().minor(), ref.version().patch(), lifecycle.name(), owner, targetType, targetId);
        references.put(wire, id);
        return id;
    }

    private void codelist(String wire, String owner, long versionId, String... codes) {
        reference(wire, Lifecycle.APPROVED, owner, "CLASSIFICATION_VERSION", versionId);
        for (String code : codes) jdbc.update("INSERT INTO platform.classification_item(classification_version_id,code,status) VALUES(?,?,'ACTIVE')", versionId, code);
        jdbc.update("INSERT INTO platform.classification_item(classification_version_id,code,status) VALUES(?,?,'RETIRED')", versionId, "RETIRED_CODE");
    }

    private void measure(String wire, Lifecycle lifecycle, String owner, String concept, String unit, Integer precision, Integer scale) {
        long unitId = jdbc.queryForObject("SELECT target_id FROM platform.statistical_reference WHERE reference_id=?", Long.class, references.get(unit));
        long id = insert("INSERT INTO platform.measure(measure_code,numeric_precision,numeric_scale,unit_id,concept_reference_id) VALUES(?,?,?,?,?)",
                wire, precision, scale, unitId, references.get(concept));
        reference(wire, lifecycle, owner, "MEASURE", id);
    }

    private void seed() {
        for (String identityOnly : new String[]{"profile:SHARED:STAT_AGGREGATE(1.0.0)", "concept:SHARED:TIME_PERIOD(1.0.0)", "concept:SHARED:REF_AREA(1.0.0)",
                "concept:SHARED:SEX(1.0.0)", "concept:SHARED:OBS_STATUS(1.0.0)", "concept:SHARED:EMPLOYED(1.0.0)", "concept:SHARED:UNEMPLOYED(1.0.0)",
                "concept:SHARED:SOURCE_NOTE(1.0.0)"})
            reference(identityOnly, Lifecycle.APPROVED, null, null, null);
        for (String landOnly : new String[]{"concept:LAND:LAND_USE(1.0.0)", "concept:LAND:AREA_SIZE(1.0.0)", "concept:LAND:SHARE(1.0.0)"})
            reference(landOnly, Lifecycle.APPROVED, LAND.productCode(), null, null);
        reference("policy:SHARED:QUALITY_BASELINE(1.0.0)", Lifecycle.APPROVED, null, "DATA_QUALITY_POLICY", 1L);
        for (String unit : new String[]{"unit:SHARED:PERSONS(1.0.0)", "unit:SHARED:HECTARE(1.0.0)", "unit:SHARED:PERCENT(1.0.0)"})
            reference(unit, Lifecycle.APPROVED, null, "STATISTICAL_UNIT", insert("INSERT INTO platform.statistical_unit(unit_code) VALUES(?)", unit));
        codelist("codelist:SHARED:CL_AREA(1.0.0)", null, 11, "GE", "GE_TB", "GE_KA");
        codelist("codelist:SHARED:CL_SEX(1.0.0)", null, 12, "F", "M", "_T");
        codelist("codelist:SHARED:CL_OBS_STATUS(1.0.0)", null, 13, "A", "E", "M", "O", "P");
        codelist("codelist:LAND:CL_LAND_USE(2.1.0)", LAND.productCode(), 14, "ARABLE", "FOREST", "URBAN");
        measure("measure:SHARED:EMPLOYED(1.0.0)", Lifecycle.APPROVED, null, "concept:SHARED:EMPLOYED(1.0.0)", "unit:SHARED:PERSONS(1.0.0)", 18, 0);
        measure("measure:SHARED:UNEMPLOYED(1.0.0)", Lifecycle.APPROVED, null, "concept:SHARED:UNEMPLOYED(1.0.0)", "unit:SHARED:PERSONS(1.0.0)", 18, 0);
        measure("measure:LAND:AREA_SIZE(1.0.0)", Lifecycle.APPROVED, LAND.productCode(), "concept:LAND:AREA_SIZE(1.0.0)", "unit:SHARED:HECTARE(1.0.0)", 28, 10);
        measure("measure:LAND:SHARE(1.0.0)", Lifecycle.PROPOSED, LAND.productCode(), "concept:LAND:SHARE(1.0.0)", "unit:SHARED:PERCENT(1.0.0)", 7, 4);
    }

    private JdbcStatisticalRegistry registry() { return new JdbcStatisticalRegistry(jdbc, new ObjectMapper()); }

    private static Set<ContractIssue.Code> codes(org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Result r) {
        return r.issues().stream().map(ContractIssue::code).collect(Collectors.toSet());
    }

    @Test void compilesToTheSameDigestsAsTheReferenceRegistry() {
        for (var draft : Map.of(labourDraft(), LABOUR, landDraft(false), LAND).entrySet()) {
            SemanticPlan expected = compiler(StatisticalContractFixtures.registry()).compile(draft.getKey(), draft.getValue(), Mode.APPROVAL).plan().orElseThrow();
            SemanticPlan actual = compiler(registry()).compile(draft.getKey(), draft.getValue(), Mode.APPROVAL).plan().orElseThrow();
            assertEquals(expected.semanticDigest(), actual.semanticDigest());
            assertEquals(expected.revisionDigest(), actual.revisionDigest());
        }
    }

    @Test void scopeLifecycleAndRetiredCodesBehaveAsThePortRequires() {
        assertEquals(Set.of(ContractIssue.Code.UNRESOLVED_REFERENCE), codes(compiler(registry()).compile(landDraft(false), LABOUR, Mode.DRAFT)), "foreign product");
        assertTrue(compiler(registry()).compile(landDraft(true), LAND, Mode.DRAFT).accepted(), "proposal previews");
        assertEquals(Set.of(ContractIssue.Code.REFERENCE_NOT_APPROVED), codes(compiler(registry()).compile(landDraft(true), LAND, Mode.APPROVAL)));
        assertFalse(registry().codelist(CL_AREA, LABOUR).orElseThrow().codes().contains("RETIRED_CODE"));
        assertTrue(registry().lifecycle(Ref.parse("measure:SHARED:EMPLOYED(2.0.0)"), LABOUR).isEmpty(), "another version is another entry");
    }

    @Test void incompleteOrAmbiguousEntriesFailClosed() {
        jdbc.update("UPDATE platform.measure SET numeric_precision = NULL WHERE measure_code = ?", "measure:SHARED:EMPLOYED(1.0.0)");
        assertTrue(registry().measure(Ref.parse("measure:SHARED:EMPLOYED(1.0.0)"), LABOUR).isEmpty(), "exact measure without an envelope");

        long personsUnit = jdbc.queryForObject("SELECT target_id FROM platform.statistical_reference WHERE reference_id=?", Long.class, references.get("unit:SHARED:PERSONS(1.0.0)"));
        reference("unit:SHARED:PERSONS(1.1.0)", Lifecycle.APPROVED, null, "STATISTICAL_UNIT", personsUnit);
        assertTrue(registry().measure(Ref.parse("measure:SHARED:UNEMPLOYED(1.0.0)"), LABOUR).isEmpty(), "two live versions of one unit row: no guess");
    }

    @Test void storedStructureResolvesAndCompilesLikeTheInlineOne() {
        long dsd = 77;
        reference("dsd:LABOUR:DSD_LABOUR(1.0.0)", Lifecycle.APPROVED, LABOUR.productCode(), "STATISTICAL_DSD", dsd);
        component(dsd, 1, "TIME_PERIOD", "DIMENSION", "concept:SHARED:TIME_PERIOD(1.0.0)", "TIME_PERIOD", null, null, "{\"formats\":[\"YEAR\",\"QUARTER\"]}", null);
        component(dsd, 2, "REF_AREA", "DIMENSION", "concept:SHARED:REF_AREA(1.0.0)", "CODED", 11L, null, null, null);
        component(dsd, 3, "SEX", "DIMENSION", "concept:SHARED:SEX(1.0.0)", "CODED", 12L, null, null, null);
        component(dsd, 4, "EMPLOYED", "MEASURE", null, null, null, null, null, "measure:SHARED:EMPLOYED(1.0.0)");
        component(dsd, 5, "UNEMPLOYED", "MEASURE", null, null, null, null, null, "measure:SHARED:UNEMPLOYED(1.0.0)");
        component(dsd, 6, "EMPLOYED_STATUS", "ATTRIBUTE", "concept:SHARED:OBS_STATUS(1.0.0)", "CODED", 13L, "MEASURE", "{\"attachment\":{\"measure\":\"EMPLOYED\"}}", null);
        component(dsd, 7, "UNEMPLOYED_STATUS", "ATTRIBUTE", "concept:SHARED:OBS_STATUS(1.0.0)", "CODED", 13L, "MEASURE", "{\"attachment\":{\"measure\":\"UNEMPLOYED\"}}", null);
        component(dsd, 8, "SOURCE_NOTE", "ATTRIBUTE", "concept:SHARED:SOURCE_NOTE(1.0.0)", "TEXT", null, "DATASET", "{\"maxLength\":200}", null);

        String byReference = labourDraft().replaceAll("(?s)\"structure\":\\{\"inline\":.*?\\]\\}\\},\\s*\"policyRefs\"",
                "\"structure\":{\"existingStructureRef\":\"dsd:LABOUR:DSD_LABOUR(1.0.0)\"},\"policyRefs\"");
        SemanticPlan stored = compiler(registry()).compile(byReference, LABOUR, Mode.APPROVAL).plan().orElseThrow();
        SemanticPlan inline = compiler(registry()).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
        assertEquals(inline.components(), stored.components(), "a stored DSD and the same inline DSD are one structure");
        assertTrue(stored.dependencies().contains(Ref.parse("dsd:LABOUR:DSD_LABOUR(1.0.0)")), "reuse pins the structure version");
        assertTrue(registry().structure(Ref.parse("dsd:LABOUR:DSD_LABOUR(1.0.0)"), LAND).isEmpty(), "not visible to another product");
    }

    private void component(long dsd, int order, String code, String role, String concept, String representationType, Long classificationVersion,
                           String attachmentLevel, String constraintJson, String measure) {
        Long measureId = measure == null ? null : jdbc.queryForObject("SELECT target_id FROM platform.statistical_reference WHERE reference_id=?", Long.class, references.get(measure));
        jdbc.update("INSERT INTO platform.statistical_component(dsd_id,component_code,component_role,component_order,required,measure_id,classification_version_id,attachment_level,constraint_json,concept_reference_id,representation_type) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                dsd, code, role, order, !"ATTRIBUTE".equals(role), measureId, classificationVersion, attachmentLevel == null ? "OBSERVATION" : attachmentLevel,
                constraintJson, concept == null ? null : references.get(concept), representationType);
    }

    // ---- migration structure (the script itself needs SQL Server; see the evidence record)

    private static String migration() throws Exception {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path file = dir.resolve(Path.of("core", "src", "main", "resources", "db", "platform", "106_statistical_reference_identity.sql"));
            if (Files.exists(file)) return Files.readString(file);
        }
        throw new IllegalStateException("migration not found");
    }

    @Test void migrationIsAdditiveIdempotentAndNeutral() throws Exception {
        String sql = migration(), upper = sql.toUpperCase(Locale.ROOT);
        for (String destructive : new String[]{"DROP TABLE", "DROP COLUMN", "DELETE FROM", "TRUNCATE", "ALTER COLUMN"}) assertFalse(upper.contains(destructive), destructive);
        assertTrue(sql.contains("IF OBJECT_ID(N'platform.statistical_reference',N'U') IS NULL"));
        assertTrue(sql.contains("IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.measure') AND name=N'unit_id')"));
        assertTrue(sql.contains("EXEC(N'ALTER TABLE platform.measure ADD CONSTRAINT fk_measure_unit"), "statements naming new columns are deferred");
        assertTrue(sql.contains("tr_statistical_reference_immutable") && sql.contains("APPROVED -> SUPERSEDED"));
        String withoutComments = sql.replaceAll("(?s)/\\*.*?\\*/", "").toUpperCase(Locale.ROOT);
        for (String literal : new String[]{"KIDS", "INSERT ", "MERGE "}) assertFalse(withoutComments.contains(literal), "no site literal and no seeded semantics: " + literal);
    }

    @Test void migrationIsRegisteredAfterItsPredecessor() throws Exception {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path runner = dir.resolve(Path.of("api", "src", "main", "java", "org", "base", "api", "service", "platform", "PlatformSchemaMigrationRunner.java"));
            if (!Files.exists(runner)) continue;
            String source = Files.readString(runner);
            int previous = source.indexOf("104_data_product_tenancy.sql"), mine = source.indexOf("executeAndRecord(control, \"db/platform/106_statistical_reference_identity.sql\")");
            assertTrue(previous > 0 && mine > previous);
            return;
        }
        fail("runner not found");
    }
}
