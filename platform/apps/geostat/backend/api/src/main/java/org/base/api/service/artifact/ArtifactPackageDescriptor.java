package org.base.api.service.artifact;

import java.util.List;

/**
 * Everything a package producer needs to assemble a contract-conformant package, read from the approved
 * Control Plane only: dataset structure (physical Access table, required and key fields) and the approved
 * artifact relations with their match rules and policies. Producers never hardcode any of it.
 */
public record ArtifactPackageDescriptor(String schema, ArtifactPackageContractResolver.DatasetContract dataset,
                                        List<ArtifactRelationDescriptor> relations) {
    public static final String SCHEMA = "geostat.artifact-package-descriptor.v1";

    public ArtifactPackageDescriptor {
        if (!SCHEMA.equals(schema)) throw new IllegalArgumentException("Unsupported package descriptor schema: " + schema);
        relations = List.copyOf(relations);
    }
}
