package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Reads approved artifact relation definitions and policies from the Control Plane (single authority). */
@Component
public class ArtifactContractResolver {
    private static final String SELECT = "SELECT d.dataset_version_id,d.relation_code,d.artifact_role,d.min_per_row,d.max_per_row,d.ordered,d.match_rule_json," +
            "p.policy_code,p.revision,p.access_mode,p.required_authority,p.allowed_media_types_json,p.max_bytes,p.signed_url_ttl_seconds,p.retention_class " +
            "FROM platform.artifact_relation_definition d JOIN platform.artifact_policy p ON p.artifact_policy_id=d.artifact_policy_id " +
            "WHERE d.lifecycle_status='APPROVED' AND p.lifecycle_status='APPROVED' AND d.dataset_version_id=?";
    private static final String RECONCILIATION_SELECT = "SELECT d.dataset_version_id,d.relation_code,d.artifact_role,d.min_per_row,d.max_per_row,d.ordered,d.match_rule_json," +
            "p.policy_code,p.revision,p.access_mode,p.required_authority,p.allowed_media_types_json,p.max_bytes,p.signed_url_ttl_seconds,p.retention_class " +
            "FROM platform.artifact_relation_definition d JOIN platform.artifact_policy p ON p.artifact_policy_id=d.artifact_policy_id " +
            "WHERE d.lifecycle_status IN('APPROVED','RETIRED') AND p.lifecycle_status IN('APPROVED','RETIRED') AND d.dataset_version_id=?";
    private final JdbcTemplate controlPlane;
    private final ObjectMapper json;
    private final ArtifactMatchRules rules;

    public ArtifactContractResolver(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane, ObjectMapper json, ArtifactMatchRules rules) {
        this.controlPlane = controlPlane;
        this.json = json;
        this.rules = rules;
    }

    public List<ArtifactRelationDefinition> approved(long datasetVersionId) {
        return controlPlane.query(SELECT + " ORDER BY d.relation_code", (rs, n) -> map(rs), datasetVersionId);
    }

    /** Active and retired immutable definitions used to re-evaluate existing snapshots. */
    public List<ArtifactRelationDefinition> forReconciliation(long datasetVersionId) {
        return controlPlane.query(RECONCILIATION_SELECT + " ORDER BY d.relation_code", (rs, n) -> map(rs), datasetVersionId);
    }

    /** Dataset versions with active or retired immutable artifact relation evidence. */
    public List<Long> reconcilableDatasetVersions() {
        return controlPlane.query("SELECT DISTINCT d.dataset_version_id FROM platform.artifact_relation_definition d " +
                        "JOIN platform.artifact_policy p ON p.artifact_policy_id=d.artifact_policy_id " +
                        "WHERE d.lifecycle_status IN('APPROVED','RETIRED') AND p.lifecycle_status IN('APPROVED','RETIRED') ORDER BY d.dataset_version_id",
                (rs, n) -> rs.getLong(1));
    }

    public Optional<ArtifactRelationDefinition> approved(long datasetVersionId, String relationCode) {
        return controlPlane.query(SELECT + " AND d.relation_code=?", (rs, n) -> map(rs), datasetVersionId, relationCode).stream().findFirst();
    }

    /** Approved declarations in transportable form, for package tooling and previews. */
    public List<ArtifactRelationDescriptor> approvedDescriptors(long datasetVersionId) {
        return controlPlane.query(SELECT + " ORDER BY d.relation_code", (rs, n) -> descriptor(rs), datasetVersionId);
    }

    private ArtifactRelationDefinition map(ResultSet rs) throws SQLException {
        return descriptor(rs).toDefinition(rules);
    }

    private ArtifactRelationDescriptor descriptor(ResultSet rs) throws SQLException {
        try {
            Set<String> media = new HashSet<>();
            for (JsonNode node : json.readTree(rs.getString("allowed_media_types_json"))) media.add(node.asText());
            var policy = new ArtifactRelationDescriptor.Policy(rs.getString("policy_code"), rs.getInt("revision"), rs.getString("access_mode"),
                    rs.getString("required_authority"), media, rs.getLong("max_bytes"), rs.getInt("signed_url_ttl_seconds"), rs.getString("retention_class"));
            int max = rs.getInt("max_per_row");
            Integer maxPerRow = rs.wasNull() ? null : max;
            return new ArtifactRelationDescriptor(rs.getLong("dataset_version_id"), rs.getString("relation_code"), rs.getString("artifact_role"),
                    rs.getInt("min_per_row"), maxPerRow, rs.getBoolean("ordered"), json.readTree(rs.getString("match_rule_json")), policy);
        } catch (java.io.IOException invalid) {
            throw new IllegalStateException("Approved artifact definition contains invalid JSON", invalid);
        }
    }
}
