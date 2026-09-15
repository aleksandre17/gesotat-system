package org.base.api.service.platform;

import java.util.List;

/** Immutable technical envelope after an artifact has passed contract selection. */
public record PlatformIngestRequest(
        long contractId,
        long productId,
        long datasetVersionId,
        String sourceName,
        String artifactName,
        String artifactFormat,
        String objectUri,
        String checksum,
        long byteSize,
        List<PlatformIngestRow> rows) {
}
