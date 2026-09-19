package org.base.api.service.artifact;

import java.io.InputStream;
import java.util.UUID;

/**
 * Port for durable, private checkpoints of a resumable upload. Keys are derived by the provider from
 * the session, part and checksum; callers never see or supply a storage key.
 */
public interface ArtifactUploadStagingStore {

    /** Writes one part; replaying identical bytes is a no-op, different bytes under the same identity is a conflict. */
    void putPart(UUID uploadSessionId, int partNumber, String sha256, InputStream content, long byteSize);

    /** Opens a previously checkpointed part. The caller owns and must close the stream. */
    InputStream openPart(UUID uploadSessionId, int partNumber, String sha256);

    void deletePart(UUID uploadSessionId, int partNumber, String sha256);
}
