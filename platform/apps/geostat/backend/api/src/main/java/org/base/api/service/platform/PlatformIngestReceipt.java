package org.base.api.service.platform;

public record PlatformIngestReceipt(long batchId, long artifactId, long datasetLoadId, int stagedRows, String status) {
}
