package org.base.api.service.artifact;

/** A tenant has exhausted its currently reserved upload capacity. */
public class ArtifactUploadQuotaExceededException extends RuntimeException {
    public ArtifactUploadQuotaExceededException() { super("Upload size exceeds the available tenant quota"); }
}
