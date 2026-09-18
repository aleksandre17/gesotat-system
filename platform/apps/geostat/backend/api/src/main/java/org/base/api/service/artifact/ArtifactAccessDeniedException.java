package org.base.api.service.artifact;

/** Caller is authenticated but the artifact policy does not permit distribution to them. */
public class ArtifactAccessDeniedException extends RuntimeException {
    public ArtifactAccessDeniedException(String message) {
        super(message);
    }
}
