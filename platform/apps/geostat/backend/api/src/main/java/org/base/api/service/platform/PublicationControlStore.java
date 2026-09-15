package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Independently committed Control-plane intent/finalization for retryable cross-plane publication. */
@Service
public class PublicationControlStore {
    private final JdbcTemplate control;
    public PublicationControlStore(@Qualifier("primaryJdbcTemplate") JdbcTemplate control) { this.control = control; }

    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public long createIntent(PublishSnapshotRequest request) {
        long releaseId = control.queryForObject("INSERT INTO platform.release(product_id,release_version,status) OUTPUT INSERTED.release_id " +
                "SELECT ?,COALESCE(MAX(release_version),0)+1,'PUBLISHING' FROM platform.release WHERE product_id=?", Long.class, request.productId(), request.productId());
        String payload = "{\"productId\":" + request.productId() + ",\"datasetVersionId\":" + request.datasetVersionId() + ",\"datasetSnapshotId\":" + request.datasetSnapshotId() + ",\"checksum\":\"" + request.checksum() + "\"}";
        control.update("INSERT INTO platform.outbox_event(aggregate_type,aggregate_id,event_type,payload_json,status) VALUES('RELEASE',?,'PUBLISH_SNAPSHOT',?,'PENDING')", releaseId, payload);
        return releaseId;
    }

    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public void complete(long releaseId) {
        control.update("UPDATE platform.release SET status='PUBLISHED',published_at=SYSUTCDATETIME() WHERE release_id=? AND status='PUBLISHING'", releaseId);
        control.update("UPDATE platform.outbox_event SET status='PROCESSED',processed_at=SYSUTCDATETIME(),attempts=attempts+1,last_error=NULL WHERE aggregate_type='RELEASE' AND aggregate_id=? AND event_type='PUBLISH_SNAPSHOT' AND status IN ('PENDING','PROCESSING')", releaseId);
    }

    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public void enqueueArchive(long productId, long publicationSnapshotId) {
        control.update("INSERT INTO platform.outbox_event(aggregate_type,aggregate_id,event_type,payload_json,status) VALUES('PUBLICATION_SNAPSHOT',?,'ARCHIVE_PUBLICATION',?,'PENDING')",
                publicationSnapshotId, "{\"productId\":" + productId + ",\"publicationSnapshotId\":" + publicationSnapshotId + "}");
    }

    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public void markProcessed(long eventId) {
        control.update("UPDATE platform.outbox_event SET status='PROCESSED',processed_at=SYSUTCDATETIME(),attempts=attempts+1,last_error=NULL WHERE event_id=? AND status='PROCESSING'", eventId);
    }
}
