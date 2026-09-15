package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Materializes validated source rows as an immutable raw dataset snapshot awaiting semantic review. */
@Service
public class PlatformSnapshotPreparationService {
    private final JdbcTemplate dataPlane;

    public PlatformSnapshotPreparationService(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane) {
        this.dataPlane = dataPlane;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public long prepare(PrepareSnapshotRequest request) {
        if (request.datasetLoadId() <= 0 || request.artifactId() <= 0 || request.checksum() == null || !request.checksum().matches("[A-Fa-f0-9]{64}"))
            throw new IllegalArgumentException("Valid datasetLoadId, artifactId and SHA-256 checksum are required");
        Long existing = dataPlane.query("SELECT dataset_snapshot_id FROM publication.dataset_snapshot WHERE dataset_load_id=?",
                rs -> rs.next() ? rs.getLong(1) : null, request.datasetLoadId());
        if (existing != null) return existing;
        Long datasetVersionId = dataPlane.query("SELECT dataset_version_id FROM ingest.dataset_load WHERE dataset_load_id=? AND status='VALIDATED'",
                rs -> rs.next() ? rs.getLong(1) : null, request.datasetLoadId());
        if (datasetVersionId == null) throw new IllegalStateException("Dataset load is not technically validated");
        Long count = dataPlane.queryForObject("SELECT COUNT(*) FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID'", Long.class, request.datasetLoadId());
        long snapshotId = dataPlane.queryForObject("INSERT INTO publication.dataset_snapshot(dataset_load_id,dataset_version_id,status,row_count,checksum) OUTPUT INSERTED.dataset_snapshot_id VALUES(?,?,'REVIEW_REQUIRED',?,?)",
                Long.class, request.datasetLoadId(), datasetVersionId, count, request.checksum());
        dataPlane.update("INSERT INTO raw.source_record(dataset_snapshot_id,artifact_id,source_row_number,source_key,payload_json,payload_hash) " +
                        "SELECT ?,?,source_row_number,source_key,raw_payload_json,payload_hash FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID'",
                snapshotId, request.artifactId(), request.datasetLoadId());
        dataPlane.update("UPDATE ingest.dataset_load SET status='PREPARED' WHERE dataset_load_id=?", request.datasetLoadId());
        return snapshotId;
    }
}
