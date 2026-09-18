package org.base.api.service.artifact;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Durable, tenant-scoped upload sessions with object-storage part checkpoints and quota reservations. */
@Service
public class ArtifactUploadSessionService {
    private static final String SESSION_COLUMNS = "upload_session_id,tenant_key_hash,owner_key_hash,idempotency_key,request_fingerprint,package_code,contract_code,contract_revision,dataset_code,dataset_version_id,expected_bytes,part_size,expected_parts,received_bytes,status,quota_released,last_error_code,artifact_manifest_id,package_checksum,expires_at";
    private final JdbcTemplate data;
    private final TransactionTemplate transaction;
    private final ArtifactObjectStore storage;
    private final ArtifactPackageContractResolver contracts;
    private final ArtifactPackageService packages;
    private final ArtifactProperties properties;

    public ArtifactUploadSessionService(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data,
                                        @Qualifier("dataPlaneTransactionManager") PlatformTransactionManager transactionManager,
                                        ObjectProvider<ArtifactObjectStore> storage,
                                        ArtifactPackageContractResolver contracts,
                                        ArtifactPackageService packages,
                                        ArtifactProperties properties) {
        this.data = data;
        this.transaction = new TransactionTemplate(transactionManager);
        this.storage = storage.getIfAvailable();
        this.contracts = contracts;
        this.packages = packages;
        this.properties = properties;
    }

    public SessionReceipt start(StartRequest request, ArtifactUploadIdentityResolver.Identity identity, String idempotencyKey) {
        validateStart(request, idempotencyKey);
        ArtifactPackageContractResolver.DatasetContract contract = contracts.resolve(request.contractCode(), request.revision(), request.datasetCode());
        String fingerprint = fingerprint(request, contract.datasetVersionId());
        int expectedParts = Math.toIntExact(1 + (request.expectedBytes() - 1) / properties.getUploadPartBytes());
        SessionReceipt created = transaction.execute(status -> {
            UploadSession prior = data.query("SELECT " + SESSION_COLUMNS + " FROM ingest.artifact_upload_session WITH (UPDLOCK,HOLDLOCK) WHERE tenant_key_hash=? AND owner_key_hash=? AND idempotency_key=?",
                    SESSION_MAPPER, identity.tenantKeyHash(), identity.ownerKeyHash(), idempotencyKey).stream().findFirst().orElse(null);
            if (prior != null) {
                if (!prior.requestFingerprint().equals(fingerprint)) throw new ArtifactUploadConflictException("Idempotency-Key was already used for a different upload request");
                return receipt(prior);
            }
            List<Quota> quota = data.query("SELECT reserved_bytes,active_sessions FROM ingest.artifact_upload_quota WITH (UPDLOCK,HOLDLOCK) WHERE tenant_key_hash=?",
                    (rs, n) -> new Quota(rs.getLong(1), rs.getInt(2)), identity.tenantKeyHash());
            Quota currentQuota = quota.stream().findFirst().orElse(new Quota(0, 0));
            if (request.expectedBytes() > properties.getMaxTenantReservedUploadBytes() - currentQuota.reservedBytes())
                throw new ArtifactUploadQuotaExceededException();
            if (currentQuota.activeSessions() >= properties.getMaxTenantActiveUploadSessions())
                throw new ArtifactUploadQuotaExceededException();
            if (quota.isEmpty()) data.update("INSERT ingest.artifact_upload_quota(tenant_key_hash,reserved_bytes,active_sessions) VALUES(?,?,1)",
                    identity.tenantKeyHash(), request.expectedBytes());
            else data.update("UPDATE ingest.artifact_upload_quota SET reserved_bytes=reserved_bytes+?,active_sessions=active_sessions+1,updated_at=SYSUTCDATETIME() WHERE tenant_key_hash=?",
                    request.expectedBytes(), identity.tenantKeyHash());
            UUID id = UUID.randomUUID();
            Instant expires = Instant.now().plusSeconds(properties.getUploadSessionTtlSeconds());
            data.update("INSERT ingest.artifact_upload_session(upload_session_id,tenant_key_hash,owner_key_hash,idempotency_key,request_fingerprint,package_code,contract_code,contract_revision,dataset_code,dataset_version_id,expected_bytes,part_size,expected_parts,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, identity.tenantKeyHash(), identity.ownerKeyHash(), idempotencyKey, fingerprint, request.packageCode(),
                    contract.contractCode(), contract.revision(), contract.datasetCode(), contract.datasetVersionId(), request.expectedBytes(),
                    properties.getUploadPartBytes(), expectedParts, java.sql.Timestamp.from(expires));
            return new SessionReceipt(id, properties.getUploadPartBytes(), expectedParts, 0, request.expectedBytes(), "OPEN", expires, null, null);
        });
        return created;
    }

    public PartReceipt uploadPart(UUID sessionId, int partNumber, long rangeStart, long rangeEnd, long rangeTotal,
                                  String sha256, InputStream content, long byteSize,
                                  ArtifactUploadIdentityResolver.Identity identity) {
        ArtifactKeys.requireSha256(sha256);
        if (storage == null) throw new ArtifactStorageException("Object Storage is not configured", null);
        long expectedSize;
        try {
            expectedSize = transaction.execute(status -> {
                UploadSession session = owned(sessionId, identity, true);
                if (!(session.status().equals("OPEN") || session.status().equals("RETRYABLE")))
                    throw new ArtifactUploadConflictException("Upload session is not accepting parts");
                if (Instant.now().isAfter(session.expiresAt())) throw new ArtifactUploadConflictException("Upload session has expired");
                if (partNumber < 1 || partNumber > session.expectedParts()) throw new IllegalArgumentException("Part number is outside the declared upload");
                long start = (long) (partNumber - 1) * session.partSize();
                long size = Math.min(session.partSize(), session.expectedBytes() - start);
                if (rangeTotal != session.expectedBytes() || rangeStart != start || rangeEnd != start + size - 1 || byteSize != size)
                    throw new IllegalArgumentException("Content-Range or part size does not match the upload session");
                Part existing = data.query("SELECT part_number,sha256,byte_size,status FROM ingest.artifact_upload_part WITH (UPDLOCK,HOLDLOCK) WHERE upload_session_id=? AND part_number=?",
                        PART_MAPPER, sessionId, partNumber).stream().findFirst().orElse(null);
                if (existing != null && (!existing.sha256().equals(sha256) || existing.byteSize() != byteSize))
                    throw new ArtifactUploadConflictException("Part number was already reserved for different bytes");
                if (existing == null) data.update("INSERT ingest.artifact_upload_part(upload_session_id,part_number,sha256,byte_size,status) VALUES(?,?,?,?,'RECEIVING')",
                        sessionId, partNumber, sha256, byteSize);
                return existing != null && existing.status().equals("RECEIVED") ? -size : size;
            });
        } catch (DuplicateKeyException race) {
            throw new ArtifactUploadConflictException("Concurrent upload part reservation conflict");
        }
        if (expectedSize < 0) return new PartReceipt(sessionId, partNumber, sha256, byteSize, true);
        try {
            storage.putStagedUploadPart(sessionId, partNumber, sha256, content, byteSize);
        } catch (IllegalArgumentException invalidDigest) {
            data.update("DELETE FROM ingest.artifact_upload_part WHERE upload_session_id=? AND part_number=? AND status='RECEIVING' AND sha256=?",
                    sessionId, partNumber, sha256);
            throw invalidDigest;
        }
        Integer checkpointed = transaction.execute(status -> {
            UploadSession session = owned(sessionId, identity, true);
            if (!(session.status().equals("OPEN") || session.status().equals("RETRYABLE"))) return 0;
            int updated = data.update("UPDATE ingest.artifact_upload_part SET status='RECEIVED',received_at=SYSUTCDATETIME() WHERE upload_session_id=? AND part_number=? AND sha256=? AND status='RECEIVING'",
                    sessionId, partNumber, sha256);
            if (updated != 1) {
                Part checkpoint = data.query("SELECT part_number,sha256,byte_size,status FROM ingest.artifact_upload_part WHERE upload_session_id=? AND part_number=?",
                        PART_MAPPER, sessionId, partNumber).stream().findFirst().orElse(null);
                return checkpoint != null && checkpoint.status().equals("RECEIVED") && checkpoint.sha256().equals(sha256)
                        && checkpoint.byteSize() == byteSize ? 2 : 0;
            }
            long received = data.queryForObject("SELECT COALESCE(SUM(byte_size),0) FROM ingest.artifact_upload_part WHERE upload_session_id=? AND status='RECEIVED'", Long.class, sessionId);
            data.update("UPDATE ingest.artifact_upload_session SET received_bytes=?,status=CASE WHEN status='RETRYABLE' THEN 'OPEN' ELSE status END,last_error_code=NULL,updated_at=SYSUTCDATETIME() WHERE upload_session_id=? AND tenant_key_hash=? AND owner_key_hash=?",
                    received, sessionId, identity.tenantKeyHash(), identity.ownerKeyHash());
            return 1;
        });
        if (checkpointed == null || checkpointed == 0) {
            storage.deleteStagedUploadPart(sessionId, partNumber, sha256);
            throw new ArtifactUploadConflictException("Upload session changed while the part was being stored");
        }
        return new PartReceipt(sessionId, partNumber, sha256, byteSize, checkpointed == 2);
    }

    public SessionReceipt complete(UUID sessionId, ArtifactUploadIdentityResolver.Identity identity) {
        UploadSession session = transaction.execute(status -> {
            UploadSession current = owned(sessionId, identity, true);
            if (current.status().equals("COMMITTED")) return current;
            if (current.status().equals("PROCESSING")) throw new ArtifactUploadConflictException("Upload completion is already running");
            if (!(current.status().equals("OPEN") || current.status().equals("RETRYABLE")))
                throw new ArtifactUploadConflictException("Upload session cannot be completed from its current state");
            if (Instant.now().isAfter(current.expiresAt())) throw new ArtifactUploadConflictException("Upload session has expired");
            Integer received = data.queryForObject("SELECT COUNT(*) FROM ingest.artifact_upload_part WHERE upload_session_id=? AND status='RECEIVED'", Integer.class, sessionId);
            if (received == null || received != current.expectedParts() || current.receivedBytes() != current.expectedBytes())
                throw new ArtifactUploadConflictException("Upload is incomplete; all parts must be checkpointed before completion");
            data.update("UPDATE ingest.artifact_upload_session SET status='PROCESSING',last_error_code=NULL,updated_at=SYSUTCDATETIME() WHERE upload_session_id=? AND status IN('OPEN','RETRYABLE')", sessionId);
            return current.withStatus("PROCESSING");
        });
        if (session.status().equals("COMMITTED")) return receipt(session);
        try {
            List<Part> parts = data.query("SELECT part_number,sha256,byte_size,status FROM ingest.artifact_upload_part WHERE upload_session_id=? AND status='RECEIVED' ORDER BY part_number", PART_MAPPER, sessionId);
            Enumeration<InputStream> streams = new Enumeration<>() {
                private int offset;
                public boolean hasMoreElements() { return offset < parts.size(); }
                public InputStream nextElement() {
                    Part part = parts.get(offset++);
                    return openVerifiedPart(sessionId, part);
                }
            };
            ArtifactPackageService.ManifestReceipt manifest;
            try (InputStream joined = new SequenceInputStream(streams)) {
                manifest = packages.uploadPackage(session.packageCode(), session.contractCode(), session.contractRevision(),
                        session.datasetCode(), joined);
            }
            transaction.executeWithoutResult(status -> finish(session, "COMMITTED", null, manifest.manifestId(), manifest.packageChecksum()));
            cleanupParts(sessionId, parts);
            return session.receipt("COMMITTED", manifest.manifestId(), manifest.packageChecksum());
        } catch (Exception failure) {
            boolean retryable = failure instanceof ArtifactStorageException || failure instanceof ArtifactScannerUnavailableException
                    || failure instanceof DataAccessException;
            String code = retryable ? "DEPENDENCY_UNAVAILABLE" : failure instanceof ArtifactMalwareDetectedException ? "CONTENT_REJECTED" : "PACKAGE_INVALID";
            transaction.executeWithoutResult(status -> finish(session, retryable ? "RETRYABLE" : "REJECTED", code, null, null));
            if (!retryable) cleanupParts(sessionId, data.query("SELECT part_number,sha256,byte_size,status FROM ingest.artifact_upload_part WHERE upload_session_id=?", PART_MAPPER, sessionId));
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new ArtifactStorageException("Resumable package could not be assembled", failure);
        }
    }

    public SessionReceipt status(UUID sessionId, ArtifactUploadIdentityResolver.Identity identity) {
        UploadSession session = owned(sessionId, identity, false);
        if ((session.status().equals("OPEN") || session.status().equals("RETRYABLE")) && Instant.now().isAfter(session.expiresAt())) {
            UploadSession expiredSession = session;
            transaction.executeWithoutResult(status -> finish(expiredSession, "EXPIRED", "SESSION_EXPIRED", null, null));
            session = session.withStatus("EXPIRED");
        }
        return receipt(session);
    }

    public void cancel(UUID sessionId, ArtifactUploadIdentityResolver.Identity identity) {
        UploadSession session = transaction.execute(status -> {
            UploadSession current = owned(sessionId, identity, true);
            if (current.status().equals("CANCELLED")) return current;
            if (!(current.status().equals("OPEN") || current.status().equals("RETRYABLE")))
                throw new ArtifactUploadConflictException("Only an open or retryable upload can be cancelled");
            finish(current, "CANCELLED", null, null, null);
            return current.withStatus("CANCELLED");
        });
        cleanupParts(sessionId, data.query("SELECT part_number,sha256,byte_size,status FROM ingest.artifact_upload_part WHERE upload_session_id=?", PART_MAPPER, sessionId));
    }

    @Scheduled(fixedDelayString = "${platform.artifacts.upload-cleanup-delay-ms:60000}")
    public void expireAndClean() {
        data.update("UPDATE ingest.artifact_upload_session SET status='RETRYABLE',last_error_code='PROCESSING_LEASE_EXPIRED',updated_at=SYSUTCDATETIME() WHERE status='PROCESSING' AND updated_at<DATEADD(SECOND,?,SYSUTCDATETIME()) AND quota_released=0",
                -properties.getUploadProcessingLeaseSeconds());
        List<UploadSession> expired = data.query("SELECT TOP (50) " + SESSION_COLUMNS + " FROM ingest.artifact_upload_session WHERE (status IN('OPEN','RETRYABLE') AND expires_at<=SYSUTCDATETIME()) OR (status IN('EXPIRED','CANCELLED','REJECTED','COMMITTED') AND EXISTS(SELECT 1 FROM ingest.artifact_upload_part p WHERE p.upload_session_id=ingest.artifact_upload_session.upload_session_id)) ORDER BY expires_at", SESSION_MAPPER);
        for (UploadSession candidate : expired) {
            if (candidate.status().equals("OPEN") || candidate.status().equals("RETRYABLE"))
                transaction.executeWithoutResult(status -> finish(candidate, "EXPIRED", "SESSION_EXPIRED", null, null));
            cleanupParts(candidate.id(), data.query("SELECT part_number,sha256,byte_size,status FROM ingest.artifact_upload_part WHERE upload_session_id=?", PART_MAPPER, candidate.id()));
        }
    }

    private void finish(UploadSession session, String state, String error, Long manifestId, String checksum) {
        boolean releaseQuota = state.equals("COMMITTED") || state.equals("REJECTED") || state.equals("CANCELLED") || state.equals("EXPIRED");
        int changed = data.update("UPDATE ingest.artifact_upload_session SET status=?,last_error_code=?,artifact_manifest_id=COALESCE(?,artifact_manifest_id),package_checksum=COALESCE(?,package_checksum),updated_at=SYSUTCDATETIME(),quota_released=CASE WHEN ?=1 THEN 1 ELSE quota_released END WHERE upload_session_id=? AND status=? AND quota_released=?",
                state, error, manifestId, checksum, releaseQuota, session.id(), session.status(), session.quotaReleased());
        if (changed == 0) return;
        if (releaseQuota && !session.quotaReleased()) {
            int released = data.update("UPDATE ingest.artifact_upload_quota SET reserved_bytes=reserved_bytes-?,active_sessions=active_sessions-1,updated_at=SYSUTCDATETIME() WHERE tenant_key_hash=? AND reserved_bytes>=? AND active_sessions>0",
                    session.expectedBytes(), session.tenantKeyHash(), session.expectedBytes());
            if (released != 1) throw new IllegalStateException("Upload quota reservation is inconsistent");
        }
    }

    private void cleanupParts(UUID sessionId, List<Part> parts) {
        if (storage == null) return;
        try {
            for (Part part : parts) storage.deleteStagedUploadPart(sessionId, part.partNumber(), part.sha256());
            data.update("DELETE FROM ingest.artifact_upload_part WHERE upload_session_id=?", sessionId);
        } catch (RuntimeException failure) {
            // The durable part rows remain as retry checkpoints for this scheduled cleanup.
        }
    }

    private InputStream openVerifiedPart(UUID sessionId, Part part) {
        return ArtifactUploadPartIntegrity.verifyOnRead(
                storage.openStagedUploadPart(sessionId, part.partNumber(), part.sha256()), part.sha256(), part.byteSize());
    }

    private UploadSession owned(UUID id, ArtifactUploadIdentityResolver.Identity identity, boolean lock) {
        String sql = "SELECT " + SESSION_COLUMNS + " FROM ingest.artifact_upload_session " + (lock ? "WITH (UPDLOCK,HOLDLOCK) " : "") +
                "WHERE upload_session_id=? AND tenant_key_hash=? AND owner_key_hash=?";
        return data.query(sql, SESSION_MAPPER, id, identity.tenantKeyHash(), identity.ownerKeyHash()).stream().findFirst()
                .orElseThrow(() -> new ArtifactNotFoundException("Upload session not found"));
    }

    private SessionReceipt receipt(UploadSession session) {
        return new SessionReceipt(session.id(), session.partSize(), session.expectedParts(), session.receivedBytes(),
                session.expectedBytes(), session.status(), session.expiresAt(), session.manifestId(), session.packageChecksum());
    }

    private void validateStart(StartRequest request, String key) {
        if (request == null || request.packageCode() == null || !request.packageCode().matches("[A-Za-z0-9][A-Za-z0-9_.:-]{0,159}"))
            throw new IllegalArgumentException("Invalid packageCode");
        if (request.expectedBytes() < 1) throw new IllegalArgumentException("expectedBytes must be positive");
        if (request.expectedBytes() > properties.getMaxUploadBytes()) throw new ArtifactUploadTooLargeException();
        if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9._:-]{15,127}"))
            throw new IllegalArgumentException("A valid Idempotency-Key header is required");
    }

    private static String fingerprint(StartRequest request, long datasetVersionId) {
        try {
            ByteArrayOutputStream canonical = new ByteArrayOutputStream();
            try (DataOutputStream fields = new DataOutputStream(canonical)) {
                writeField(fields, request.packageCode());
                writeField(fields, request.contractCode());
                writeField(fields, Integer.toString(request.revision()));
                writeField(fields, request.datasetCode());
                writeField(fields, Long.toString(datasetVersionId));
                writeField(fields, Long.toString(request.expectedBytes()));
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()));
        }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 is unavailable", impossible); }
        catch (IOException impossible) { throw new IllegalStateException("Canonical upload fingerprint encoding failed", impossible); }
    }

    private static void writeField(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    public record StartRequest(String packageCode, String contractCode, int revision, String datasetCode, long expectedBytes) {}
    public record SessionReceipt(UUID uploadSessionId, long partSize, int expectedParts, long receivedBytes, long expectedBytes,
                                 String status, Instant expiresAt, Long manifestId, String packageChecksum) {}
    public record PartReceipt(UUID uploadSessionId, int partNumber, String sha256, long byteSize, boolean replayed) {}
    private record Quota(long reservedBytes, int activeSessions) {}
    private record Part(int partNumber, String sha256, long byteSize, String status) {}
    private record UploadSession(UUID id, String tenantKeyHash, String ownerKeyHash, String idempotencyKey, String requestFingerprint,
                                 String packageCode, String contractCode, int contractRevision, String datasetCode, long expectedBytes,
                                 long partSize, int expectedParts, long receivedBytes, String status, boolean quotaReleased,
                                 String errorCode, Long manifestId, String packageChecksum, Instant expiresAt) {
        UploadSession withStatus(String value) { return new UploadSession(id, tenantKeyHash, ownerKeyHash, idempotencyKey, requestFingerprint,
                packageCode, contractCode, contractRevision, datasetCode, expectedBytes, partSize, expectedParts, receivedBytes, value,
                quotaReleased, errorCode, manifestId, packageChecksum, expiresAt); }
        SessionReceipt receipt(String value, Long manifest, String checksum) { return new SessionReceipt(id, partSize, expectedParts,
                receivedBytes, expectedBytes, value, expiresAt, manifest, checksum); }
    }

    private static final RowMapper<UploadSession> SESSION_MAPPER = (rs, n) -> mapSession(rs);
    private static final RowMapper<Part> PART_MAPPER = (rs, n) -> new Part(rs.getInt("part_number"), rs.getString("sha256"), rs.getLong("byte_size"), rs.getString("status"));
    private static UploadSession mapSession(ResultSet rs) throws SQLException {
        long manifest = rs.getLong("artifact_manifest_id"); Long manifestId = rs.wasNull() ? null : manifest;
        return new UploadSession(rs.getObject("upload_session_id", UUID.class), rs.getString("tenant_key_hash"), rs.getString("owner_key_hash"),
                rs.getString("idempotency_key"), rs.getString("request_fingerprint"), rs.getString("package_code"), rs.getString("contract_code"),
                rs.getInt("contract_revision"), rs.getString("dataset_code"), rs.getLong("expected_bytes"), rs.getLong("part_size"), rs.getInt("expected_parts"),
                rs.getLong("received_bytes"), rs.getString("status"), rs.getBoolean("quota_released"), rs.getString("last_error_code"),
                manifestId, rs.getString("package_checksum"), rs.getTimestamp("expires_at").toInstant());
    }
}
