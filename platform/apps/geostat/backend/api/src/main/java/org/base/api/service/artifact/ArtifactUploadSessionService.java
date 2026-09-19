package org.base.api.service.artifact;

import org.base.api.service.artifact.upload.UploadSession;
import org.base.api.service.artifact.upload.UploadSessionRepository;
import org.base.api.service.artifact.upload.UploadSessionStatus;
import org.base.api.service.platform.PlatformSchemaReadiness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** Durable, tenant-scoped upload sessions with object-storage part checkpoints and quota reservations. */
@Service
public class ArtifactUploadSessionService {
    private static final Pattern PACKAGE_CODE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]{0,159}");
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{15,127}");
    private static final int CLEANUP_BATCH = 50;

    private final UploadSessionRepository sessions;
    private final TransactionTemplate transaction;
    private final ArtifactUploadStagingStore staging;
    private final ArtifactPackageContractResolver contracts;
    private final ArtifactPackageService packages;
    private final ArtifactProperties properties;
    private final PlatformSchemaReadiness schemaReadiness;
    private final Clock clock;

    public record StartRequest(String packageCode, String contractCode, int revision, String datasetCode, long expectedBytes) {}
    public record SessionReceipt(UUID uploadSessionId, long partSize, int expectedParts, long receivedBytes, long expectedBytes,
                                 String status, Instant expiresAt, Long manifestId, String packageChecksum) {}
    public record PartReceipt(UUID uploadSessionId, int partNumber, String sha256, long byteSize, boolean replayed) {}

    private enum Checkpoint { STORED, REPLAYED, LOST }

    public ArtifactUploadSessionService(UploadSessionRepository sessions,
                                        @Qualifier("dataPlaneTransactionManager") PlatformTransactionManager transactionManager,
                                        ObjectProvider<ArtifactUploadStagingStore> staging,
                                        ArtifactPackageContractResolver contracts,
                                        ArtifactPackageService packages,
                                        ArtifactProperties properties,
                                        PlatformSchemaReadiness schemaReadiness,
                                        ObjectProvider<Clock> clock) {
        this.sessions = sessions;
        this.transaction = new TransactionTemplate(transactionManager);
        this.staging = staging.getIfAvailable();
        this.contracts = contracts;
        this.packages = packages;
        this.properties = properties;
        this.schemaReadiness = schemaReadiness;
        this.clock = clock.getIfAvailable(Clock::systemUTC);
    }

    public SessionReceipt start(StartRequest request, ArtifactUploadIdentityResolver.Identity identity, String idempotencyKey) {
        validateStart(request, idempotencyKey);
        ArtifactPackageContractResolver.DatasetContract contract = contracts.resolve(request.contractCode(), request.revision(), request.datasetCode());
        String fingerprint = fingerprint(request, contract.datasetVersionId());
        long partSize = properties.getUploadPartBytes();
        int expectedParts = Math.toIntExact(1 + (request.expectedBytes() - 1) / partSize);
        return transaction.execute(status -> {
            var prior = sessions.lockByIdempotencyKey(identity, idempotencyKey);
            if (prior.isPresent()) {
                if (!prior.get().requestFingerprint().equals(fingerprint))
                    throw new ArtifactUploadConflictException("Idempotency-Key was already used for a different upload request");
                return receipt(prior.get());
            }
            var quota = sessions.lockQuota(identity.tenantKeyHash());
            UploadSession.Quota current = quota.orElse(new UploadSession.Quota(0, 0));
            if (request.expectedBytes() > properties.getMaxTenantReservedUploadBytes() - current.reservedBytes()
                    || current.activeSessions() >= properties.getMaxTenantActiveUploadSessions())
                throw new ArtifactUploadQuotaExceededException();
            sessions.reserveQuota(identity.tenantKeyHash(), request.expectedBytes(), quota.isEmpty());
            UUID id = UUID.randomUUID();
            Instant expires = clock.instant().plusSeconds(properties.getUploadSessionTtlSeconds());
            sessions.insert(id, identity, idempotencyKey, fingerprint, request.packageCode(), contract, request.expectedBytes(), partSize, expectedParts, expires);
            return new SessionReceipt(id, partSize, expectedParts, 0, request.expectedBytes(), UploadSessionStatus.OPEN.name(), expires, null, null);
        });
    }

    public PartReceipt uploadPart(UUID sessionId, int partNumber, long rangeStart, long rangeEnd, long rangeTotal,
                                  String sha256, InputStream content, long byteSize,
                                  ArtifactUploadIdentityResolver.Identity identity) {
        ArtifactKeys.requireSha256(sha256);
        if (staging == null) throw new ArtifactStorageException("Object Storage is not configured", null);
        boolean alreadyReceived;
        try {
            alreadyReceived = Boolean.TRUE.equals(transaction.execute(status -> reservePart(sessionId, partNumber, rangeStart, rangeEnd, rangeTotal, sha256, byteSize, identity)));
        } catch (DuplicateKeyException race) {
            throw new ArtifactUploadConflictException("Concurrent upload part reservation conflict");
        }
        if (alreadyReceived) return new PartReceipt(sessionId, partNumber, sha256, byteSize, true);
        try {
            staging.putPart(sessionId, partNumber, sha256, content, byteSize);
        } catch (IllegalArgumentException invalidDigest) {
            sessions.discardReservedPart(sessionId, partNumber, sha256);
            throw invalidDigest;
        }
        Checkpoint checkpoint = transaction.execute(status -> checkpoint(sessionId, partNumber, sha256, byteSize, identity));
        if (checkpoint == null || checkpoint == Checkpoint.LOST) {
            staging.deletePart(sessionId, partNumber, sha256);
            throw new ArtifactUploadConflictException("Upload session changed while the part was being stored");
        }
        return new PartReceipt(sessionId, partNumber, sha256, byteSize, checkpoint == Checkpoint.REPLAYED);
    }

    /** @return true when identical bytes were already checkpointed for this part */
    private boolean reservePart(UUID sessionId, int partNumber, long rangeStart, long rangeEnd, long rangeTotal, String sha256, long byteSize,
                                ArtifactUploadIdentityResolver.Identity identity) {
        UploadSession session = owned(sessionId, identity, true);
        if (!session.status().active()) throw new ArtifactUploadConflictException("Upload session is not accepting parts");
        if (session.expiredAt(clock.instant())) throw new ArtifactUploadConflictException("Upload session has expired");
        if (partNumber < 1 || partNumber > session.expectedParts()) throw new IllegalArgumentException("Part number is outside the declared upload");
        long start = session.partStart(partNumber);
        long size = session.partLength(partNumber);
        if (rangeTotal != session.expectedBytes() || rangeStart != start || rangeEnd != start + size - 1 || byteSize != size)
            throw new IllegalArgumentException("Content-Range or part size does not match the upload session");
        var existing = sessions.part(sessionId, partNumber, true);
        if (existing.isPresent() && (!existing.get().sha256().equals(sha256) || existing.get().byteSize() != byteSize))
            throw new ArtifactUploadConflictException("Part number was already reserved for different bytes");
        if (existing.isEmpty()) sessions.reservePart(sessionId, partNumber, sha256, byteSize);
        return existing.isPresent() && existing.get().received();
    }

    private Checkpoint checkpoint(UUID sessionId, int partNumber, String sha256, long byteSize, ArtifactUploadIdentityResolver.Identity identity) {
        UploadSession session = owned(sessionId, identity, true);
        if (!session.status().active()) return Checkpoint.LOST;
        if (!sessions.markPartReceived(sessionId, partNumber, sha256)) {
            // A concurrent request for the same bytes won the checkpoint; this one is a replay of it.
            return sessions.part(sessionId, partNumber, false)
                    .filter(part -> part.received() && part.sha256().equals(sha256) && part.byteSize() == byteSize)
                    .map(part -> Checkpoint.REPLAYED).orElse(Checkpoint.LOST);
        }
        sessions.recordProgress(session);
        return Checkpoint.STORED;
    }

    public SessionReceipt complete(UUID sessionId, ArtifactUploadIdentityResolver.Identity identity) {
        UploadSession session = transaction.execute(status -> {
            UploadSession current = owned(sessionId, identity, true);
            if (current.status() == UploadSessionStatus.COMMITTED) return current;
            if (current.status() == UploadSessionStatus.PROCESSING) throw new ArtifactUploadConflictException("Upload completion is already running");
            if (!current.status().active()) throw new ArtifactUploadConflictException("Upload session cannot be completed from its current state");
            if (current.expiredAt(clock.instant())) throw new ArtifactUploadConflictException("Upload session has expired");
            if (sessions.parts(sessionId, true).size() != current.expectedParts() || current.receivedBytes() != current.expectedBytes())
                throw new ArtifactUploadConflictException("Upload is incomplete; all parts must be checkpointed before completion");
            sessions.markProcessing(sessionId);
            return current.withStatus(UploadSessionStatus.PROCESSING);
        });
        if (session.status() == UploadSessionStatus.COMMITTED) return receipt(session);
        try {
            List<UploadSession.Part> parts = sessions.parts(sessionId, true);
            ArtifactPackageService.ManifestReceipt manifest;
            try (InputStream joined = new SequenceInputStream(Collections.enumeration(parts.stream()
                    .map(part -> new LazyPartStream(() -> openVerifiedPart(sessionId, part))).toList()))) {
                manifest = packages.uploadPackage(session.packageCode(), session.contractCode(), session.contractRevision(), session.datasetCode(), joined);
            }
            transaction.executeWithoutResult(status -> finish(session, UploadSessionStatus.COMMITTED, null, manifest.manifestId(), manifest.packageChecksum()));
            cleanupParts(sessionId);
            return new SessionReceipt(session.id(), session.partSize(), session.expectedParts(), session.receivedBytes(), session.expectedBytes(),
                    UploadSessionStatus.COMMITTED.name(), session.expiresAt(), manifest.manifestId(), manifest.packageChecksum());
        } catch (Exception failure) {
            boolean retryable = failure instanceof ArtifactStorageException || failure instanceof DataAccessException;
            transaction.executeWithoutResult(status -> finish(session, retryable ? UploadSessionStatus.RETRYABLE : UploadSessionStatus.REJECTED,
                    retryable ? "DEPENDENCY_UNAVAILABLE" : "PACKAGE_INVALID", null, null));
            if (!retryable) cleanupParts(sessionId);
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new ArtifactStorageException("Resumable package could not be assembled", failure);
        }
    }

    public SessionReceipt status(UUID sessionId, ArtifactUploadIdentityResolver.Identity identity) {
        UploadSession session = owned(sessionId, identity, false);
        if (session.status().active() && session.expiredAt(clock.instant())) {
            transaction.executeWithoutResult(status -> finish(session, UploadSessionStatus.EXPIRED, "SESSION_EXPIRED", null, null));
            return receipt(session.withStatus(UploadSessionStatus.EXPIRED));
        }
        return receipt(session);
    }

    public void cancel(UUID sessionId, ArtifactUploadIdentityResolver.Identity identity) {
        transaction.executeWithoutResult(status -> {
            UploadSession current = owned(sessionId, identity, true);
            if (current.status() == UploadSessionStatus.CANCELLED) return;
            if (!current.status().active()) throw new ArtifactUploadConflictException("Only an open or retryable upload can be cancelled");
            finish(current, UploadSessionStatus.CANCELLED, null, null, null);
        });
        cleanupParts(sessionId);
    }

    @Scheduled(fixedDelayString = "${platform.artifacts.upload-cleanup-delay-ms:60000}")
    public void expireAndClean() {
        if (!schemaReadiness.isReady()) return;
        sessions.requeueStaleProcessing(properties.getUploadProcessingLeaseSeconds());
        for (UploadSession candidate : sessions.cleanupCandidates(CLEANUP_BATCH)) {
            if (candidate.status().active())
                transaction.executeWithoutResult(status -> finish(candidate, UploadSessionStatus.EXPIRED, "SESSION_EXPIRED", null, null));
            cleanupParts(candidate.id());
        }
    }

    /** Moves the session and, when the new state is terminal, returns its quota exactly once. */
    private void finish(UploadSession session, UploadSessionStatus next, String errorCode, Long manifestId, String packageChecksum) {
        if (!sessions.transition(session, next, errorCode, manifestId, packageChecksum)) return;
        if (next.releasesQuota() && !session.quotaReleased() && !sessions.releaseQuota(session))
            throw new IllegalStateException("Upload quota reservation is inconsistent");
    }

    private void cleanupParts(UUID sessionId) {
        if (staging == null) return;
        try {
            for (UploadSession.Part part : sessions.parts(sessionId, false)) staging.deletePart(sessionId, part.partNumber(), part.sha256());
            sessions.deleteParts(sessionId);
        } catch (RuntimeException failure) {
            // The durable part rows remain as retry checkpoints for the scheduled cleanup.
        }
    }

    private InputStream openVerifiedPart(UUID sessionId, UploadSession.Part part) {
        return ArtifactUploadPartIntegrity.verifyOnRead(staging.openPart(sessionId, part.partNumber(), part.sha256()), part.sha256(), part.byteSize());
    }

    private UploadSession owned(UUID sessionId, ArtifactUploadIdentityResolver.Identity identity, boolean lock) {
        return sessions.owned(sessionId, identity, lock).orElseThrow(() -> new ArtifactNotFoundException("Upload session not found"));
    }

    private static SessionReceipt receipt(UploadSession session) {
        return new SessionReceipt(session.id(), session.partSize(), session.expectedParts(), session.receivedBytes(), session.expectedBytes(),
                session.status().name(), session.expiresAt(), session.manifestId(), session.packageChecksum());
    }

    private void validateStart(StartRequest request, String idempotencyKey) {
        if (request == null || request.packageCode() == null || !PACKAGE_CODE.matcher(request.packageCode()).matches())
            throw new IllegalArgumentException("Invalid packageCode");
        if (request.expectedBytes() < 1) throw new IllegalArgumentException("expectedBytes must be positive");
        if (request.expectedBytes() > properties.getMaxUploadBytes()) throw new ArtifactUploadTooLargeException();
        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches())
            throw new IllegalArgumentException("A valid Idempotency-Key header is required");
    }

    /** Length-prefixed fields, so no two different requests share a pre-hash byte stream. */
    private static String fingerprint(StartRequest request, long datasetVersionId) {
        ByteArrayOutputStream canonical = new ByteArrayOutputStream();
        try (DataOutputStream fields = new DataOutputStream(canonical)) {
            for (String field : List.of(request.packageCode(), request.contractCode(), Integer.toString(request.revision()),
                    request.datasetCode(), Long.toString(datasetVersionId), Long.toString(request.expectedBytes()))) {
                byte[] bytes = field.getBytes(StandardCharsets.UTF_8);
                fields.writeInt(bytes.length);
                fields.write(bytes);
            }
        } catch (IOException impossible) {
            throw new IllegalStateException("Canonical upload fingerprint encoding failed", impossible);
        }
        var digest = Sha256.newDigest();
        digest.update(canonical.toByteArray());
        return Sha256.hex(digest);
    }

    /** Opens its part only when the joined stream reaches it, so one part is open at a time. */
    private static final class LazyPartStream extends InputStream {
        private final java.util.function.Supplier<InputStream> opener;
        private InputStream delegate;

        private LazyPartStream(java.util.function.Supplier<InputStream> opener) {
            this.opener = opener;
        }

        private InputStream delegate() {
            if (delegate == null) delegate = opener.get();
            return delegate;
        }

        @Override public int read() throws IOException { return delegate().read(); }
        @Override public int read(byte[] buffer, int offset, int length) throws IOException { return delegate().read(buffer, offset, length); }
        @Override public void close() throws IOException { if (delegate != null) delegate.close(); }
    }
}
