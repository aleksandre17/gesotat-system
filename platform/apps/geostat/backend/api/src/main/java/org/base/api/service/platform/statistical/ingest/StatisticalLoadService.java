package org.base.api.service.platform.statistical.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.canonical.CanonicalJson;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Actor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Governed load of a filled authoring file into a REVIEW_REQUIRED (candidate) dataset snapshot.
 *
 * Order of effects: (1) validate, nothing written; (2) keep the source bytes content-addressed in the object
 * store — the replay source; (3) one Data Plane transaction writes batch, artifact, load, snapshot, lineage
 * records, staged rows and canonical observations. A crash before (3) leaves an unreferenced immutable object the storage
 * sweep already handles; a crash inside (3) rolls back. Publication is never part of a load: the existing
 * quality, privacy and release gates decide that later.
 *
 * Idempotent per (dataset version, file digest): the same file for the same approved revision returns the first
 * receipt. A load is a full snapshot — the platform's native unit; incremental modes are refused, not guessed.
 */
public final class StatisticalLoadService {
    public static final String MODE_FULL_SNAPSHOT = "FULL_SNAPSHOT";
    /** Object key prefix of retained source files; follows the artifact key convention (lowercase segments, trailing slash). */
    public static final String OBJECT_PREFIX = "statistical-authoring/";

    public enum Outcome { LOADED, ALREADY_LOADED, REJECTED }

    public record Receipt(Outcome outcome, String revisionDigest, String fileSha256, Long batchId, Long datasetSnapshotId,
                          int rows, int observations, List<String> fileIssues, List<WideRowNormalizer.RowIssue> rowIssues, List<Long> quarantinedRows) { }

    public static final class UnsupportedMode extends RuntimeException {
        public UnsupportedMode(String mode) { super("load mode is not supported: " + mode + " (supported: " + MODE_FULL_SNAPSHOT + ")"); }
    }

    public static final class ImportForbidden extends RuntimeException { }

    private final ContractWorkflow workflow;
    private final ContractWorkflow.AccessDecision access;
    private final StatisticalRegistry registry;
    private final StatisticalBindingService bindings;
    private final CanonicalObservationWriter writer;
    private final ArtifactObjectStore objects;
    private final JdbcTemplate dataPlane;
    private final TransactionTemplate dataTransaction;
    private final ObjectMapper mapper;
    private final String statusConceptCode;
    private final long maxUploadBytes;
    private final AccessAuthoringAdapter adapter = new AccessAuthoringAdapter();

    public StatisticalLoadService(ContractWorkflow workflow, ContractWorkflow.AccessDecision access, StatisticalRegistry registry, StatisticalBindingService bindings,
                                  CanonicalObservationWriter writer, ArtifactObjectStore objects, JdbcTemplate dataPlane, TransactionTemplate dataTransaction,
                                  ObjectMapper mapper, String statusConceptCode, long maxUploadBytes) {
        this.workflow = workflow; this.access = access; this.registry = registry; this.bindings = bindings; this.writer = writer; this.objects = objects;
        this.dataPlane = dataPlane; this.dataTransaction = dataTransaction; this.mapper = mapper; this.statusConceptCode = statusConceptCode; this.maxUploadBytes = maxUploadBytes;
    }

    public Receipt load(Actor actor, String contractId, String mode, String fileName, InputStream upload) throws IOException {
        if (!MODE_FULL_SNAPSHOT.equals(mode)) throw new UnsupportedMode(mode);
        SemanticPlan plan = workflow.approvedPlan(actor, contractId);
        String product = workflow.get(actor, contractId).productCode();
        if (!access.allowed(actor, product, ContractWorkflow.Authority.IMPORT)) throw new ImportForbidden();

        Path directory = Files.createTempDirectory("stat-load-");
        try {
            Path file = directory.resolve("upload.accdb");
            String sha256 = copyBounded(upload, file);
            AccessAuthoringAdapter.ReadResult read;
            try { read = adapter.read(file.toFile(), plan); }
            catch (IOException | RuntimeException unreadable) { return rejected(plan, sha256, 0, List.of("NOT_A_READABLE_AUTHORING_FILE"), List.of()); }
            if (!read.accepted()) return rejected(plan, sha256, 0, read.issues(), List.of());

            StatisticalRegistry.Scope scope = new StatisticalRegistry.Scope(product);
            WideRowNormalizer.Result normalised = new WideRowNormalizer(plan,
                    (ref, code) -> registry.codelist(ref, scope).map(list -> list.codes().contains(code)).orElse(false), statusConceptCode).normalize(read.rows());
            if (!normalised.accepted()) return rejected(plan, sha256, read.rows().size(), List.of(), normalised.issues());

            StatisticalBindingService.Bound bound = bindings.bind(plan, product);
            List<Map<String, Object>> previous = dataPlane.queryForList("""
                    SELECT b.batch_id, s.dataset_snapshot_id, l.accepted_count FROM ingest.artifact a
                    JOIN ingest.batch b ON b.batch_id=a.batch_id JOIN ingest.dataset_load l ON l.batch_id=b.batch_id
                    JOIN publication.dataset_snapshot s ON s.dataset_load_id=l.dataset_load_id
                    WHERE a.checksum=? AND l.dataset_version_id=?""", sha256, bound.datasetVersionId());
            if (!previous.isEmpty())
                return new Receipt(Outcome.ALREADY_LOADED, plan.revisionDigest(), sha256, ((Number) previous.get(0).get("batch_id")).longValue(),
                        ((Number) previous.get(0).get("dataset_snapshot_id")).longValue(), read.rows().size(), normalised.observations().size(), List.of(), normalised.issues(),
                        normalised.quarantinedRows().stream().sorted().toList());

            ArtifactObjectStore.ObjectLocation stored;
            try (InputStream in = Files.newInputStream(file)) {
                stored = objects.putContentAddressed(OBJECT_PREFIX, sha256, "accdb", "application/msaccess", in, Files.size(file));
            }
            String objectUri = "s3://" + stored.bucket() + "/" + stored.key();
            String safeName = fileName == null || fileName.isBlank() ? "authoring.accdb" : fileName.replaceAll("[^\\p{L}\\p{N}._ -]", "_");
            String tableName = plan.physical().tables().get(0).name();

            return dataTransaction.execute(status -> {
                long batchId = insert("INSERT INTO ingest.batch(contract_id,product_id,status,checksum,started_at,finished_at) VALUES(?,?,'LOADED',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                        "SELECT MAX(batch_id) FROM ingest.batch WHERE checksum=? AND contract_id=?", List.of(bound.ingestionContractId(), bound.productId(), sha256), List.of(sha256, bound.ingestionContractId()));
                long artifactId = insert("INSERT INTO ingest.artifact(batch_id,original_name,format,object_uri,checksum,byte_size) VALUES(?,?,?,?,?,?)",
                        "SELECT MAX(artifact_id) FROM ingest.artifact WHERE batch_id=?", List.of(batchId, safeName, "ACCDB", objectUri, sha256, size(file)), List.of(batchId));
                int accepted = read.rows().size() - normalised.quarantinedRows().size();
                long loadId = insert("INSERT INTO ingest.dataset_load(batch_id,dataset_version_id,source_name,source_row_count,accepted_count,rejected_count,status) VALUES(?,?,?,?,?,?,'LOADED')",
                        "SELECT MAX(dataset_load_id) FROM ingest.dataset_load WHERE batch_id=?", List.of(batchId, bound.datasetVersionId(), tableName, read.rows().size(), accepted, normalised.quarantinedRows().size()), List.of(batchId));
                long snapshotId = insert("INSERT INTO publication.dataset_snapshot(dataset_load_id,dataset_version_id,status,row_count,checksum) VALUES(?,?,'REVIEW_REQUIRED',?,?)",
                        "SELECT dataset_snapshot_id FROM publication.dataset_snapshot WHERE dataset_load_id=?", List.of(loadId, bound.datasetVersionId(), accepted, sha256), List.of(loadId)); // row_count counts source rows, as every governed snapshot does

                Map<Long, Long> lineage = new HashMap<>();
                for (int i = 0; i < read.rows().size(); i++) {
                    long rowNumber = i + 1L;
                    String payload = json(new TreeMap<>(stringify(read.rows().get(i))));
                    String payloadHash = CanonicalJson.digest("geostat.stat-source-row.v1", payload);
                    boolean quarantined = normalised.quarantinedRows().contains(rowNumber);
                    // Staged rows are the record the release gates measure: every source row is accounted for, valid or not.
                    dataPlane.update("INSERT INTO ingest.staged_row(dataset_load_id,source_row_number,source_key,raw_payload_json,payload_hash,validation_status,error_json) VALUES(?,?,?,?,?,?,?)",
                            loadId, rowNumber, tableName + "#" + rowNumber, payload, payloadHash, quarantined ? "REJECTED" : "VALID",
                            quarantined ? json(normalised.issues().stream().filter(issue -> issue.row() == rowNumber).toList()) : null);
                    if (quarantined) continue;
                    lineage.put(rowNumber, insert("INSERT INTO raw.source_record(dataset_snapshot_id,artifact_id,source_row_number,source_key,payload_json,payload_hash) VALUES(?,?,?,?,?,?)",
                            "SELECT MAX(source_record_id) FROM raw.source_record WHERE dataset_snapshot_id=? AND source_row_number=?",
                            List.of(snapshotId, artifactId, rowNumber, tableName + "#" + rowNumber, payload, payloadHash), List.of(snapshotId, rowNumber)));
                }
                writer.write(plan, normalised.observations(), bindings.binding(plan, bound), new CanonicalObservationWriter.LoadContext(snapshotId, lineage));
                return new Receipt(Outcome.LOADED, plan.revisionDigest(), sha256, batchId, snapshotId, read.rows().size(), normalised.observations().size(), List.of(),
                        normalised.issues(), normalised.quarantinedRows().stream().sorted().toList());
            });
        } finally {
            try (var paths = Files.walk(directory)) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } }); }
        }
    }

    private static Receipt rejected(SemanticPlan plan, String sha256, int rows, List<String> fileIssues, List<WideRowNormalizer.RowIssue> rowIssues) {
        return new Receipt(Outcome.REJECTED, plan.revisionDigest(), sha256, null, null, rows, 0, fileIssues, rowIssues, List.of());
    }

    private String copyBounded(InputStream upload, Path target) throws IOException {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream in = new DigestInputStream(upload, sha); var out = Files.newOutputStream(target)) {
                byte[] buffer = new byte[65536];
                long total = 0;
                for (int n; (n = in.read(buffer)) > 0; ) {
                    total += n;
                    if (total > maxUploadBytes) throw new org.base.api.service.platform.statistical.access.AuthoringFileService.FileTooLarge(maxUploadBytes);
                    out.write(buffer, 0, n);
                }
            }
            return HexFormat.of().formatHex(sha.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private long insert(String insert, String select, List<Object> insertArgs, List<Object> selectArgs) {
        dataPlane.update(insert, insertArgs.toArray());
        return dataPlane.queryForObject(select, Long.class, selectArgs.toArray());
    }

    private static long size(Path file) { try { return Files.size(file); } catch (IOException e) { throw new java.io.UncheckedIOException(e); } }

    private static Map<String, String> stringify(Map<String, Object> row) {
        Map<String, String> out = new HashMap<>();
        row.forEach((k, v) -> out.put(k, v == null ? null : v instanceof java.math.BigDecimal d ? d.toPlainString() : v.toString()));
        return out;
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
