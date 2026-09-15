package org.base.api.service.platform;

public record RollbackPublicationRequest(long productId, long targetPublicationSnapshotId) {
}
