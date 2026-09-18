package org.base.api.service.artifact;

import org.base.api.service.platform.PlatformJobLeaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.base.api.service.platform.PlatformSchemaReadiness;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Bounded, distributed and restartable verification sweep over registered artifact objects. */
@Service
public class ArtifactObjectIntegrityAuditService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactObjectIntegrityAuditService.class);
    private static final String JOB_NAME = "ARTIFACT_OBJECT_INTEGRITY_AUDIT";
    private final ArtifactRegistry registry;
    private final ObjectProvider<ArtifactObjectStore> stores;
    private final ArtifactMetrics metrics;
    private final ArtifactProperties properties;
    private final PlatformJobLeaseService lease;
    private final PlatformSchemaReadiness schemaReadiness;

    public ArtifactObjectIntegrityAuditService(ArtifactRegistry registry, ObjectProvider<ArtifactObjectStore> stores,
                                               ArtifactMetrics metrics, ArtifactProperties properties,
                                               PlatformJobLeaseService lease, PlatformSchemaReadiness schemaReadiness) {
        this.registry = registry;
        this.stores = stores;
        this.metrics = metrics;
        this.properties = properties;
        this.lease = lease;
        this.schemaReadiness = schemaReadiness;
    }

    @Scheduled(fixedDelayString = "${platform.artifacts.integrity-audit-delay-millis:60000}")
    public void auditDueObjects() {
        if (!schemaReadiness.isReady() || !properties.isIntegrityAuditEnabled() || !lease.acquire(JOB_NAME, properties.getIntegrityAuditLeaseMinutes())) return;
        Long runId = null;
        try {
            registry.abandonStaleAuditRuns(Math.max(5, properties.getIntegrityAuditLeaseMinutes() * 2));
            var candidates = registry.auditCandidates(properties.getIntegrityAuditBatchSize(),
                    properties.getIntegrityAuditRecheckMinutes(), properties.getIntegrityAuditIssueRetryMinutes());
            if (candidates.isEmpty()) return;
            runId = registry.startAuditRun();
            ArtifactObjectStore storage = stores.getIfAvailable();
            if (storage == null) {
                finishRetryable(runId, "OBJECT_STORAGE_UNAVAILABLE");
                return;
            }
            long verifiedBytes = 0;
            String retryCode = null;
            for (ArtifactRegistry.AuditCandidate candidate : candidates) {
                if (candidate.byteSize() > properties.getIntegrityAuditMaxBytesPerRun() - verifiedBytes) {
                    if (verifiedBytes == 0) retryCode = "OBJECT_EXCEEDS_AUDIT_BUDGET";
                    break;
                }
                if (!lease.acquire(JOB_NAME, properties.getIntegrityAuditLeaseMinutes())) {
                    retryCode = "AUDIT_LEASE_LOST";
                    break;
                }
                ArtifactObjectStore.ObjectLocation location = new ArtifactObjectStore.ObjectLocation(candidate.bucket(), candidate.objectKey());
                Optional<ArtifactObjectStore.ObjectStat> stat = storage.stat(location);
                VerificationStatus status;
                Long observedBytes = null;
                String observedSha256 = null;
                if (stat.isEmpty()) {
                    status = VerificationStatus.MISSING;
                } else {
                    observedBytes = stat.get().byteSize();
                    if (observedBytes != candidate.byteSize()) {
                        status = VerificationStatus.CHECKSUM_MISMATCH;
                    } else {
                        observedSha256 = storage.sha256(location);
                        status = candidate.sha256().equals(observedSha256) ? VerificationStatus.VERIFIED : VerificationStatus.CHECKSUM_MISMATCH;
                    }
                }
                if (!lease.acquire(JOB_NAME, properties.getIntegrityAuditLeaseMinutes())) {
                    retryCode = "AUDIT_LEASE_LOST";
                    break;
                }
                registry.recordAuditResult(runId, candidate, status, observedBytes, observedSha256);
                metrics.verification(status.name(), 1);
                verifiedBytes += candidate.byteSize();
            }
            String outcome = retryCode == null ? "COMPLETED" : "RETRYABLE";
            registry.completeAuditRun(runId, outcome, retryCode);
            metrics.integrityAudit(outcome);
        } catch (RuntimeException failure) {
            String code = failure instanceof ArtifactStorageException ? "OBJECT_STORAGE_UNAVAILABLE"
                    : failure instanceof DataAccessException ? "DATA_PLANE_UNAVAILABLE" : "AUDIT_FAILED";
            if (runId != null) finishRetryable(runId, code);
            log.warn("artifact.integrity_audit failed run={} code={} exception={}", runId, code, failure.getClass().getSimpleName());
        } finally {
            try { lease.release(JOB_NAME); }
            catch (RuntimeException releaseFailure) { log.warn("artifact.integrity_audit lease release failed exception={}", releaseFailure.getClass().getSimpleName()); }
        }
    }

    private void finishRetryable(long runId, String code) {
        registry.completeAuditRun(runId, "RETRYABLE", code);
        metrics.integrityAudit("RETRYABLE");
    }
}
