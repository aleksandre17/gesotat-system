package org.base.api.service.platform;

import java.util.List;

/** One immutable artifact, one governed batch, and all of its independently resumable dataset loads. */
public record PlatformPackageIngestReceipt(long batchId, long artifactId, String status, List<PlatformIngestReceipt> datasetLoads) {
}
