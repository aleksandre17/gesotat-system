package org.base.api.service.artifact.run;

import jakarta.annotation.PostConstruct;
import org.base.api.security.tenancy.CurrentCaller;
import org.base.api.service.platform.PlatformJobLeaseService;
import org.base.api.service.platform.PlatformSchemaReadiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Advances due package runs on one node at a time. A run interrupted by a crash is resumed, not restarted. */
@Component
public class PackageRunWorker {
    private static final Logger log = LoggerFactory.getLogger(PackageRunWorker.class);
    private static final String JOB_NAME = "ARTIFACT_PACKAGE_RUN";

    private final PackageRunService runs;
    private final PackageRunRepository repository;
    private final PlatformJobLeaseService lease;
    private final PlatformSchemaReadiness schemaReadiness;
    private final CurrentCaller callers;
    private final boolean enabled;
    private final int batchSize;
    private final int leaseMinutes;
    private final int retryAfterSeconds;
    private final int staleRunningSeconds;

    public PackageRunWorker(PackageRunService runs, PackageRunRepository repository, PlatformJobLeaseService lease,
                            PlatformSchemaReadiness schemaReadiness, CurrentCaller callers,
                            @Value("${platform.artifacts.package-run.enabled:true}") boolean enabled,
                            @Value("${platform.artifacts.package-run.batch-size:5}") int batchSize,
                            @Value("${platform.artifacts.package-run.lease-minutes:30}") int leaseMinutes,
                            @Value("${platform.artifacts.package-run.retry-after-seconds:120}") int retryAfterSeconds,
                            @Value("${platform.artifacts.package-run.stale-running-seconds:3600}") int staleRunningSeconds) {
        this.runs = runs;
        this.repository = repository;
        this.lease = lease;
        this.schemaReadiness = schemaReadiness;
        this.callers = callers;
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.leaseMinutes = leaseMinutes;
        this.retryAfterSeconds = retryAfterSeconds;
        this.staleRunningSeconds = staleRunningSeconds;
    }

    @PostConstruct
    void validate() {
        if (batchSize < 1 || leaseMinutes < 1 || retryAfterSeconds < 1 || staleRunningSeconds < leaseMinutes * 60)
            throw new IllegalStateException("platform.artifacts.package-run: bounds must be positive and stale-running-seconds must cover the lease");
    }

    @Scheduled(fixedDelayString = "${platform.artifacts.package-run.delay-millis:15000}")
    public void advanceDueRuns() {
        if (!enabled || !schemaReadiness.isReady() || !lease.acquire(JOB_NAME, leaseMinutes)) return;
        try {
            for (long runId : repository.due(batchSize, retryAfterSeconds, staleRunningSeconds)) {
                // Renewed per run: one long stage must not let a second node start the same work.
                if (!lease.acquire(JOB_NAME, leaseMinutes)) return;
                try {
                    // A worker has no request caller. It runs as the explicit SYSTEM caller, never as an
                    // absent one, so a stage can never pass a tenancy check by accident.
                    callers.asSystem(JOB_NAME, () -> runs.advance(runId));
                } catch (RuntimeException failure) {
                    log.warn("artifact.package_run advance failed run={} exception={}", runId, failure.getClass().getSimpleName());
                }
            }
        } finally {
            try { lease.release(JOB_NAME); }
            catch (RuntimeException releaseFailure) { log.warn("artifact.package_run lease release failed exception={}", releaseFailure.getClass().getSimpleName()); }
        }
    }
}
