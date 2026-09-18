package org.base.api.service.artifact;

import org.base.api.service.platform.PlatformJobLeaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.base.api.service.platform.PlatformSchemaReadiness;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Reconciles approved artifact relations for snapshots in bounded, distributed batches. */
@Service
public class ArtifactRelationIntegrityAuditService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactRelationIntegrityAuditService.class);
    private static final String JOB_NAME = "ARTIFACT_RELATION_INTEGRITY_AUDIT";

    private final ArtifactContractResolver contracts;
    private final ArtifactAttachmentRepository attachments;
    private final ArtifactReconciliationService reconciliation;
    private final ArtifactMetrics metrics;
    private final ArtifactProperties properties;
    private final PlatformJobLeaseService lease;
    private final PlatformSchemaReadiness schemaReadiness;

    public ArtifactRelationIntegrityAuditService(ArtifactContractResolver contracts, ArtifactAttachmentRepository attachments,
                                                 ArtifactReconciliationService reconciliation, ArtifactMetrics metrics,
                                                 ArtifactProperties properties, PlatformJobLeaseService lease,
                                                 PlatformSchemaReadiness schemaReadiness) {
        this.contracts = contracts;
        this.attachments = attachments;
        this.reconciliation = reconciliation;
        this.metrics = metrics;
        this.properties = properties;
        this.lease = lease;
        this.schemaReadiness = schemaReadiness;
    }

    @Scheduled(fixedDelayString = "${platform.artifacts.integrity-audit-delay-millis:60000}")
    public void reconcileDueSnapshots() {
        if (!schemaReadiness.isReady() || !properties.isIntegrityAuditEnabled()
                || !lease.acquire(JOB_NAME, properties.getIntegrityAuditLeaseMinutes())) return;
        try {
            var datasetVersions = contracts.reconcilableDatasetVersions();
            var candidates = attachments.reconciliationCandidates(datasetVersions,
                    properties.getRelationAuditBatchSize(), properties.getIntegrityAuditRecheckMinutes());
            for (long snapshotId : candidates) {
                if (!lease.acquire(JOB_NAME, properties.getIntegrityAuditLeaseMinutes())) {
                    metrics.relationAudit("RETRYABLE");
                    return;
                }
                reconciliation.reconcile(snapshotId);
            }
            metrics.relationAudit("COMPLETED");
        } catch (RuntimeException failure) {
            metrics.relationAudit("RETRYABLE");
            log.warn("artifact.relation_integrity_audit failed exception={}", failure.getClass().getSimpleName());
        } finally {
            try { lease.release(JOB_NAME); }
            catch (RuntimeException releaseFailure) {
                log.warn("artifact.relation_integrity_audit lease release failed exception={}", releaseFailure.getClass().getSimpleName());
            }
        }
    }
}
