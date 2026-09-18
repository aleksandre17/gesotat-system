package org.base.api.service.artifact;

/** Object Storage is unavailable or refused an operation; callers map it to 503, never to a silent fallback. */
public class ArtifactStorageException extends RuntimeException {
    public ArtifactStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
