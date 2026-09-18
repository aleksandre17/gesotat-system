package org.base.api.service.artifact;

/** Fail-closed signal mapped to service unavailable when a required scanner cannot decide. */
public class ArtifactScannerUnavailableException extends RuntimeException {
    public ArtifactScannerUnavailableException(String message, Throwable cause) { super(message, cause); }
    public ArtifactScannerUnavailableException(String message) { super(message); }
}
