package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Copies immutable raw lineage to the one-year archive plane; no source or legacy tables are changed. */
@Service
public class PlatformArchiveService {
    private final JdbcTemplate dataPlane;
    private final JdbcTemplate archivePlane;

    public PlatformArchiveService(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                  @Qualifier("archivePlaneJdbcTemplate") JdbcTemplate archivePlane) {
        this.dataPlane = dataPlane;
        this.archivePlane = archivePlane;
    }

    @Transactional(transactionManager = "archivePlaneTransactionManager")
    public long archive(ArchiveSnapshotRequest request) {
        if (request.productId() <= 0 || request.publicationSnapshotId() <= 0 || request.datasetVersionId() <= 0 || request.datasetSnapshotId() <= 0)
            throw new IllegalArgumentException("Archive identifiers must be positive");
        if (request.checksum() == null || !request.checksum().matches("[A-Fa-f0-9]{64}"))
            throw new IllegalArgumentException("checksum must be SHA-256 hex");

        Long existing = archivePlane.query("SELECT archive_snapshot_id FROM archive.snapshot WHERE product_id=? AND original_snapshot_id=?",
                rs -> rs.next() ? rs.getLong(1) : null, request.productId(), request.publicationSnapshotId());
        long archiveId = existing != null ? existing : archivePlane.queryForObject("INSERT INTO archive.snapshot(product_id,original_snapshot_id,purge_after,checksum,status) " +
                        "OUTPUT INSERTED.archive_snapshot_id VALUES(?,?,DATEADD(YEAR,1,SYSUTCDATETIME()),?,'ACTIVE')", Long.class,
                request.productId(), request.publicationSnapshotId(), request.checksum());

        List<Map<String, Object>> sourceRows = dataPlane.queryForList("SELECT source_record_id,source_key,payload_json,payload_hash FROM raw.source_record WHERE dataset_snapshot_id=? ORDER BY source_record_id", request.datasetSnapshotId());
        archivePlane.batchUpdate("IF NOT EXISTS (SELECT 1 FROM archive.record WHERE archive_snapshot_id=? AND original_source_record_id=?) INSERT INTO archive.record(archive_snapshot_id,dataset_version_id,original_source_record_id,record_kind,source_key,payload_json,payload_hash) VALUES(?,?,?,'RAW_SOURCE',?,?,?)",
                sourceRows, 500, (ps, row) -> {
                    ps.setLong(1, archiveId);
                    ps.setLong(2, ((Number) row.get("source_record_id")).longValue());
                    ps.setLong(3, archiveId);
                    ps.setLong(4, request.datasetVersionId());
                    ps.setLong(5, ((Number) row.get("source_record_id")).longValue());
                    ps.setString(6, (String) row.get("source_key"));
                    ps.setString(7, (String) row.get("payload_json"));
                    ps.setString(8, (String) row.get("payload_hash"));
                });
        /* Upload receipts plus every content object attached to the snapshot's rows, so an archived
           snapshot keeps the byte references its published attachments pointed to. */
        List<Map<String, Object>> artifacts = dataPlane.queryForList("SELECT a.object_uri,MIN(a.checksum) checksum,MIN(a.original_name) original_name FROM (" +
                "SELECT a.object_uri,a.checksum,a.original_name FROM ingest.artifact a JOIN raw.source_record r ON r.artifact_id=a.artifact_id WHERE r.dataset_snapshot_id=? " +
                "UNION ALL SELECT CONCAT('s3://',o.bucket,'/',o.object_key),o.sha256,v.original_name FROM entity.artifact_attachment t " +
                "JOIN ingest.artifact_version v ON v.artifact_version_id=t.artifact_version_id JOIN ingest.artifact_object o ON o.artifact_object_id=v.artifact_object_id " +
                "WHERE t.dataset_snapshot_id=?) a GROUP BY a.object_uri", request.datasetSnapshotId(), request.datasetSnapshotId());
        archivePlane.batchUpdate("IF NOT EXISTS (SELECT 1 FROM archive.artifact_reference WHERE archive_snapshot_id=? AND object_uri=?) INSERT INTO archive.artifact_reference(archive_snapshot_id,object_uri,checksum,original_name) VALUES(?,?,?,?)",
                artifacts, 250, (ps, row) -> {
                    ps.setLong(1, archiveId); ps.setString(2, (String) row.get("object_uri"));
                    ps.setLong(3, archiveId); ps.setString(4, (String) row.get("object_uri"));
                    ps.setString(5, (String) row.get("checksum")); ps.setString(6, (String) row.get("original_name"));
                });
        return archiveId;
    }
}
