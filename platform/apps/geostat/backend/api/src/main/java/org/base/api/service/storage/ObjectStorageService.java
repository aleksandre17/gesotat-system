package org.base.api.service.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.CopyObjectArgs;
import io.minio.SourceObject;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.Http;
import org.base.api.service.artifact.ArtifactKeys;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.artifact.ArtifactStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** Private S3-compatible artifact store. SQL contains references and hashes, never source binaries. */
@Service
@ConditionalOnExpression("'${storage.s3.endpoint:}'.length() > 0")
public class ObjectStorageService implements ArtifactObjectStore {
    private final MinioClient minio;
    /** Signs URLs for the host a consumer can reach; absent means distribution is disabled (fail closed). */
    private final MinioClient distribution;
    private final String ingestBucket;
    private final String quarantineBucket;
    private final String archiveBucket;
    private final String exportBucket;

    public ObjectStorageService(@Value("${storage.s3.endpoint}") String endpoint,
                                @Value("${storage.s3.access-key}") String accessKey,
                                @Value("${storage.s3.secret-key}") String secretKey,
                                @Value("${storage.s3.ingest-bucket}") String ingestBucket,
                                @Value("${storage.s3.quarantine-bucket}") String quarantineBucket,
                                @Value("${storage.s3.archive-bucket}") String archiveBucket,
                                @Value("${storage.s3.export-bucket}") String exportBucket,
                                @Value("${storage.s3.public-endpoint:}") String publicEndpoint,
                                @Value("${storage.s3.region:us-east-1}") String region) {
        this.minio = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.distribution = publicEndpoint == null || publicEndpoint.isBlank() ? null
                /* An explicit region keeps signing offline; otherwise the SDK looks it up over the network. */
                : MinioClient.builder().endpoint(publicEndpoint).credentials(accessKey, secretKey).region(region).build();
        this.ingestBucket = ingestBucket;
        this.quarantineBucket = quarantineBucket;
        this.archiveBucket = archiveBucket;
        this.exportBucket = exportBucket;
    }

    public StoredArtifact storeOriginal(String originalName, String contentType, InputStream content, long size) throws Exception {
        provision();
        String safeName = originalName == null ? "artifact" : originalName.replaceAll("[^A-Za-z0-9._-]", "_");
        String objectName = "incoming/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + safeName;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        DigestInputStream digestingStream = new DigestInputStream(content, digest);
        minio.putObject(PutObjectArgs.builder().bucket(ingestBucket).object(objectName)
                .contentType(contentType == null ? "application/octet-stream" : contentType)
                .stream(digestingStream, size >= 0 ? size : -1, 10L * 1024 * 1024).build());
        return new StoredArtifact("s3://" + ingestBucket + "/" + objectName, hex(digest.digest()), size);
    }

    /** Stores an immutable archive record payload under a deterministic key. */
    public String storeArchivePayload(long archiveSnapshotId, long archiveRecordId, String payload) throws Exception {
        provision();
        byte[] bytes = (payload == null ? "" : payload).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String objectName = "snapshot/" + archiveSnapshotId + "/record/" + archiveRecordId + ".json";
        minio.putObject(PutObjectArgs.builder().bucket(archiveBucket).object(objectName)
                .contentType("application/json").stream(new ByteArrayInputStream(bytes), (long) bytes.length, -1L).build());
        return "s3://" + archiveBucket + "/" + objectName;
    }

    /** Materializes the immutable, checksum-addressed artifact for a resumable worker. */
    public void materialize(String sourceUri, Path destination) throws Exception {
        String[] source = parseS3(sourceUri);
        try (InputStream input = minio.getObject(GetObjectArgs.builder().bucket(source[0]).object(source[1]).build())) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Non-mutating readiness probe for monitoring. */
    public boolean isReady() {
        try { return buckets().stream().allMatch(bucket -> {
            try { return minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()); }
            catch (Exception error) { return false; }
        }); }
        catch (Exception ignored) { return false; }
    }

    public void provision() throws Exception { for (String bucket : buckets()) ensureBucket(bucket); }

    /** Keeps the original immutable object and makes a separate auditable quarantine copy. */
    public String quarantine(String sourceUri) throws Exception {
        provision();
        String[] source=parseS3(sourceUri);
        String object="quarantine/"+LocalDate.now()+"/"+UUID.randomUUID()+"-"+source[1].replace('/','_');
        minio.copyObject(CopyObjectArgs.builder().bucket(quarantineBucket).object(object)
                .source(SourceObject.builder().bucket(source[0]).object(source[1]).build()).build());
        return "s3://"+quarantineBucket+"/"+object;
    }

    @Override
    public String ingestBucket() { return ingestBucket; }

    @Override
    public Optional<ObjectStat> stat(ObjectLocation location) {
        try {
            var stat = minio.statObject(StatObjectArgs.builder().bucket(location.bucket()).object(location.key()).build());
            return Optional.of(new ObjectStat(stat.size(), stat.contentType()));
        } catch (ErrorResponseException error) {
            if ("NoSuchKey".equals(error.errorResponse().code()) || "NoSuchObject".equals(error.errorResponse().code())) return Optional.empty();
            throw new ArtifactStorageException("Object stat failed", error);
        } catch (Exception error) {
            throw new ArtifactStorageException("Object stat failed", error);
        }
    }

    @Override
    public String sha256(ObjectLocation location) {
        try (InputStream input = minio.getObject(GetObjectArgs.builder().bucket(location.bucket()).object(location.key()).build())) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = input.read(buffer)) > 0; ) digest.update(buffer, 0, read);
            return hex(digest.digest());
        } catch (Exception error) {
            throw new ArtifactStorageException("Object checksum read failed", error);
        }
    }

    @Override
    public InputStream open(ObjectLocation location) {
        try {
            return minio.getObject(GetObjectArgs.builder().bucket(location.bucket()).object(location.key()).build());
        } catch (Exception error) {
            throw new ArtifactStorageException("Object read stream could not be opened", error);
        }
    }

    @Override
    public byte[] read(ObjectLocation location, int maxBytes) {
        try (InputStream input = minio.getObject(GetObjectArgs.builder().bucket(location.bucket()).object(location.key()).build())) {
            byte[] bytes = input.readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) throw new IllegalArgumentException("Object exceeds " + maxBytes + " bytes");
            return bytes;
        } catch (IllegalArgumentException error) {
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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            minio.putObject(PutObjectArgs.builder().bucket(location.bucket()).object(location.key()).contentType(mediaType)
                    .userMetadata(Map.of("sha256", sha256))
                    .stream(new DigestInputStream(content, digest), byteSize, -1L).build());
            String actual = hex(digest.digest());
            if (!actual.equals(sha256)) {
                minio.removeObject(io.minio.RemoveObjectArgs.builder().bucket(location.bucket()).object(location.key()).build());
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
        if (distribution == null) throw new ArtifactStorageException("Artifact distribution endpoint is not configured", null);
        try {
            String disposition = "attachment; filename*=UTF-8''" + URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
            return URI.create(distribution.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Http.Method.GET)
                    .bucket(location.bucket()).object(location.key()).expiry((int) ttl.toSeconds())
                    .extraQueryParams(Map.of("response-content-disposition", disposition, "response-content-type", mediaType)).build()));
        } catch (Exception error) {
            throw new ArtifactStorageException("Signed URL generation failed", error);
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private java.util.List<String> buckets() { return java.util.List.of(ingestBucket, quarantineBucket, archiveBucket, exportBucket); }

    private static String[] parseS3(String uri) {
        if(uri==null||!uri.startsWith("s3://"))throw new IllegalArgumentException("Expected s3:// URI");
        String value=uri.substring(5); int slash=value.indexOf('/'); if(slash<1||slash==value.length()-1)throw new IllegalArgumentException("Invalid s3 URI");
        return new String[]{value.substring(0,slash),value.substring(slash+1)};
    }

    private static String hex(byte[] digest) {
        StringBuilder output = new StringBuilder(64);
        for (byte value : digest) output.append(String.format("%02x", value));
        return output.toString();
    }
}
