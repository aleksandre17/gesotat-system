package org.base.api.service.artifact.run;

/** Names of the values the built-in stages exchange through {@link PackageRunState}. */
public final class PackageRunKeys {
    public static final String BATCH_ID = "batchId";
    public static final String INGEST_ARTIFACT_ID = "ingestArtifactId";
    public static final String DATASET_CHECKSUM = "datasetChecksum";
    public static final String DATASET_LOAD_ID = "datasetLoadId";
    public static final String DATASET_SNAPSHOT_ID = "datasetSnapshotId";
    public static final String ATTACHMENT_CHECKSUM = "attachmentChecksum";
    public static final String GATE_FACTS_DIGEST = "gateFactsDigest";
    public static final String SNAPSHOT_STATUS = "snapshotStatus";

    private PackageRunKeys() {}
}
