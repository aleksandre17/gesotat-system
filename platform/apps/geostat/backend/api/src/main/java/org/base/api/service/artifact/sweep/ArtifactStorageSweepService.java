package org.base.api.service.artifact.sweep;

import org.base.api.service.artifact.ArtifactMetrics;
import org.base.api.service.artifact.ArtifactObjectInventory;
import org.base.api.service.artifact.ArtifactObjectInventory.ListedObject;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.artifact.ArtifactProperties;
import org.base.api.service.artifact.ArtifactStorageException;
import org.base.api.service.platform.PlatformJobLeaseService;
import org.base.api.service.platform.PlatformSchemaReadiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reconciles Object Storage against the registry, one bounded page per tick: an object under a governed
 * prefix that the registry does not know is recorded as an orphan. The sweep is resumable (cursor per
 * scope), runs on one node at a time (lease) and never deletes bytes; cleanup is a separate, explicitly
 * scoped decision.
 */
@Service
public class ArtifactStorageSweepService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactStorageSweepService.class);
    private static final String JOB_NAME = "ARTIFACT_STORAGE_SWEEP";

    private final ArtifactStorageSweepRepository sweeps;
    private final ObjectProvider<ArtifactObjectInventory> inventories;
    private final ObjectProvider<ArtifactObjectStore> stores;
    private final ArtifactStorageSweepProperties properties;
    private final ArtifactProperties artifacts;
    private final ArtifactMetrics metrics;
    private final PlatformJobLeaseService lease;
    private final PlatformSchemaReadiness schemaReadiness;
    private final Clock clock;

    public ArtifactStorageSweepService(ArtifactStorageSweepRepository sweeps, ObjectProvider<ArtifactObjectInventory> inventories,
                                       ObjectProvider<ArtifactObjectStore> stores, ArtifactStorageSweepProperties properties,
                                       ArtifactProperties artifacts, ArtifactMetrics metrics, PlatformJobLeaseService lease,
                                       PlatformSchemaReadiness schemaReadiness, ObjectProvider<Clock> clock) {
        this.sweeps = sweeps;
        this.inventories = inventories;
        this.stores = stores;
        this.properties = properties;
        this.artifacts = artifacts;
        this.metrics = metrics;
        this.lease = lease;
        this.schemaReadiness = schemaReadiness;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    @Scheduled(fixedDelayString = "${platform.artifacts.storage-sweep.delay-millis:300000}")
    public void sweepNextPage() {
        ArtifactObjectInventory inventory = inventories.getIfAvailable();
        ArtifactObjectStore store = stores.getIfAvailable();
        if (!properties.isEnabled() || inventory == null || store == null || !schemaReadiness.isReady()
                || !lease.acquire(JOB_NAME, properties.getLeaseMinutes())) return;
        try {
            sweeps.syncScopes(store.ingestBucket(), artifacts.getUploadPrefix());
            var scope = sweeps.nextScope();
            if (scope.isEmpty()) return;
            var page = classify(scope.get(), inventory.page(scope.get().bucket(), scope.get().prefix(), scope.get().cursorKey(), properties.getBatchSize()));
            sweeps.recordPage(scope.get(), page);
            metrics.storageSweep(page.finished() ? "CYCLE_COMPLETED" : "PAGE_COMPLETED", page.orphans().size());
            // Counts only; keys and prefixes stay out of the log.
            if (page.finished()) log.info("artifact.storage_sweep cycle completed scope={} lastPageObjects={} lastPageOrphans={}",
                    scope.get().sweepId(), page.objectsListed(), page.orphans().size());
        } catch (ArtifactStorageException | DataAccessException failure) {
            // The cursor did not move, so the same page is retried on the next tick.
            metrics.storageSweep("RETRYABLE", 0);
            log.warn("artifact.storage_sweep retryable exception={}", failure.getClass().getSimpleName());
        } finally {
            try { lease.release(JOB_NAME); }
            catch (RuntimeException releaseFailure) { log.warn("artifact.storage_sweep lease release failed exception={}", releaseFailure.getClass().getSimpleName()); }
        }
    }

    ArtifactStorageSweepRepository.PageResult classify(ArtifactStorageSweepRepository.Scope scope, List<ListedObject> listed) {
        Set<String> keys = new LinkedHashSet<>();
        listed.forEach(object -> keys.add(object.key()));
        Set<String> registered = sweeps.registeredKeys(scope.bucket(), keys);
        Instant settledBefore = clock.instant().minus(properties.getGraceMinutes(), ChronoUnit.MINUTES);
        List<ArtifactStorageSweepRepository.Orphan> orphans = listed.stream()
                .filter(object -> !registered.contains(object.key()) && object.lastModified().isBefore(settledBefore))
                .map(object -> new ArtifactStorageSweepRepository.Orphan(object.key(), object.byteSize())).toList();
        return new ArtifactStorageSweepRepository.PageResult(listed.isEmpty() ? null : listed.get(listed.size() - 1).key(),
                listed.size(), orphans, keys, registered, listed.size() < properties.getBatchSize());
    }
}
