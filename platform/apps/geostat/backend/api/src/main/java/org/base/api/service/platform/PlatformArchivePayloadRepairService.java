package org.base.api.service.platform;

import org.base.api.service.storage.ObjectStorageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/** Reconciles archive SQL records with immutable S3-compatible payload objects. */
@Service
@ConditionalOnBean(ObjectStorageService.class)
public class PlatformArchivePayloadRepairService {
    private final JdbcTemplate archive;
    private final ObjectStorageService storage;
    private final PlatformSchemaReadiness schemaReadiness;
    public PlatformArchivePayloadRepairService(@Qualifier("archivePlaneJdbcTemplate") JdbcTemplate archive, ObjectStorageService storage,
                                               PlatformSchemaReadiness schemaReadiness) {
        this.archive = archive; this.storage = storage; this.schemaReadiness = schemaReadiness;
    }

    @Scheduled(initialDelayString="${platform.archive-pointer.initial-delay-ms:15000}", fixedDelayString="${platform.archive-pointer.fixed-delay-ms:3600000}")
    public void reconcile() {
        if (!schemaReadiness.isReady()) return;
        archive.query("SELECT archive_record_id,archive_snapshot_id,payload_json,payload_hash FROM archive.record", rs -> {
            while (rs.next()) try {
                long record = rs.getLong(1), snapshot = rs.getLong(2);
                String payload = rs.getString(3);
                String uri = storage.storeArchivePayload(snapshot, record, payload);
                long size = payload == null ? 0 : payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                archive.update("UPDATE archive.payload_pointer SET storage_uri=?,byte_size=?,immutable_flag=1 WHERE source_record_id=? AND storage_uri LIKE 'archive://%'", uri, size, record);
                archive.update("IF NOT EXISTS (SELECT 1 FROM archive.payload_pointer WHERE source_record_id=? AND storage_uri=?) INSERT archive.payload_pointer(source_record_id,storage_uri,storage_tier,checksum,byte_size,immutable_flag) VALUES(?,?,?,?,?,?)", record, uri, record, uri, "OBJECT_STORAGE", rs.getString(4), size, true);
            } catch (Exception ignored) { /* retry on the next scheduled run; archive SQL remains intact */ }
            return null;
        });
    }
}
