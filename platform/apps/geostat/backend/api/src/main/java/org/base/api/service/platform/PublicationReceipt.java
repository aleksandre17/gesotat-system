package org.base.api.service.platform;

public record PublicationReceipt(long releaseId, long publicationSnapshotId, long previousPublicationSnapshotId, String status) {
}
