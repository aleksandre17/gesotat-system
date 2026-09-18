package org.base.api.service.artifact;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Data Plane persistence of manifests, content objects and artifact versions. Append-only and idempotent. */
@Repository
public class ArtifactRegistry {
    private final JdbcTemplate dataPlane;

    /** A manifest entry as stored, with its surrogate identities and verification state. */
    public record StoredEntry(long artifactVersionId, long artifactObjectId, ArtifactManifest.Entry entry, VerificationStatus verificationStatus) {}

    public record Registration(long manifestId, boolean created) {}

    public record AuditCandidate(long artifactObjectId, String sha256, long byteSize, String bucket, String objectKey) {}

    public List<AuditCandidate> auditCandidates(int limit, int recheckMinutes, int issueRetryMinutes) {
        return dataPlane.query("SELECT TOP (?) artifact_object_id,sha256,byte_size,bucket,object_key FROM ingest.artifact_object " +
                        "WHERE last_audit_attempt_at IS NULL OR " +
                        "(verification_status='VERIFIED' AND last_audit_attempt_at<=DATEADD(MINUTE,-?,SYSUTCDATETIME())) OR " +
                        "(verification_status<>'VERIFIED' AND last_audit_attempt_at<=DATEADD(MINUTE,-?,SYSUTCDATETIME())) " +
                        "ORDER BY last_audit_attempt_at,artifact_object_id",
                (rs, n) -> new AuditCandidate(rs.getLong("artifact_object_id"), rs.getString("sha256"), rs.getLong("byte_size"),
                        rs.getString("bucket"), rs.getString("object_key")), limit, recheckMinutes, issueRetryMinutes);
    }

    public long startAuditRun() {
        return dataPlane.queryForObject("INSERT INTO ingest.artifact_object_audit_run OUTPUT INSERTED.audit_run_id DEFAULT VALUES", Long.class);
    }

    /** Persists the current object state, immutable issue evidence and run counters atomically. */
    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public void recordAuditResult(long runId, AuditCandidate candidate, VerificationStatus status, Long observedBytes, String observedSha256) {
        if (status == VerificationStatus.REGISTERED) throw new IllegalArgumentException("REGISTERED is not an audit result");
        String counter = switch (status) {
            case VERIFIED -> "objects_verified";
            case MISSING -> "objects_missing";
            case CHECKSUM_MISMATCH -> "objects_mismatched";
            case REGISTERED -> throw new IllegalArgumentException("REGISTERED is not an audit result");
        };
        int counted = dataPlane.update("UPDATE ingest.artifact_object_audit_run SET objects_examined=objects_examined+1," + counter + "=" + counter + "+1 WHERE audit_run_id=? AND status='RUNNING'", runId);
        if (counted != 1) throw new IllegalStateException("Artifact audit run is no longer active");
        int updated = dataPlane.update("UPDATE ingest.artifact_object SET verification_status=?,verified_at=SYSUTCDATETIME(),last_audit_attempt_at=SYSUTCDATETIME() WHERE artifact_object_id=?",
                status.name(), candidate.artifactObjectId());
        if (updated != 1) throw new IllegalStateException("Artifact object disappeared during integrity audit");
        if (status == VerificationStatus.VERIFIED) {
            dataPlane.update("UPDATE ingest.artifact_object_audit_issue SET resolved_at=SYSUTCDATETIME(),audit_run_id=? WHERE artifact_object_id=? AND resolved_at IS NULL",
                    runId, candidate.artifactObjectId());
        } else {
            String issue = status == VerificationStatus.MISSING ? "MISSING" : "CHECKSUM_MISMATCH";
            int updatedIssue = dataPlane.update("UPDATE ingest.artifact_object_audit_issue SET audit_run_id=?,issue_code=?,expected_sha256=?,observed_sha256=?,expected_byte_size=?,observed_byte_size=?,last_detected_at=SYSUTCDATETIME(),occurrence_count=occurrence_count+1 " +
                            "WHERE artifact_object_id=? AND resolved_at IS NULL",
                    runId, issue, candidate.sha256(), observedSha256, candidate.byteSize(), observedBytes, candidate.artifactObjectId());
            if (updatedIssue == 0) dataPlane.update("INSERT INTO ingest.artifact_object_audit_issue(audit_run_id,artifact_object_id,issue_code,expected_sha256,observed_sha256,expected_byte_size,observed_byte_size) VALUES(?,?,?,?,?,?,?)",
                    runId, candidate.artifactObjectId(), issue, candidate.sha256(), observedSha256, candidate.byteSize(), observedBytes);
        }
    }

    public void completeAuditRun(long runId, String status, String errorCode) {
        dataPlane.update("UPDATE ingest.artifact_object_audit_run SET status=?,last_error_code=?,completed_at=SYSUTCDATETIME() WHERE audit_run_id=? AND status='RUNNING'",
                status, errorCode, runId);
    }

    public void abandonStaleAuditRuns(int staleMinutes) {
        dataPlane.update("UPDATE ingest.artifact_object_audit_run SET status='ABANDONED',last_error_code='WORKER_LEASE_EXPIRED',completed_at=SYSUTCDATETIME() " +
                "WHERE status='RUNNING' AND started_at<DATEADD(MINUTE,-?,SYSUTCDATETIME())", staleMinutes);
    }

    public ArtifactRegistry(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane) {
        this.dataPlane = dataPlane;
    }

    /**
     * Registers a manifest once; the same package checksum always returns the same manifest.
     * The lookup uses a serializable key-range lock on the unique checksum index so concurrent
     * retries converge on one row instead of surfacing a uniqueness race as a failed request.
     */
    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public Registration register(ArtifactManifest manifest) {
        Long existing = manifestId(manifest.packageChecksum());
        if (existing != null) return new Registration(existing, false);
        long manifestId = dataPlane.queryForObject("INSERT INTO ingest.artifact_manifest(manifest_schema,package_code,package_checksum,generator_version,entry_count,source_reference,contract_code,contract_revision,dataset_version_id) " +
                        "OUTPUT INSERTED.artifact_manifest_id VALUES(?,?,?,?,?,?,?,?,?)", Long.class,
                manifest.schema(), manifest.packageCode(), manifest.packageChecksum(), manifest.generatorVersion(), manifest.entries().size(), manifest.sourceReference(),
                manifest.contractCode(), manifest.contractRevision(), manifest.datasetVersionId());
        for (ArtifactManifest.Entry entry : manifest.entries()) {
            long objectId = objectId(entry);
            dataPlane.update("INSERT INTO ingest.artifact_version(artifact_manifest_id,original_path,original_name,artifact_object_id) VALUES(?,?,?,?)",
                    manifestId, entry.originalPath(), entry.originalName(), objectId);
        }
        return new Registration(manifestId, true);
    }

    public Long manifestId(String packageChecksum) {
        return dataPlane.query("SELECT artifact_manifest_id FROM ingest.artifact_manifest WITH (UPDLOCK,HOLDLOCK,INDEX(uq_artifact_manifest_checksum)) WHERE package_checksum=?",
                rs -> rs.next() ? rs.getLong(1) : null, packageChecksum);
    }

    public boolean manifestExists(long manifestId) {
        Integer count = dataPlane.queryForObject("SELECT COUNT(*) FROM ingest.artifact_manifest WHERE artifact_manifest_id=?", Integer.class, manifestId);
        return count != null && count > 0;
    }

    public List<StoredEntry> entries(long manifestId) {
        return dataPlane.query("SELECT v.artifact_version_id,o.artifact_object_id,v.original_path,o.sha256,o.byte_size,o.media_type,o.bucket,o.object_key,o.verification_status " +
                        "FROM ingest.artifact_version v JOIN ingest.artifact_object o ON o.artifact_object_id=v.artifact_object_id " +
                        "WHERE v.artifact_manifest_id=? ORDER BY v.original_path",
                (rs, n) -> new StoredEntry(rs.getLong(1), rs.getLong(2),
                        new ArtifactManifest.Entry(rs.getString(3), rs.getString(4), rs.getLong(5), rs.getString(6), rs.getString(7), rs.getString(8)),
                        VerificationStatus.valueOf(rs.getString(9))), manifestId);
    }

    public void markVerification(long artifactObjectId, VerificationStatus status) {
        dataPlane.update("UPDATE ingest.artifact_object SET verification_status=?, verified_at=SYSUTCDATETIME() WHERE artifact_object_id=?", status.name(), artifactObjectId);
    }

    /** Content identity is the checksum; a second location for the same bytes is not a new object. */
    private long objectId(ArtifactManifest.Entry entry) {
        List<long[]> found = dataPlane.query("SELECT artifact_object_id,byte_size FROM ingest.artifact_object WITH (UPDLOCK,HOLDLOCK) WHERE sha256=?",
                (rs, n) -> new long[]{rs.getLong(1), rs.getLong(2)}, entry.sha256());
        if (!found.isEmpty()) {
            if (found.get(0)[1] != entry.byteSize())
                throw new IllegalStateException("Byte size conflict for sha256 " + entry.sha256() + ": registered " + found.get(0)[1] + ", manifest " + entry.byteSize());
            return found.get(0)[0];
        }
        return dataPlane.queryForObject("INSERT INTO ingest.artifact_object(sha256,byte_size,media_type,bucket,object_key) OUTPUT INSERTED.artifact_object_id VALUES(?,?,?,?,?)",
                Long.class, entry.sha256(), entry.byteSize(), entry.mediaType(), entry.bucket(), entry.objectKey());
    }
}
