package org.base.api.service.artifact;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/** Port for immutable artifact bytes. The domain never sees a provider SDK or credentials. */
public interface ArtifactObjectStore {

    /** Physical address of one object. */
    record ObjectLocation(String bucket, String key) {
        public ObjectLocation {
            if (bucket == null || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) throw new IllegalArgumentException("Invalid bucket name");
            ArtifactKeys.requireSafeKey(key);
        }
    }

    record ObjectStat(long byteSize, String contentType) {}

    /** Private bucket where accepted package content lives. */
    String ingestBucket();

    Optional<ObjectStat> stat(ObjectLocation location);

    /** Streams the object and returns its lowercase SHA-256 hex digest. */
    String sha256(ObjectLocation location);

    /** Opens the full object as a stream. The caller owns and must close the returned stream. */
    InputStream open(ObjectLocation location);

    /** Reads a small object fully; fails when it exceeds {@code maxBytes}. */
    byte[] read(ObjectLocation location, int maxBytes);

    /** Writes content under its checksum address; an existing object with the same address is kept. */
    ObjectLocation putContentAddressed(String prefix, String sha256, String extension, String mediaType, InputStream content, long byteSize);


    /** Short-lived GET URL for the public distribution endpoint. */
    URI presignGet(ObjectLocation location, Duration ttl, String downloadName, String mediaType);
}
