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
        long manifestId = dataPlane.queryForObject("INSERT INTO ingest.artifact_manifest(manifest_schema,package_code,package_checksum,generator_version,entry_count,source_reference) " +
                        "OUTPUT INSERTED.artifact_manifest_id VALUES(?,?,?,?,?,?)", Long.class,
                manifest.schema(), manifest.packageCode(), manifest.packageChecksum(), manifest.generatorVersion(), manifest.entries().size(), manifest.sourceReference());
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
