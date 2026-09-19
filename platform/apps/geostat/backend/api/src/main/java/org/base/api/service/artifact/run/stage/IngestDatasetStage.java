package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.ArtifactKeys;
import org.base.api.service.artifact.ArtifactManifest;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.artifact.ArtifactRegistry;
import org.base.api.service.artifact.ArtifactStorageException;
import org.base.api.service.artifact.run.PackageDatasetIngestor;
import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunRepository;
import org.base.api.service.artifact.run.PackageRunStage;
import org.base.api.service.storage.StoredArtifact;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/** Stages the dataset file of the package, read back from its verified content address. */
@Component
public class IngestDatasetStage implements PackageRunStage {
    private final ArtifactRegistry registry;
    private final ObjectProvider<ArtifactObjectStore> stores;
    private final List<PackageDatasetIngestor> ingestors;
    private final PackageRunRepository runs;

    public IngestDatasetStage(ArtifactRegistry registry, ObjectProvider<ArtifactObjectStore> stores,
                              List<PackageDatasetIngestor> ingestors, PackageRunRepository runs) {
        this.registry = registry;
        this.stores = stores;
        this.ingestors = List.copyOf(ingestors);
        this.runs = runs;
    }

    @Override
    public String code() {
        return "INGEST_DATASET";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public Result execute(PackageRun run) throws Exception {
        ArtifactObjectStore store = stores.getIfAvailable();
        if (store == null) throw new ArtifactStorageException("Object Storage is not configured", null);
        for (ArtifactRegistry.StoredEntry stored : registry.entries(run.manifestId())) {
            ArtifactManifest.Entry entry = stored.entry();
            String extension = ArtifactKeys.extensionOf(entry.originalPath());
            var ingestor = ingestors.stream().filter(candidate -> candidate.ingests(extension)).findFirst();
            if (ingestor.isEmpty()) continue;
            Path local = Files.createTempFile("package-run-dataset-", "." + extension);
            try {
                try (InputStream content = store.open(new ArtifactObjectStore.ObjectLocation(entry.bucket(), entry.objectKey()))) {
                    Files.copy(content, local, StandardCopyOption.REPLACE_EXISTING);
                }
                var receipt = ingestor.get().ingest(local.toFile(),
                        new StoredArtifact("s3://" + entry.bucket() + "/" + entry.objectKey(), entry.sha256(), entry.byteSize()));
                var loadId = runs.datasetLoad(receipt.batchId(), run.datasetVersionId());
                if (loadId.isEmpty())
                    return Result.blocked(run.state(), "DATASET_NOT_IN_PACKAGE", Map.of("batchId", receipt.batchId()));
                return Result.completed(run.state().with(PackageRunKeys.BATCH_ID, receipt.batchId())
                                .with(PackageRunKeys.INGEST_ARTIFACT_ID, receipt.artifactId())
                                .with(PackageRunKeys.DATASET_CHECKSUM, entry.sha256())
                                .with(PackageRunKeys.DATASET_LOAD_ID, loadId.get()),
                        Map.of("batchId", receipt.batchId(), "datasetLoadId", loadId.get(), "datasetLoads", receipt.datasetLoads().size()));
            } finally {
                Files.deleteIfExists(local);
            }
        }
        return Result.blocked(run.state(), "NO_DATASET_FILE", Map.of());
    }
}
