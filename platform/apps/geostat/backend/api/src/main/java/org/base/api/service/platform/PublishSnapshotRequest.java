package org.base.api.service.platform;

public record PublishSnapshotRequest(long productId, long datasetVersionId, long datasetSnapshotId, String checksum) {
}
