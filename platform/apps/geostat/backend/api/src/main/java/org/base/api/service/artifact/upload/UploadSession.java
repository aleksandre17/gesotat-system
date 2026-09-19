package org.base.api.service.artifact.upload;

import java.time.Instant;
import java.util.UUID;

/** Persisted state of one tenant-scoped resumable upload. */
public record UploadSession(UUID id, String tenantKeyHash, String ownerKeyHash, String requestFingerprint, String packageCode,
                            String contractCode, int contractRevision, String datasetCode, long expectedBytes, long partSize,
                            int expectedParts, long receivedBytes, UploadSessionStatus status, boolean quotaReleased,
                            Long manifestId, String packageChecksum, Instant expiresAt) {

    /** A checkpointed or reserved part. */
    public record Part(int partNumber, String sha256, long byteSize, boolean received) {}

    /** Tenant reservation totals; absent means the tenant has reserved nothing yet. */
    public record Quota(long reservedBytes, int activeSessions) {}

    public UploadSession withStatus(UploadSessionStatus next) {
        return new UploadSession(id, tenantKeyHash, ownerKeyHash, requestFingerprint, packageCode, contractCode, contractRevision,
                datasetCode, expectedBytes, partSize, expectedParts, receivedBytes, next, quotaReleased, manifestId, packageChecksum, expiresAt);
    }

    public boolean expiredAt(Instant now) {
        return now.isAfter(expiresAt);
    }

    public long partStart(int partNumber) {
        return (long) (partNumber - 1) * partSize;
    }

    public long partLength(int partNumber) {
        return Math.min(partSize, expectedBytes - partStart(partNumber));
    }
}
