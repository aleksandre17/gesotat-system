package org.base.api.service.artifact.upload;

import org.base.api.service.artifact.ArtifactPackageContractResolver;
import org.base.api.service.artifact.ArtifactUploadIdentityResolver.Identity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Data Plane persistence of upload sessions, part checkpoints and tenant quota. Locks are taken by the caller's transaction. */
@Repository
public class UploadSessionRepository {
    private static final String SESSION_COLUMNS = "upload_session_id,tenant_key_hash,owner_key_hash,request_fingerprint,package_code,contract_code,contract_revision,dataset_code,expected_bytes,part_size,expected_parts,received_bytes,status,quota_released,artifact_manifest_id,package_checksum,expires_at";
    private static final String PART_COLUMNS = "part_number,sha256,byte_size,status";
    private static final String LOCK = " WITH (UPDLOCK,HOLDLOCK)";
    private static final String RECEIVED = "RECEIVED";

    private static final RowMapper<UploadSession> SESSION = (rs, n) -> {
        long manifest = rs.getLong("artifact_manifest_id");
        Long manifestId = rs.wasNull() ? null : manifest;
        return new UploadSession(rs.getObject("upload_session_id", UUID.class), rs.getString("tenant_key_hash"), rs.getString("owner_key_hash"),
                rs.getString("request_fingerprint"), rs.getString("package_code"), rs.getString("contract_code"), rs.getInt("contract_revision"),
                rs.getString("dataset_code"), rs.getLong("expected_bytes"), rs.getLong("part_size"), rs.getInt("expected_parts"),
                rs.getLong("received_bytes"), UploadSessionStatus.valueOf(rs.getString("status")), rs.getBoolean("quota_released"),
                manifestId, rs.getString("package_checksum"), rs.getTimestamp("expires_at").toInstant());
    };
    private static final RowMapper<UploadSession.Part> PART = (rs, n) ->
            new UploadSession.Part(rs.getInt("part_number"), rs.getString("sha256"), rs.getLong("byte_size"), RECEIVED.equals(rs.getString("status")));

    private final JdbcTemplate data;

    public UploadSessionRepository(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data) {
        this.data = data;
    }

    public Optional<UploadSession> lockByIdempotencyKey(Identity identity, String idempotencyKey) {
        return data.query("SELECT " + SESSION_COLUMNS + " FROM ingest.artifact_upload_session" + LOCK + " WHERE tenant_key_hash=? AND owner_key_hash=? AND idempotency_key=?",
                SESSION, identity.tenantKeyHash(), identity.ownerKeyHash(), idempotencyKey).stream().findFirst();
    }

    /** A session is visible only to the tenant and owner that created it. */
    public Optional<UploadSession> owned(UUID sessionId, Identity identity, boolean lock) {
        return data.query("SELECT " + SESSION_COLUMNS + " FROM ingest.artifact_upload_session" + (lock ? LOCK : "") + " WHERE upload_session_id=? AND tenant_key_hash=? AND owner_key_hash=?",
                SESSION, sessionId, identity.tenantKeyHash(), identity.ownerKeyHash()).stream().findFirst();
    }

    public Optional<UploadSession.Quota> lockQuota(String tenantKeyHash) {
        return data.query("SELECT reserved_bytes,active_sessions FROM ingest.artifact_upload_quota" + LOCK + " WHERE tenant_key_hash=?",
                (rs, n) -> new UploadSession.Quota(rs.getLong(1), rs.getInt(2)), tenantKeyHash).stream().findFirst();
    }

    public void reserveQuota(String tenantKeyHash, long bytes, boolean firstReservation) {
        if (firstReservation) data.update("INSERT ingest.artifact_upload_quota(tenant_key_hash,reserved_bytes,active_sessions) VALUES(?,?,1)", tenantKeyHash, bytes);
        else data.update("UPDATE ingest.artifact_upload_quota SET reserved_bytes=reserved_bytes+?,active_sessions=active_sessions+1,updated_at=SYSUTCDATETIME() WHERE tenant_key_hash=?", bytes, tenantKeyHash);
    }

    /** @return false when the reservation being released is not there, which means quota accounting is corrupt */
    public boolean releaseQuota(UploadSession session) {
        return data.update("UPDATE ingest.artifact_upload_quota SET reserved_bytes=reserved_bytes-?,active_sessions=active_sessions-1,updated_at=SYSUTCDATETIME() WHERE tenant_key_hash=? AND reserved_bytes>=? AND active_sessions>0",
                session.expectedBytes(), session.tenantKeyHash(), session.expectedBytes()) == 1;
    }

    public void insert(UUID sessionId, Identity identity, String idempotencyKey, String fingerprint, String packageCode,
                       ArtifactPackageContractResolver.DatasetContract contract, long expectedBytes, long partSize, int expectedParts, Instant expiresAt) {
        data.update("INSERT ingest.artifact_upload_session(upload_session_id,tenant_key_hash,owner_key_hash,idempotency_key,request_fingerprint,package_code,contract_code,contract_revision,dataset_code,dataset_version_id,expected_bytes,part_size,expected_parts,expires_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                sessionId, identity.tenantKeyHash(), identity.ownerKeyHash(), idempotencyKey, fingerprint, packageCode, contract.contractCode(),
                contract.revision(), contract.datasetCode(), contract.datasetVersionId(), expectedBytes, partSize, expectedParts, Timestamp.from(expiresAt));
    }

    public Optional<UploadSession.Part> part(UUID sessionId, int partNumber, boolean lock) {
        return data.query("SELECT " + PART_COLUMNS + " FROM ingest.artifact_upload_part" + (lock ? LOCK : "") + " WHERE upload_session_id=? AND part_number=?",
                PART, sessionId, partNumber).stream().findFirst();
    }

    public void reservePart(UUID sessionId, int partNumber, String sha256, long byteSize) {
        data.update("INSERT ingest.artifact_upload_part(upload_session_id,part_number,sha256,byte_size,status) VALUES(?,?,?,?,'RECEIVING')", sessionId, partNumber, sha256, byteSize);
    }

    public void discardReservedPart(UUID sessionId, int partNumber, String sha256) {
        data.update("DELETE FROM ingest.artifact_upload_part WHERE upload_session_id=? AND part_number=? AND status='RECEIVING' AND sha256=?", sessionId, partNumber, sha256);
    }

    public boolean markPartReceived(UUID sessionId, int partNumber, String sha256) {
        return data.update("UPDATE ingest.artifact_upload_part SET status='RECEIVED',received_at=SYSUTCDATETIME() WHERE upload_session_id=? AND part_number=? AND sha256=? AND status='RECEIVING'",
                sessionId, partNumber, sha256) == 1;
    }

    /** Recomputes received bytes from the checkpoints and reopens a retryable session. */
    public void recordProgress(UploadSession session) {
        Long received = data.queryForObject("SELECT COALESCE(SUM(byte_size),0) FROM ingest.artifact_upload_part WHERE upload_session_id=? AND status='RECEIVED'", Long.class, session.id());
        data.update("UPDATE ingest.artifact_upload_session SET received_bytes=?,status=CASE WHEN status='RETRYABLE' THEN 'OPEN' ELSE status END,last_error_code=NULL,updated_at=SYSUTCDATETIME() WHERE upload_session_id=? AND tenant_key_hash=? AND owner_key_hash=?",
                received == null ? 0 : received, session.id(), session.tenantKeyHash(), session.ownerKeyHash());
    }

    public List<UploadSession.Part> parts(UUID sessionId, boolean receivedOnly) {
        return data.query("SELECT " + PART_COLUMNS + " FROM ingest.artifact_upload_part WHERE upload_session_id=?" + (receivedOnly ? " AND status='RECEIVED'" : "") + " ORDER BY part_number",
                PART, sessionId);
    }

    public void deleteParts(UUID sessionId) {
        data.update("DELETE FROM ingest.artifact_upload_part WHERE upload_session_id=?", sessionId);
    }

    public void markProcessing(UUID sessionId) {
        data.update("UPDATE ingest.artifact_upload_session SET status='PROCESSING',last_error_code=NULL,updated_at=SYSUTCDATETIME() WHERE upload_session_id=? AND status IN('OPEN','RETRYABLE')", sessionId);
    }

    /** Optimistic transition from the state the caller read; false when the session moved meanwhile. */
    public boolean transition(UploadSession from, UploadSessionStatus to, String errorCode, Long manifestId, String packageChecksum) {
        return data.update("UPDATE ingest.artifact_upload_session SET status=?,last_error_code=?,artifact_manifest_id=COALESCE(?,artifact_manifest_id),package_checksum=COALESCE(?,package_checksum),updated_at=SYSUTCDATETIME(),quota_released=CASE WHEN ?=1 THEN 1 ELSE quota_released END WHERE upload_session_id=? AND status=? AND quota_released=?",
                to.name(), errorCode, manifestId, packageChecksum, to.releasesQuota(), from.id(), from.status().name(), from.quotaReleased()) == 1;
    }

    /** A completion whose worker died keeps its parts and becomes retryable. */
    public void requeueStaleProcessing(long leaseSeconds) {
        data.update("UPDATE ingest.artifact_upload_session SET status='RETRYABLE',last_error_code='PROCESSING_LEASE_EXPIRED',updated_at=SYSUTCDATETIME() WHERE status='PROCESSING' AND updated_at<DATEADD(SECOND,?,SYSUTCDATETIME()) AND quota_released=0",
                -leaseSeconds);
    }

    /** Sessions past their deadline, and finished sessions that still hold part checkpoints. */
    public List<UploadSession> cleanupCandidates(int limit) {
        return data.query("SELECT TOP (?) " + SESSION_COLUMNS + " FROM ingest.artifact_upload_session WHERE (status IN('OPEN','RETRYABLE') AND expires_at<=SYSUTCDATETIME()) OR (status IN('EXPIRED','CANCELLED','REJECTED','COMMITTED') AND EXISTS(SELECT 1 FROM ingest.artifact_upload_part p WHERE p.upload_session_id=ingest.artifact_upload_session.upload_session_id)) ORDER BY expires_at",
                SESSION, limit);
    }
}
