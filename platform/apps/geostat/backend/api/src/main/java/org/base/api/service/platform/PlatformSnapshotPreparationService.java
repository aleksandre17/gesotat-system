package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.base.api.security.tenancy.TenantAccessGuard;

/** Materializes validated source rows as an immutable raw dataset snapshot awaiting semantic review. */
@Service
public class PlatformSnapshotPreparationService {
    private final JdbcTemplate dataPlane;
    private final TenantAccessGuard tenants;

    public PlatformSnapshotPreparationService(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                              TenantAccessGuard tenants) {
        this.dataPlane = dataPlane;
        this.tenants = tenants;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public long prepare(PrepareSnapshotRequest request) {
        if (request.datasetLoadId() <= 0 || request.artifactId() <= 0 || request.checksum() == null || !request.checksum().matches("[A-Fa-f0-9]{64}"))
            throw new IllegalArgumentException("Valid datasetLoadId, artifactId and SHA-256 checksum are required");
        // The body names the dataset load; this is the tenancy enforcement point of the prepare route.
        tenants.requireDatasetLoad(request.datasetLoadId());
        PreparationContext context = dataPlane.query(
                "SELECT l.dataset_version_id,l.status,a.checksum FROM ingest.dataset_load l " +
                        "JOIN ingest.artifact a ON a.batch_id=l.batch_id AND a.artifact_id=? WHERE l.dataset_load_id=?",
                rs -> rs.next() ? new PreparationContext(rs.getLong(1), rs.getString(2), rs.getString(3)) : null,
                request.artifactId(), request.datasetLoadId());
        if (context == null) throw new IllegalArgumentException("Artifact does not belong to the requested dataset load");
        if (!context.artifactChecksum().equalsIgnoreCase(request.checksum()))
            throw new IllegalArgumentException("Requested checksum does not match the dataset load artifact");

        Snapshot existing = dataPlane.query(
                "SELECT dataset_snapshot_id,dataset_version_id,row_count,checksum FROM publication.dataset_snapshot WITH (UPDLOCK,HOLDLOCK) WHERE dataset_load_id=?",
                rs -> rs.next() ? new Snapshot(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4)) : null,
                request.datasetLoadId());
        if (existing != null) {
            if (existing.datasetVersionId() != context.datasetVersionId() || !existing.checksum().equalsIgnoreCase(context.artifactChecksum()))
                throw new IllegalStateException("Existing snapshot provenance conflicts with its dataset load artifact");
            Long rawRows = dataPlane.queryForObject("SELECT COUNT(*) FROM raw.source_record WHERE dataset_snapshot_id=?", Long.class, existing.id());
            Long wrongArtifactRows = dataPlane.queryForObject("SELECT COUNT(*) FROM raw.source_record WHERE dataset_snapshot_id=? AND artifact_id<>?", Long.class, existing.id(), request.artifactId());
            if (rawRows == null || rawRows != existing.rowCount() || wrongArtifactRows == null || wrongArtifactRows != 0)
                throw new IllegalStateException("Existing snapshot rows do not match the recorded artifact provenance");
            return existing.id();
        }
        if (!"VALIDATED".equals(context.loadStatus())) throw new IllegalStateException("Dataset load is not technically validated");
        Long count = dataPlane.queryForObject("SELECT COUNT(*) FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID'", Long.class, request.datasetLoadId());
        long snapshotId = dataPlane.queryForObject("INSERT INTO publication.dataset_snapshot(dataset_load_id,dataset_version_id,status,row_count,checksum) OUTPUT INSERTED.dataset_snapshot_id VALUES(?,?,'REVIEW_REQUIRED',?,?)",
                Long.class, request.datasetLoadId(), context.datasetVersionId(), count, context.artifactChecksum());
        int inserted = dataPlane.update("INSERT INTO raw.source_record(dataset_snapshot_id,artifact_id,source_row_number,source_key,payload_json,payload_hash) " +
                        "SELECT ?,?,source_row_number,source_key,raw_payload_json,payload_hash FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID'",
                snapshotId, request.artifactId(), request.datasetLoadId());
        if (inserted != count) throw new IllegalStateException("Snapshot source-row materialization count does not match validated rows");
        int updated = dataPlane.update("UPDATE ingest.dataset_load SET status='PREPARED' WHERE dataset_load_id=? AND status='VALIDATED'", request.datasetLoadId());
        if (updated != 1) throw new IllegalStateException("Dataset load state changed while its snapshot was being prepared");
        return snapshotId;
    }

    private record PreparationContext(long datasetVersionId, String loadStatus, String artifactChecksum) {}
    private record Snapshot(long id, long datasetVersionId, long rowCount, String checksum) {}
}
