package org.base.api.service.storage.s3;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.base.api.service.artifact.ArtifactKeys;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.artifact.ArtifactStorageException;
import org.base.api.service.artifact.Sha256;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** S3 adapter of the immutable artifact byte port. SQL holds references and hashes, never these bytes. */
public class S3ArtifactObjectStore implements ArtifactObjectStore {
    private static final Set<String> ABSENT_CODES = Set.of("NoSuchKey", "NoSuchObject");
    private static final String CHECKSUM_METADATA = "sha256";

    private final S3Clients clients;
    private final String ingestBucket;

    public S3ArtifactObjectStore(S3Clients clients, String ingestBucket) {
        this.clients = clients;
        this.ingestBucket = ingestBucket;
    }

    @Override
    public String ingestBucket() {
        return ingestBucket;
    }

    @Override
    public Optional<ObjectStat> stat(ObjectLocation location) {
        try {
            var stat = client().statObject(StatObjectArgs.builder().bucket(location.bucket()).object(location.key()).build());
            return Optional.of(new ObjectStat(stat.size(), stat.contentType()));
        } catch (ErrorResponseException error) {
            if (ABSENT_CODES.contains(error.errorResponse().code())) return Optional.empty();
            throw new ArtifactStorageException("Object stat failed", error);
        } catch (Exception error) {
            throw new ArtifactStorageException("Object stat failed", error);
        }
    }

    @Override
    public String sha256(ObjectLocation location) {
        try (InputStream input = open(location)) {
            return Sha256.of(input);
        } catch (ArtifactStorageException error) {
            throw error;
        } catch (Exception error) {
            throw new ArtifactStorageException("Object checksum read failed", error);
        }
    }

    @Override
    public InputStream open(ObjectLocation location) {
        try {
            return client().getObject(GetObjectArgs.builder().bucket(location.bucket()).object(location.key()).build());
        } catch (Exception error) {
            throw new ArtifactStorageException("Object read stream could not be opened", error);
        }
    }

    @Override
    public byte[] read(ObjectLocation location, int maxBytes) {
        try (InputStream input = open(location)) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) throw new IllegalArgumentException("Object exceeds " + maxBytes + " bytes");
            return bytes;
        } catch (IllegalArgumentException | ArtifactStorageException error) {
            throw error;
        } catch (Exception error) {
            throw new ArtifactStorageException("Object read failed", error);
        }
    }

    @Override
    public ObjectLocation putContentAddressed(String prefix, String sha256, String extension, String mediaType, InputStream content, long byteSize) {
        ObjectLocation location = new ObjectLocation(ingestBucket, ArtifactKeys.contentKey(prefix, sha256, extension));
        if (stat(location).isPresent()) return location;
        try {
            MessageDigest digest = Sha256.newDigest();
            client().putObject(PutObjectArgs.builder().bucket(location.bucket()).object(location.key()).contentType(mediaType)
                    .userMetadata(Map.of(CHECKSUM_METADATA, sha256))
                    .stream(new DigestInputStream(content, digest), byteSize, -1L).build());
            if (!Sha256.hex(digest).equals(sha256)) {
                client().removeObject(RemoveObjectArgs.builder().bucket(location.bucket()).object(location.key()).build());
                throw new IllegalArgumentException("Uploaded content does not match declared sha256");
            }
            return location;
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new ArtifactStorageException("Object write failed", error);
        }
    }

    @Override
    public URI presignGet(ObjectLocation location, Duration ttl, String downloadName, String mediaType) {
        MinioClient distribution = clients.distribution()
                .orElseThrow(() -> new ArtifactStorageException("Artifact distribution endpoint is not configured", null));
        try {
            String disposition = "attachment; filename*=UTF-8''" + URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
            return URI.create(distribution.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Http.Method.GET)
                    .bucket(location.bucket()).object(location.key()).expiry((int) ttl.toSeconds())
                    .extraQueryParams(Map.of("response-content-disposition", disposition, "response-content-type", mediaType)).build()));
        } catch (Exception error) {
            throw new ArtifactStorageException("Signed URL generation failed", error);
        }
    }

    private MinioClient client() {
        return clients.internal();
    }
}
