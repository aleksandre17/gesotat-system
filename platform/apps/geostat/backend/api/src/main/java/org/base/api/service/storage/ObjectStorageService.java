package org.base.api.service.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.CopyObjectArgs;
import io.minio.SourceObject;
import io.minio.GetObjectArgs;
import org.base.api.service.storage.s3.S3Clients;
import org.base.api.service.storage.s3.S3StorageProperties;
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

/**
 * Ingestion-side object operations: original source receipt, quarantine copy, archive payload, bucket
 * provisioning and readiness. Immutable artifact bytes are served by the artifact storage ports.
 */
@Service
@ConditionalOnExpression("'${storage.s3.endpoint:}'.length() > 0")
public class ObjectStorageService {
    private final MinioClient minio;
    private final String ingestBucket;
    private final String quarantineBucket;
    private final String archiveBucket;
    private final String exportBucket;

    public ObjectStorageService(S3Clients clients, S3StorageProperties properties) {
        this.minio = clients.internal();
        this.ingestBucket = properties.ingestBucket();
        this.quarantineBucket = properties.quarantineBucket();
        this.archiveBucket = properties.archiveBucket();
        this.exportBucket = properties.exportBucket();
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
