package org.base.api.service.storage.s3;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.base.api.service.artifact.ArtifactKeys;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.artifact.ArtifactObjectStore.ObjectLocation;
import org.base.api.service.artifact.ArtifactStorageException;
import org.base.api.service.artifact.ArtifactUploadStagingStore;

import java.io.InputStream;
import java.util.UUID;

/** Upload checkpoints as checksum-addressed objects under {@code <stagingPrefix><session>/<part>/}. */
public class S3ArtifactUploadStagingStore implements ArtifactUploadStagingStore {
    private static final String PART_EXTENSION = "part";
    private static final String PART_MEDIA_TYPE = "application/octet-stream";

    private final MinioClient client;
    private final ArtifactObjectStore objects;
    private final String stagingPrefix;

    public S3ArtifactUploadStagingStore(MinioClient client, ArtifactObjectStore objects, String stagingPrefix) {
        if (stagingPrefix == null || stagingPrefix.isBlank() || !stagingPrefix.endsWith("/"))
            throw new IllegalArgumentException("storage.s3.staging-prefix must be a non-empty prefix ending with a slash");
        this.client = client;
        this.objects = objects;
        this.stagingPrefix = stagingPrefix;
    }

    @Override
    public void putPart(UUID uploadSessionId, int partNumber, String sha256, InputStream content, long byteSize) {
        if (byteSize < 1) throw new IllegalArgumentException("Invalid upload part identity");
        ObjectLocation location = location(uploadSessionId, partNumber, sha256);
        var existing = objects.stat(location);
        if (existing.isPresent()) {
            if (existing.get().byteSize() != byteSize || !objects.sha256(location).equals(sha256))
                throw new IllegalStateException("Staged upload checkpoint conflicts with existing bytes");
            return;
        }
        objects.putContentAddressed(partPrefix(uploadSessionId, partNumber), sha256, PART_EXTENSION, PART_MEDIA_TYPE, content, byteSize);
    }

    @Override
    public InputStream openPart(UUID uploadSessionId, int partNumber, String sha256) {
        return objects.open(location(uploadSessionId, partNumber, sha256));
    }

    @Override
    public void deletePart(UUID uploadSessionId, int partNumber, String sha256) {
        ObjectLocation location = location(uploadSessionId, partNumber, sha256);
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(location.bucket()).object(location.key()).build());
        } catch (Exception error) {
            throw new ArtifactStorageException("Upload checkpoint cleanup failed", error);
        }
    }

    /** The key is always rebuilt from server-side identity, so it cannot leave the session namespace. */
    private ObjectLocation location(UUID uploadSessionId, int partNumber, String sha256) {
        ArtifactKeys.requireSha256(sha256);
        return new ObjectLocation(objects.ingestBucket(), ArtifactKeys.contentKey(partPrefix(uploadSessionId, partNumber), sha256, PART_EXTENSION));
    }

    private String partPrefix(UUID uploadSessionId, int partNumber) {
        if (uploadSessionId == null || partNumber < 1) throw new IllegalArgumentException("Invalid upload part identity");
        return stagingPrefix + uploadSessionId + "/" + partNumber + "/";
    }
}
