package org.base.api.service.artifact.sweep;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Operational bounds of the storage sweep; validated at startup (fail fast). */
@Component
@ConfigurationProperties(prefix = "platform.artifacts.storage-sweep")
public class ArtifactStorageSweepProperties {
    /** One statement checks a page against the registry, so a page must fit the provider's parameter limit. */
    static final int MAX_BATCH_SIZE = 1_000;

    private boolean enabled = true;
    private int batchSize = 500;
    private int leaseMinutes = 10;
    /** Objects younger than this may belong to an admission still in flight and are not yet orphans. */
    private int graceMinutes = 1_440;

    @PostConstruct
    void validate() {
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE || leaseMinutes < 1 || graceMinutes < 0)
            throw new IllegalStateException("platform.artifacts.storage-sweep: batch-size must be 1.." + MAX_BATCH_SIZE
                    + ", lease-minutes positive and grace-minutes non-negative");
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getLeaseMinutes() { return leaseMinutes; }
    public void setLeaseMinutes(int leaseMinutes) { this.leaseMinutes = leaseMinutes; }
    public int getGraceMinutes() { return graceMinutes; }
    public void setGraceMinutes(int graceMinutes) { this.graceMinutes = graceMinutes; }
}
