package org.base.api.service.artifact.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Data Plane persistence of package runs and their append-only stage history. */
@Repository
public class PackageRunRepository {
    private static final String COLUMNS = "package_run_id,artifact_manifest_id,dataset_version_id,status,next_stage_code,attempt,state_json,last_issue_code";

    private final JdbcTemplate dataPlane;
    private final ObjectMapper json;

    /** Contract identity an admitted manifest was bound to; empty for a manifest without a contract. */
    public record ManifestBinding(long manifestId, Long datasetVersionId) {}

    public record StageRecord(int attempt, String stageCode, String outcome, String issueCode, Map<String, Object> detail, Instant recordedAt) {}

    public PackageRunRepository(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane, ObjectMapper json) {
        this.dataPlane = dataPlane;
        this.json = json;
    }

    public Optional<ManifestBinding> manifest(long manifestId) {
        return dataPlane.query("SELECT artifact_manifest_id,dataset_version_id FROM ingest.artifact_manifest WHERE artifact_manifest_id=?",
                (rs, n) -> new ManifestBinding(rs.getLong(1), (Long) rs.getObject(2)), manifestId).stream().findFirst();
    }

    public Optional<PackageRun> find(long runId) {
        return dataPlane.query("SELECT " + COLUMNS + " FROM ingest.artifact_package_run WHERE package_run_id=?", mapper(), runId).stream().findFirst();
    }

    /** Holds the manifest's run key range so concurrent starts of one manifest return one run. */
    public Optional<PackageRun> lockByManifest(long manifestId) {
        return dataPlane.query("SELECT " + COLUMNS + " FROM ingest.artifact_package_run WITH (UPDLOCK, HOLDLOCK) WHERE artifact_manifest_id=?",
                mapper(), manifestId).stream().findFirst();
    }

    public long insert(long manifestId, long datasetVersionId, String firstStageCode, String requestedBy) {
        return dataPlane.queryForObject("INSERT INTO ingest.artifact_package_run(artifact_manifest_id,dataset_version_id,next_stage_code,requested_by)"
                + " OUTPUT inserted.package_run_id VALUES(?,?,?,?)", Long.class, manifestId, datasetVersionId, firstStageCode, requestedBy);
    }

    /** Runs a worker may advance: never started, or stopped by infrastructure at least {@code retryAfterSeconds} ago. */
    public List<Long> due(int limit, int retryAfterSeconds, int staleRunningSeconds) {
        return dataPlane.queryForList("SELECT TOP (?) package_run_id FROM ingest.artifact_package_run WHERE status='PENDING'"
                + " OR (status='RETRYABLE' AND updated_at<DATEADD(SECOND,-?,SYSUTCDATETIME()))"
                + " OR (status='RUNNING' AND updated_at<DATEADD(SECOND,-?,SYSUTCDATETIME())) ORDER BY updated_at,package_run_id",
                Long.class, limit, retryAfterSeconds, staleRunningSeconds);
    }

    /** Claims the run for one attempt; false when another worker claimed it or it is not runnable. */
    public boolean claim(PackageRun run) {
        return dataPlane.update("UPDATE ingest.artifact_package_run SET status='RUNNING',attempt=attempt+1,updated_at=SYSUTCDATETIME()"
                + " WHERE package_run_id=? AND status=? AND attempt=?", run.runId(), run.status().name(), run.attempt()) == 1;
    }

    public void progress(long runId, String nextStageCode, PackageRunState state) {
        dataPlane.update("UPDATE ingest.artifact_package_run SET next_stage_code=?,state_json=?,last_issue_code=NULL,updated_at=SYSUTCDATETIME() WHERE package_run_id=?",
                nextStageCode, write(state.values()), runId);
    }

    public void stop(long runId, PackageRun.Status status, String issueCode, PackageRunState state) {
        dataPlane.update("UPDATE ingest.artifact_package_run SET status=?,last_issue_code=?,state_json=?,updated_at=SYSUTCDATETIME() WHERE package_run_id=?",
                status.name(), issueCode, write(state.values()), runId);
    }

    public void complete(long runId, PackageRunState state) {
        dataPlane.update("UPDATE ingest.artifact_package_run SET status='COMPLETED',next_stage_code=NULL,last_issue_code=NULL,state_json=?,"
                + "updated_at=SYSUTCDATETIME(),completed_at=SYSUTCDATETIME() WHERE package_run_id=?", write(state.values()), runId);
    }

    /** Operator retry of a blocked run: it becomes runnable again at the stage that blocked. */
    public boolean release(long runId) {
        return dataPlane.update("UPDATE ingest.artifact_package_run SET status='PENDING',updated_at=SYSUTCDATETIME() WHERE package_run_id=? AND status IN('BLOCKED','RETRYABLE')", runId) == 1;
    }

    public void recordStage(long runId, int attempt, String stageCode, String outcome, String issueCode, Map<String, Object> detail) {
        dataPlane.update("INSERT INTO ingest.artifact_package_run_stage(package_run_id,attempt,stage_code,outcome,issue_code,detail_json) VALUES(?,?,?,?,?,?)",
                runId, attempt, stageCode, outcome, issueCode, detail.isEmpty() ? null : write(detail));
    }

    public List<StageRecord> history(long runId) {
        return dataPlane.query("SELECT attempt,stage_code,outcome,issue_code,detail_json,recorded_at FROM ingest.artifact_package_run_stage WHERE package_run_id=? ORDER BY package_run_stage_id",
                (rs, n) -> new StageRecord(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        read(rs.getString(5), new TypeReference<LinkedHashMap<String, Object>>() {}), rs.getTimestamp(6).toInstant()), runId);
    }

    /** The load of one dataset version inside an ingestion batch. */
    public Optional<Long> datasetLoad(long batchId, long datasetVersionId) {
        return dataPlane.queryForList("SELECT dataset_load_id FROM ingest.dataset_load WHERE batch_id=? AND dataset_version_id=? ORDER BY dataset_load_id DESC",
                Long.class, batchId, datasetVersionId).stream().findFirst();
    }

    public String datasetLoadStatus(long datasetLoadId) {
        return dataPlane.queryForList("SELECT status FROM ingest.dataset_load WHERE dataset_load_id=?", String.class, datasetLoadId)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("Dataset load " + datasetLoadId + " not found"));
    }

    public String datasetSnapshotStatus(long datasetSnapshotId) {
        return dataPlane.queryForList("SELECT status FROM publication.dataset_snapshot WHERE dataset_snapshot_id=?", String.class, datasetSnapshotId)
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("Dataset snapshot " + datasetSnapshotId + " not found"));
    }

    private RowMapper<PackageRun> mapper() {
        return (rs, n) -> new PackageRun(rs.getLong(1), rs.getLong(2), rs.getLong(3), PackageRun.Status.valueOf(rs.getString(4)), rs.getString(5),
                rs.getInt(6), new PackageRunState(read(rs.getString(7), new TypeReference<LinkedHashMap<String, String>>() {})), rs.getString(8));
    }

    private <T extends Map<String, ?>> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value == null ? "{}" : value, type);
        } catch (JsonProcessingException corrupt) {
            throw new IllegalStateException("Stored package run JSON is invalid", corrupt);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("Package run JSON could not be serialized", impossible);
        }
    }
}
