package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The atomic half of a release: all active-plane visibility changes share one local transaction. */
@Service
public class DataPlanePublicationWriter {
    private final JdbcTemplate dataPlane;

    public DataPlanePublicationWriter(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane) {
        this.dataPlane = dataPlane;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public PublicationReceipt publish(PublishSnapshotRequest request, long releaseId, long rowCount) {
        Long existing = dataPlane.query("SELECT snapshot_id FROM publication.snapshot WHERE release_id=?", rs -> rs.next() ? rs.getLong(1) : null, releaseId);
        if (existing != null) {
            Long previousExisting = dataPlane.query("SELECT previous_snapshot_id FROM publication.snapshot WHERE snapshot_id=?", rs -> rs.next() && rs.getObject(1) != null ? rs.getLong(1) : null, existing);
            return new PublicationReceipt(releaseId, existing, previousExisting == null ? 0 : previousExisting, "PUBLISHED");
        }
        Long previous = dataPlane.query("SELECT TOP 1 snapshot_id FROM publication.snapshot WHERE product_id=? AND status='PUBLISHED' ORDER BY published_at DESC,snapshot_id DESC",
                rs -> rs.next() ? rs.getLong(1) : null, request.productId());
        long snapshotId = dataPlane.queryForObject("INSERT INTO publication.snapshot(product_id,release_id,status,published_at,previous_snapshot_id) OUTPUT INSERTED.snapshot_id VALUES(?,?,'PUBLISHED',SYSUTCDATETIME(),?)",
                Long.class, request.productId(), releaseId, previous);
        var members = dataPlane.queryForList("SELECT dataset_snapshot_id,dataset_version_id,row_count,checksum FROM (SELECT s.dataset_snapshot_id,s.dataset_version_id,s.row_count,s.checksum,ROW_NUMBER() OVER(PARTITION BY s.dataset_version_id ORDER BY s.dataset_snapshot_id DESC) rn FROM publication.dataset_snapshot s JOIN ingest.dataset_load l ON l.dataset_load_id=s.dataset_load_id JOIN ingest.batch b ON b.batch_id=l.batch_id WHERE b.product_id=? AND s.status='REVIEW_REQUIRED') q WHERE rn=1 ORDER BY dataset_snapshot_id", request.productId());
        if (members.isEmpty()) throw new IllegalStateException("No release-gated dataset snapshots are ready");
        for (var member: members) dataPlane.update("INSERT INTO publication.snapshot_member(snapshot_id,dataset_version_id,dataset_snapshot_id,row_count,checksum) VALUES(?,?,?,?,?)", snapshotId, member.get("dataset_version_id"), member.get("dataset_snapshot_id"), member.get("row_count"), member.get("checksum"));
        dataPlane.update("UPDATE publication.dataset_snapshot SET status='PUBLISHED' WHERE dataset_snapshot_id IN (SELECT s.dataset_snapshot_id FROM publication.dataset_snapshot s JOIN ingest.dataset_load l ON l.dataset_load_id=s.dataset_load_id JOIN ingest.batch b ON b.batch_id=l.batch_id WHERE b.product_id=? AND s.status='REVIEW_REQUIRED')", request.productId());
        if (previous != null) dataPlane.update("UPDATE publication.snapshot SET status='SUPERSEDED' WHERE snapshot_id=?", previous);
        return new PublicationReceipt(releaseId, snapshotId, previous == null ? 0 : previous, "PUBLISHED");
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public PublicationReceipt rollback(RollbackPublicationRequest request) {
        Long targetRelease = dataPlane.query("SELECT release_id FROM publication.snapshot WHERE snapshot_id=? AND product_id=? AND status IN ('PUBLISHED','SUPERSEDED')",
                rs -> rs.next() ? rs.getLong(1) : null, request.targetPublicationSnapshotId(), request.productId());
        if (targetRelease == null) throw new IllegalArgumentException("Target snapshot does not belong to product or cannot be restored");
        Long current = dataPlane.query("SELECT TOP 1 snapshot_id FROM publication.snapshot WHERE product_id=? AND status='PUBLISHED' ORDER BY published_at DESC,snapshot_id DESC",
                rs -> rs.next() ? rs.getLong(1) : null, request.productId());
        if (current != null && current != request.targetPublicationSnapshotId())
            dataPlane.update("UPDATE publication.snapshot SET status='SUPERSEDED' WHERE snapshot_id=?", current);
        dataPlane.update("UPDATE publication.snapshot SET status='PUBLISHED',published_at=SYSUTCDATETIME() WHERE snapshot_id=?", request.targetPublicationSnapshotId());
        return new PublicationReceipt(targetRelease, request.targetPublicationSnapshotId(), current == null ? 0 : current, "ROLLED_BACK");
    }
}
