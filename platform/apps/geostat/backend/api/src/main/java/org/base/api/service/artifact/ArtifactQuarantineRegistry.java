package org.base.api.service.artifact;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Append-only Data Plane evidence for deterministic quarantine copies. */
@Repository
public class ArtifactQuarantineRegistry {
    private final JdbcTemplate dataPlane;

    public record QuarantineReceipt(long quarantineId, boolean created) { }

    public ArtifactQuarantineRegistry(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane) {
        this.dataPlane = dataPlane;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public QuarantineReceipt record(String sha256, long byteSize, ArtifactObjectStore.ObjectLocation location,
                                    String sourceType, String sourceReferenceHash, String scannerCode, String signature) {
        Long existing = dataPlane.query("SELECT artifact_quarantine_id FROM ingest.artifact_quarantine WITH (UPDLOCK,HOLDLOCK,INDEX(uq_artifact_quarantine_source)) "
                        + "WHERE sha256=? AND source_reference_hash=?", rs -> rs.next() ? rs.getLong(1) : null,
                sha256, sourceReferenceHash);
        if (existing != null) return new QuarantineReceipt(existing, false);
        Long id = dataPlane.queryForObject("INSERT INTO ingest.artifact_quarantine(sha256,byte_size,object_bucket,object_key,source_type,source_reference_hash,scanner_code,scanner_signature) "
                        + "OUTPUT INSERTED.artifact_quarantine_id VALUES(?,?,?,?,?,?,?,?)", Long.class,
                sha256, byteSize, location.bucket(), location.key(), sourceType, sourceReferenceHash, scannerCode, signature);
        return new QuarantineReceipt(id, true);
    }
}
