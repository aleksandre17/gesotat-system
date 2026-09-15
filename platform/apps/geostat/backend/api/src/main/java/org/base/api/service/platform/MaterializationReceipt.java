package org.base.api.service.platform;

public record MaterializationReceipt(long datasetSnapshotId, long contractSourceId, String family, long writtenRows, String status) {
}
