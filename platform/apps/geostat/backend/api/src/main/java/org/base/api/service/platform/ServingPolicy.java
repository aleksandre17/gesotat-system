package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Who may read which contract table, and where its rows come from when they are not in the Data Plane.
 *
 * A contract table outside the Data Plane is served only when the approved contract says it is servable, and
 * only to a caller holding the authority that row demands (migration 109, AIR-2026-045). Nothing here knows a
 * site, a family or a table name: the decision and the authority come from the contract row.
 *
 * Rows of a servable non-DATA table are read from canonical raw storage — the immutable record of what the
 * source actually contained — and only from a PUBLISHED snapshot, so serving can never reveal data that has
 * not passed the publication gates.
 */
@Service
public class ServingPolicy {

    /** What the contract says about one table. */
    public record TableServing(String logicalTableCode, String storagePlane, boolean servable, String requiredAuthority) {
        public boolean dataPlane() { return "DATA".equalsIgnoreCase(storagePlane); }
    }

    public static final class ServingForbidden extends RuntimeException {
        public ServingForbidden(String message) { super(message); }
    }

    private final JdbcTemplate control;
    private final JdbcTemplate data;
    private final ObjectMapper mapper;

    public ServingPolicy(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,
                         @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data, ObjectMapper mapper) {
        this.control = control;
        this.data = data;
        this.mapper = mapper;
    }

    public Optional<TableServing> serving(long revisionId, String logicalTableCode) {
        List<TableServing> rows = control.query("""
                SELECT TOP 1 t.logical_table_code, t.storage_plane, t.servable, t.serving_authority
                FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id = t.structure_id
                WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE')
                  AND t.logical_table_code = ? ORDER BY t.revision DESC""",
                (rs, n) -> new TableServing(rs.getString(1), rs.getString(2), rs.getBoolean(3), rs.getString(4)), logicalTableCode);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** True when this caller may read the table at all. A table the contract does not declare is never served. */
    public boolean permits(long revisionId, String logicalTableCode) {
        return serving(revisionId, logicalTableCode).map(this::permits).orElse(false);
    }

    public boolean permits(TableServing table) {
        if (table.dataPlane()) return true;
        if (!table.servable()) return false;
        return table.requiredAuthority() == null || holds(table.requiredAuthority());
    }

    /** Refuses with the reason the caller is allowed to know: the table is not served, or not served to them. */
    public TableServing require(long revisionId, String logicalTableCode) {
        TableServing table = serving(revisionId, logicalTableCode)
                .orElseThrow(() -> new IllegalArgumentException("Table is not declared by the approved contract: " + logicalTableCode));
        if (table.dataPlane() || permits(table)) return table;
        throw new ServingForbidden(table.servable()
                ? "Reading " + logicalTableCode + " requires the " + table.requiredAuthority() + " authority"
                : "Contract table is not served: " + logicalTableCode);
    }

    private static boolean holds(String authority) {
        Authentication caller = SecurityContextHolder.getContext().getAuthentication();
        if (caller == null || !caller.isAuthenticated()) return false;
        return caller.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    /**
     * Rows of a servable non-DATA table, projected from the immutable raw record of the newest PUBLISHED
     * snapshot of that table's dataset. An unpublished table serves nothing rather than draft content.
     */
    public List<Map<String, Object>> rawRows(String logicalTableCode, List<String> fields, int limit) {
        // The dataset registry lives in the Control Plane, the rows in the Data Plane: resolve, then read.
        List<Long> versions = control.query("""
                SELECT v.dataset_version_id FROM platform.dataset_version v
                JOIN platform.dataset d ON d.dataset_id = v.dataset_id WHERE d.dataset_code = ?""",
                (rs, n) -> rs.getLong(1), logicalTableCode);
        if (versions.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(versions.size(), "?"));
        List<Object> parameters = new java.util.ArrayList<>();
        parameters.add(Math.min(Math.max(1, limit), 1000));
        parameters.addAll(versions);
        String sql = "SELECT TOP (?) r.payload_json FROM raw.source_record r WHERE r.dataset_snapshot_id = ("
                + "SELECT TOP 1 s.dataset_snapshot_id FROM publication.dataset_snapshot s WHERE s.status = 'PUBLISHED'"
                + " AND s.dataset_version_id IN (" + placeholders + ") ORDER BY s.dataset_snapshot_id DESC) ORDER BY r.source_record_id";
        List<String> payloads = data.query(sql, (rs, n) -> rs.getString(1), parameters.toArray());
        return project(payloads, fields);
    }

    List<Map<String, Object>> project(List<String> payloads, List<String> fields) {
        List<Map<String, Object>> out = new java.util.ArrayList<>(payloads.size());
        for (String payload : payloads) {
            Map<String, Object> row = new LinkedHashMap<>();
            Map<?, ?> parsed;
            try { parsed = mapper.readValue(payload, Map.class); } catch (Exception notJson) { continue; }
            for (String field : fields) row.put(field, parsed.get(field)); // declared fields only; nothing undeclared is served
            out.add(row);
        }
        return out;
    }
}
