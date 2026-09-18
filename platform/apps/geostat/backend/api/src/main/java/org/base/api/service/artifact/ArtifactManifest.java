package org.base.api.service.artifact;

import java.util.List;

/**
 * Accepted package manifest ({@value #SCHEMA}). Entries are sorted by original path and the
 * package checksum is computed over that canonical form, so the same package always yields the
 * same manifest identity (idempotent re-ingest).
 */
public record ArtifactManifest(String schema, String packageCode, String packageChecksum, String generatorVersion,
                               String sourceReference, List<Entry> entries, String contractCode, Integer contractRevision,
                               Long datasetVersionId) {
    public static final String SCHEMA = "geostat.artifact-manifest.v1";

    public ArtifactManifest {
        entries = List.copyOf(entries);
        if ((contractCode == null) != (contractRevision == null) || (contractCode == null) != (datasetVersionId == null))
            throw new IllegalArgumentException("Manifest contract identity must be complete or absent");
        if (contractCode != null && (contractCode.isBlank() || contractRevision < 1 || datasetVersionId < 1))
            throw new IllegalArgumentException("Manifest contract identity is invalid");
    }

    /** One file of the package: where it came from, what its bytes are and where they live. */
    public record Entry(String originalPath, String sha256, long byteSize, String mediaType, String bucket, String objectKey) {
        public String originalName() {
            int slash = originalPath.lastIndexOf('/');
            return slash < 0 ? originalPath : originalPath.substring(slash + 1);
        }
    }
}
