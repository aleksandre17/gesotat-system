package org.base.api.service.artifact;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Append-only access to {@code publication.release_gate_audit}. */
@Repository
public class ReleaseGateEvidenceRepository {
    private final JdbcTemplate dataPlane;

    public record Evidence(GateResult result, String evidenceJson) {}

    public ReleaseGateEvidenceRepository(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane) {
        this.dataPlane = dataPlane;
    }

    public void record(long datasetSnapshotId, String gateCode, GateResult result, String evidenceJson) {
        dataPlane.update("INSERT INTO publication.release_gate_audit(dataset_snapshot_id,gate_code,result,evidence_json) VALUES(?,?,?,?)",
                datasetSnapshotId, gateCode, result.name(), evidenceJson);
    }

    public Optional<Evidence> latest(long datasetSnapshotId, String gateCode) {
        return dataPlane.query("SELECT TOP 1 result,evidence_json FROM publication.release_gate_audit WHERE dataset_snapshot_id=? AND gate_code=? ORDER BY audit_id DESC",
                (rs, n) -> new Evidence(GateResult.valueOf(rs.getString(1)), rs.getString(2)), datasetSnapshotId, gateCode).stream().findFirst();
    }
}
