package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
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

    public Optional<ArtifactRelationDefinition> approved(long datasetVersionId, String relationCode) {
        return controlPlane.query(SELECT + " AND d.relation_code=?", (rs, n) -> map(rs), datasetVersionId, relationCode).stream().findFirst();
    }

    private ArtifactRelationDefinition map(ResultSet rs) throws SQLException {
        try {
            Set<String> media = new HashSet<>();
            for (JsonNode node : json.readTree(rs.getString("allowed_media_types_json"))) media.add(node.asText());
            ArtifactPolicy policy = new ArtifactPolicy(rs.getString("policy_code"), rs.getInt("revision"),
                    ArtifactPolicy.AccessMode.valueOf(rs.getString("access_mode")), rs.getString("required_authority"), media,
                    rs.getLong("max_bytes"), Duration.ofSeconds(rs.getInt("signed_url_ttl_seconds")), RetentionClass.valueOf(rs.getString("retention_class")));
            int max = rs.getInt("max_per_row");
            Integer maxPerRow = rs.wasNull() ? null : max;
            return new ArtifactRelationDefinition(rs.getLong("dataset_version_id"), rs.getString("relation_code"), ArtifactRole.valueOf(rs.getString("artifact_role")), policy,
                    rs.getInt("min_per_row"), maxPerRow, rs.getBoolean("ordered"), rules.parse(json.readTree(rs.getString("match_rule_json"))));
        } catch (java.io.IOException invalid) {
            throw new IllegalStateException("Approved artifact definition contains invalid JSON", invalid);
        }
    }
}
