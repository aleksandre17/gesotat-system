package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enforces approved one-year archive retention. Artifact object-store lifecycle must use the same retention window. */
@Service
public class PlatformArchiveRetentionService {
    private final JdbcTemplate archive;
    private final PlatformJobLeaseService lease;
    private final PlatformSchemaReadiness schemaReadiness;
    public PlatformArchiveRetentionService(@Qualifier("archivePlaneJdbcTemplate") JdbcTemplate archive,PlatformJobLeaseService lease,PlatformSchemaReadiness schemaReadiness){this.archive=archive;this.lease=lease;this.schemaReadiness=schemaReadiness;}

    @Scheduled(cron = "${platform.archive.purge.cron:0 30 3 * * *}")
    @Transactional(transactionManager = "archivePlaneTransactionManager")
    public void purgeExpired() {
        if (!schemaReadiness.isReady()) return;
        if(!lease.acquire("archive-retention-purge",60))return;
        try {
            archive.update("DELETE ar FROM archive.artifact_reference ar JOIN archive.snapshot s ON s.archive_snapshot_id=ar.archive_snapshot_id WHERE s.status='ACTIVE' AND s.purge_after<=SYSUTCDATETIME()");
            archive.update("DELETE r FROM archive.record r JOIN archive.snapshot s ON s.archive_snapshot_id=r.archive_snapshot_id WHERE s.status='ACTIVE' AND s.purge_after<=SYSUTCDATETIME()");
            archive.update("DELETE FROM archive.snapshot WHERE status='ACTIVE' AND purge_after<=SYSUTCDATETIME()");
        } finally { lease.release("archive-retention-purge"); }
    }
}
