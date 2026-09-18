package org.base.api.service.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Serving boundary for published attachments. Returns contract metadata and short-lived signed
 * URLs; never a bucket, object key or storage path. Only the newest PUBLISHED snapshot serves.
 */
@Service
public class ArtifactDistributionService {
    private static final Logger log = LoggerFactory.getLogger(ArtifactDistributionService.class);
    private static final Pattern RECORD_TYPE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final int MAX_EXTERNAL_KEY = 512;

    private final ArtifactAttachmentRepository attachments;
    private final ArtifactContractResolver contracts;
    private final ObjectProvider<ArtifactObjectStore> store;
    private final ArtifactMetrics metrics;
    private final Clock clock;

    public record DownloadPolicy(String mode, int expiresInSeconds) {}

    public record ArtifactDescriptor(String relationCode, ArtifactRole role, String language, int ordinal, String fileName,
                                     String mediaType, long byteSize, String sha256, DownloadPolicy download) {}

    public record PublishedArtifacts(String recordType, String externalKey, long datasetSnapshotId, List<ArtifactDescriptor> artifacts) {}

    public record SignedDownload(URI url, Instant expiresAt, String fileName, String mediaType, long byteSize, String sha256) {}

    public ArtifactDistributionService(ArtifactAttachmentRepository attachments, ArtifactContractResolver contracts,
                                       ObjectProvider<ArtifactObjectStore> store, ArtifactMetrics metrics, ObjectProvider<Clock> clock) {
        this.attachments = attachments;
        this.contracts = contracts;
        this.store = store;
        this.metrics = metrics;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    public PublishedArtifacts list(String recordType, String externalKey) {
        ArtifactAttachmentRepository.PublishedEntity entity = published(recordType, externalKey);
        Map<String, ArtifactRelationDefinition> definitions = contracts.approved(entity.datasetVersionId()).stream()
                .collect(Collectors.toUnmodifiableMap(ArtifactRelationDefinition::relationCode, Function.identity()));
        List<ArtifactDescriptor> artifacts = attachments.attachedObjects(entity).stream()
                .map(a -> {
                    ArtifactRelationDefinition definition = definitions.get(a.relationCode());
                    if (definition == null) throw new IllegalStateException("Published attachment relation is not approved: " + a.relationCode());
                    return new ArtifactDescriptor(a.relationCode(), a.role(), a.language(), a.ordinal(), a.originalName(), a.mediaType(), a.byteSize(), a.sha256(),
                            new DownloadPolicy(definition.policy().accessMode().name(), Math.toIntExact(definition.policy().signedUrlTtl().toSeconds())));
                })
                .toList();
        return new PublishedArtifacts(recordType, externalKey, entity.snapshotId(), artifacts);
    }

    public SignedDownload download(String recordType, String externalKey, String relationCode, String language, int ordinal, Set<String> authorities) {
        ArtifactAttachmentRepository.PublishedEntity entity = published(recordType, externalKey);
        ArtifactRelationDefinition definition = contracts.approved(entity.datasetVersionId(), relationCode)
                .orElseThrow(() -> new ArtifactNotFoundException("Relation " + relationCode + " is not declared for " + recordType));
        ArtifactPolicy policy = definition.policy();
        if (!policy.permits(authorities)) {
            metrics.download(relationCode, "DENIED");
            throw new ArtifactAccessDeniedException("Policy " + policy.policyCode() + " does not permit this download");
        }
        ArtifactAttachmentRepository.AttachedObject attached = attachments.attachedObject(entity, relationCode, language, ordinal)
                .orElseThrow(() -> new ArtifactNotFoundException("No " + relationCode + "/" + language + "/" + ordinal + " artifact for " + recordType + " " + externalKey));
        if (attached.verification() != VerificationStatus.VERIFIED)
            throw new ArtifactStorageException("Artifact object is not verified: " + attached.verification(), null);
        ArtifactObjectStore objects = store.getIfAvailable();
        if (objects == null) throw new ArtifactStorageException("Object Storage is not configured", null);
        URI url = objects.presignGet(new ArtifactObjectStore.ObjectLocation(attached.bucket(), attached.objectKey()), policy.signedUrlTtl(),
                attached.originalName(), attached.mediaType());
        Instant expiresAt = clock.instant().plus(policy.signedUrlTtl());
        metrics.download(relationCode, "ISSUED");
        log.info("artifact.download issued recordType={} key={} relation={} language={} ordinal={} sha256={} snapshot={} ttlSeconds={}",
                recordType, externalKey, relationCode, language, ordinal, attached.sha256(), entity.snapshotId(), policy.signedUrlTtl().toSeconds());
        return new SignedDownload(url, expiresAt, attached.originalName(), attached.mediaType(), attached.byteSize(), attached.sha256());
    }

    private ArtifactAttachmentRepository.PublishedEntity published(String recordType, String externalKey) {
        if (recordType == null || !RECORD_TYPE.matcher(recordType).matches()) throw new IllegalArgumentException("Invalid record type");
        if (externalKey == null || externalKey.isBlank() || externalKey.length() > MAX_EXTERNAL_KEY) throw new IllegalArgumentException("Invalid external key");
        return attachments.newestPublished(recordType, externalKey)
                .orElseThrow(() -> new ArtifactNotFoundException("No published " + recordType + " with key " + externalKey));
    }
}
