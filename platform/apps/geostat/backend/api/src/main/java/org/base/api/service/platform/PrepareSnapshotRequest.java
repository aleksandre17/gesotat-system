package org.base.api.service.platform;

public record PrepareSnapshotRequest(long datasetLoadId, long artifactId, String checksum) {
}
