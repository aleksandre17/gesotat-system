package org.base.api.service.artifact;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Operational bounds of the artifact line. Every value is configuration, validated at startup (fail fast). */
@Component
@ConfigurationProperties(prefix = "platform.artifacts")
public class ArtifactProperties {
    /** Platform-owned, content-addressed pool for uploaded package bytes. Callers never choose storage paths. */
    private String uploadPrefix = "artifacts/sha256/";
    private int maxInventoryBytes = 16 * 1024 * 1024;
    private int maxPackageEntries = 20_000;
    private long maxEntryBytes = 1024L * 1024 * 1024;
    private long maxPackageBytes = 8L * 1024 * 1024 * 1024;
    /** Compressed multipart request boundary; separate from the uncompressed package expansion budget. */
    private long maxUploadBytes = 2L * 1024 * 1024 * 1024;
    /** Upper bound of findings returned in one report; counts per code are always complete. */
    private int reportIssueLimit = 200;

    @PostConstruct
    void validate() {
        ArtifactKeys.requirePrefix(uploadPrefix);
        if (maxInventoryBytes <= 0 || maxPackageEntries <= 0 || maxEntryBytes <= 0 || maxPackageBytes < maxEntryBytes
                || maxUploadBytes <= 0 || reportIssueLimit <= 0)
            throw new IllegalStateException("platform.artifacts limits must be positive and maxPackageBytes >= maxEntryBytes");
    }

    public String getUploadPrefix() { return uploadPrefix; }
    public void setUploadPrefix(String uploadPrefix) { this.uploadPrefix = uploadPrefix; }
    public int getMaxInventoryBytes() { return maxInventoryBytes; }
    public void setMaxInventoryBytes(int maxInventoryBytes) { this.maxInventoryBytes = maxInventoryBytes; }
    public int getMaxPackageEntries() { return maxPackageEntries; }
    public void setMaxPackageEntries(int maxPackageEntries) { this.maxPackageEntries = maxPackageEntries; }
    public long getMaxEntryBytes() { return maxEntryBytes; }
    public void setMaxEntryBytes(long maxEntryBytes) { this.maxEntryBytes = maxEntryBytes; }
    public long getMaxPackageBytes() { return maxPackageBytes; }
    public void setMaxPackageBytes(long maxPackageBytes) { this.maxPackageBytes = maxPackageBytes; }
    public long getMaxUploadBytes() { return maxUploadBytes; }
    public void setMaxUploadBytes(long maxUploadBytes) { this.maxUploadBytes = maxUploadBytes; }
    public int getReportIssueLimit() { return reportIssueLimit; }
    public void setReportIssueLimit(int reportIssueLimit) { this.reportIssueLimit = reportIssueLimit; }
}
