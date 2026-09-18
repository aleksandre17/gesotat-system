package org.base.api.service.artifact;

/** Requested artifact, manifest, entity or snapshot does not exist in the governed scope. */
public class ArtifactNotFoundException extends RuntimeException {
    public ArtifactNotFoundException(String message) {
        super(message);
    }
}
