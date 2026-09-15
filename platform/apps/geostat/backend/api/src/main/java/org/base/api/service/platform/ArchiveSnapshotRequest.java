package org.base.api.service.platform;

/** Archive command emitted after a published snapshot is superseded or expires. */
public record ArchiveSnapshotRequest(long productId, long publicationSnapshotId, long datasetVersionId,
                                     long datasetSnapshotId, String checksum) {
}
