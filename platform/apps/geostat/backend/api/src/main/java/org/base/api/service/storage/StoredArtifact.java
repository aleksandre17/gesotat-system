package org.base.api.service.storage;

/** Database-safe reference to an immutable original artifact. */
public record StoredArtifact(String objectUri, String checksum, long byteSize) {
}
