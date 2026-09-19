package org.base.api.service.artifact.run;

import org.base.api.service.platform.PlatformPackageIngestReceipt;
import org.base.api.service.storage.StoredArtifact;

import java.io.File;

/**
 * Stages the rows of a package dataset file into the ingestion pipeline. One bean per dataset format;
 * the run never names a format.
 */
public interface PackageDatasetIngestor {

    boolean ingests(String extension);

    /** Must be idempotent for the same stored artifact: a replay returns the batch it created before. */
    PlatformPackageIngestReceipt ingest(File datasetFile, StoredArtifact artifact) throws Exception;
}
