package org.base.api.service.artifact.run;

import org.base.api.service.artifact.AccessDatasetCarrier;
import org.base.api.service.platform.PlatformPackageIngestReceipt;
import org.base.api.service.platform.access.SemanticAccessPackageIngestionService;
import org.base.api.service.storage.StoredArtifact;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class AccessPackageDatasetIngestor implements PackageDatasetIngestor {
    private final AccessDatasetCarrier carrier;
    private final SemanticAccessPackageIngestionService ingestion;

    public AccessPackageDatasetIngestor(AccessDatasetCarrier carrier, SemanticAccessPackageIngestionService ingestion) {
        this.carrier = carrier;
        this.ingestion = ingestion;
    }

    @Override
    public boolean ingests(String extension) {
        return carrier.carries(extension);
    }

    @Override
    public PlatformPackageIngestReceipt ingest(File datasetFile, StoredArtifact artifact) throws Exception {
        return ingestion.ingest(datasetFile, artifact);
    }
}
