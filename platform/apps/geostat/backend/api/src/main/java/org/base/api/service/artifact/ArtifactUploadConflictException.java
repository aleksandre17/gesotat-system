package org.base.api.service.artifact;

/** A valid upload operation conflicts with the durable session's current state. */
public final class ArtifactUploadConflictException extends IllegalStateException {
    public ArtifactUploadConflictException(String message) {
        super(message);
    }
}
