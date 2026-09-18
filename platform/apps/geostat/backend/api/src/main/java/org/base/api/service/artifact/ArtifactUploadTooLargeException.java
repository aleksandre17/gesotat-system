package org.base.api.service.artifact;

/** The declared upload exceeds the configured per-package admission limit. */
public final class ArtifactUploadTooLargeException extends RuntimeException {
    public ArtifactUploadTooLargeException() {
        super("Upload exceeds the configured maximum package size");
    }
}
