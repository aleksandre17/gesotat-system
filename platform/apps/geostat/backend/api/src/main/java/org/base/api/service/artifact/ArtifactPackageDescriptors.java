package org.base.api.service.artifact;

import org.springframework.stereotype.Component;

/** Composes the package descriptor from the two Control Plane authorities (dataset contract, relation contract). */
@Component
public class ArtifactPackageDescriptors {
    private final ArtifactPackageContractResolver datasets;
    private final ArtifactContractResolver relations;

    public ArtifactPackageDescriptors(ArtifactPackageContractResolver datasets, ArtifactContractResolver relations) {
        this.datasets = datasets;
        this.relations = relations;
    }

    public ArtifactPackageDescriptor describe(String contractCode, int revision, String datasetCode) {
        var dataset = datasets.resolve(contractCode, revision, datasetCode);
        return new ArtifactPackageDescriptor(ArtifactPackageDescriptor.SCHEMA, dataset, relations.approvedDescriptors(dataset.datasetVersionId()));
    }
}
