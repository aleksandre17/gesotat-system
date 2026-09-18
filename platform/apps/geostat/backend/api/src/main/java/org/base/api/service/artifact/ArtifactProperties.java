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
    private long uploadPartBytes = 64L * 1024 * 1024;
    private long maxTenantReservedUploadBytes = 16L * 1024 * 1024 * 1024;
    private int maxTenantActiveUploadSessions = 100;
    private long uploadSessionTtlSeconds = 86_400;
    private int uploadProcessingLeaseSeconds = 900;
    /** Upper bound of findings returned in one report; counts per code are always complete. */
    private int reportIssueLimit = 200;
    /** Artifact admission is fail-closed unless an operational malware scanner returns CLEAN. */
    private boolean malwareScanRequired = true;
    private String malwareScannerHost = "";
    private int malwareScannerPort = 3310;
    private int malwareScannerTimeoutMillis = 30_000;
    private long malwareScannerMaxBytes = 1024L * 1024 * 1024;

    @PostConstruct
    void validate() {
        ArtifactKeys.requirePrefix(uploadPrefix);
        if (maxInventoryBytes <= 0 || maxPackageEntries <= 0 || maxEntryBytes <= 0 || maxPackageBytes < maxEntryBytes
                || maxUploadBytes <= 0 || uploadPartBytes <= 0 || uploadPartBytes > maxUploadBytes
                || 1 + (maxUploadBytes - 1) / uploadPartBytes > Integer.MAX_VALUE
                || maxTenantReservedUploadBytes <= 0 || maxTenantActiveUploadSessions <= 0 || uploadSessionTtlSeconds <= 0
                || uploadProcessingLeaseSeconds <= 0
                || reportIssueLimit <= 0 || malwareScannerPort < 1 || malwareScannerPort > 65535
                || malwareScannerTimeoutMillis <= 0 || malwareScannerMaxBytes <= 0)
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
    public long getUploadPartBytes() { return uploadPartBytes; }
    public void setUploadPartBytes(long uploadPartBytes) { this.uploadPartBytes = uploadPartBytes; }
    public long getMaxTenantReservedUploadBytes() { return maxTenantReservedUploadBytes; }
    public void setMaxTenantReservedUploadBytes(long maxTenantReservedUploadBytes) { this.maxTenantReservedUploadBytes = maxTenantReservedUploadBytes; }
    public int getMaxTenantActiveUploadSessions() { return maxTenantActiveUploadSessions; }
    public void setMaxTenantActiveUploadSessions(int maxTenantActiveUploadSessions) { this.maxTenantActiveUploadSessions = maxTenantActiveUploadSessions; }
    public long getUploadSessionTtlSeconds() { return uploadSessionTtlSeconds; }
    public void setUploadSessionTtlSeconds(long uploadSessionTtlSeconds) { this.uploadSessionTtlSeconds = uploadSessionTtlSeconds; }
    public int getUploadProcessingLeaseSeconds() { return uploadProcessingLeaseSeconds; }
    public void setUploadProcessingLeaseSeconds(int uploadProcessingLeaseSeconds) { this.uploadProcessingLeaseSeconds = uploadProcessingLeaseSeconds; }
    public int getReportIssueLimit() { return reportIssueLimit; }
    public void setReportIssueLimit(int reportIssueLimit) { this.reportIssueLimit = reportIssueLimit; }
    public boolean isMalwareScanRequired() { return malwareScanRequired; }
    public void setMalwareScanRequired(boolean malwareScanRequired) { this.malwareScanRequired = malwareScanRequired; }
    public String getMalwareScannerHost() { return malwareScannerHost; }
    public void setMalwareScannerHost(String malwareScannerHost) { this.malwareScannerHost = malwareScannerHost == null ? "" : malwareScannerHost.strip(); }
    public int getMalwareScannerPort() { return malwareScannerPort; }
    public void setMalwareScannerPort(int malwareScannerPort) { this.malwareScannerPort = malwareScannerPort; }
    public int getMalwareScannerTimeoutMillis() { return malwareScannerTimeoutMillis; }
    public void setMalwareScannerTimeoutMillis(int malwareScannerTimeoutMillis) { this.malwareScannerTimeoutMillis = malwareScannerTimeoutMillis; }
    public long getMalwareScannerMaxBytes() { return malwareScannerMaxBytes; }
    public void setMalwareScannerMaxBytes(long malwareScannerMaxBytes) { this.malwareScannerMaxBytes = malwareScannerMaxBytes; }
}
