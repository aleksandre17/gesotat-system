package org.base.api.service.publication.gate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Measures a snapshot in the Data Plane and resolves referenced vocabulary in the Control Plane. SQL lives only here. */
@Repository
public class SnapshotFactsRepository {
    /** Stays below SQL Server's 2,100-parameter limit. */
    private static final int IN_CHUNK = 1800;
    private final JdbcTemplate dataPlane;
    private final JdbcTemplate controlPlane;

    public SnapshotFactsRepository(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                   @Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        this.dataPlane = dataPlane;
        this.controlPlane = controlPlane;
    }

    public Optional<SnapshotFacts> load(long snapshotId) {
        return dataPlane.query("""
                SELECT s.dataset_snapshot_id, s.dataset_version_id, s.status, s.dataset_load_id, s.row_count, s.checksum,
                  (SELECT TOP 1 a.checksum FROM ingest.artifact a WHERE a.batch_id=l.batch_id ORDER BY a.artifact_id),
                  (SELECT COUNT(*) FROM publication.dataset_snapshot x WHERE x.dataset_load_id=s.dataset_load_id),
                  (SELECT COUNT(*) FROM ingest.staged_row r WHERE r.dataset_load_id=s.dataset_load_id),
                  (SELECT COUNT(*) FROM ingest.staged_row r WHERE r.dataset_load_id=s.dataset_load_id AND r.validation_status='VALID'),
                  (SELECT COUNT(*) FROM ingest.staged_row r WHERE r.dataset_load_id=s.dataset_load_id AND r.validation_status='REJECTED'),
                  (SELECT COUNT(*) FROM raw.source_record r WHERE r.dataset_snapshot_id=s.dataset_snapshot_id),
                  (SELECT COUNT(DISTINCT r.source_key) FROM raw.source_record r WHERE r.dataset_snapshot_id=s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM raw.source_record r WHERE r.dataset_snapshot_id=s.dataset_snapshot_id AND r.source_key IS NULL),
                  (SELECT COUNT(*) FROM raw.source_record r JOIN ingest.artifact a ON a.artifact_id=r.artifact_id
                     WHERE r.dataset_snapshot_id=s.dataset_snapshot_id AND a.batch_id<>l.batch_id),
                  (SELECT COUNT(*) FROM entity.entity_record e WHERE e.dataset_snapshot_id=s.dataset_snapshot_id),
                  (SELECT COUNT(DISTINCT e.external_key) FROM entity.entity_record e WHERE e.dataset_snapshot_id=s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM entity.entity_record e WHERE e.dataset_snapshot_id=s.dataset_snapshot_id AND e.external_key IS NULL),
                  (SELECT COUNT(*) FROM entity.entity_record e JOIN raw.source_record r ON r.source_record_id=e.source_record_id
                     WHERE e.dataset_snapshot_id=s.dataset_snapshot_id AND r.dataset_snapshot_id<>s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM entity.entity_link k JOIN raw.source_record r ON r.source_record_id=k.source_record_id
                     WHERE r.dataset_snapshot_id=s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM entity.entity_link k JOIN raw.source_record r ON r.source_record_id=k.source_record_id
                     JOIN entity.entity_record fe ON fe.entity_id=k.from_entity_id JOIN entity.entity_record te ON te.entity_id=k.to_entity_id
                     WHERE r.dataset_snapshot_id=s.dataset_snapshot_id AND (fe.is_current=0 OR te.is_current=0)),
                  (SELECT COUNT(*) FROM entity.entity_classification c JOIN raw.source_record r ON r.source_record_id=c.source_record_id
                     WHERE r.dataset_snapshot_id=s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM [statistics].observation o JOIN [statistics].series se ON se.series_id=o.series_id
                     WHERE se.dataset_snapshot_id=s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM [statistics].observation o JOIN [statistics].series se ON se.series_id=o.series_id
                     JOIN raw.source_record r ON r.source_record_id=o.source_record_id
                     WHERE se.dataset_snapshot_id=s.dataset_snapshot_id AND r.dataset_snapshot_id<>s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM [statistics].observation o JOIN raw.source_record r ON r.source_record_id=o.source_record_id
                     JOIN [statistics].series se ON se.series_id=o.series_id
                     WHERE r.dataset_snapshot_id=s.dataset_snapshot_id AND se.dataset_snapshot_id<>s.dataset_snapshot_id),
                  (SELECT COUNT(*) FROM [statistics].observation o JOIN [statistics].series se ON se.series_id=o.series_id
                     WHERE se.dataset_snapshot_id=s.dataset_snapshot_id AND o.numeric_value IS NULL AND o.text_value IS NULL AND o.boolean_value IS NULL)
                FROM publication.dataset_snapshot s JOIN ingest.dataset_load l ON l.dataset_load_id=s.dataset_load_id
                WHERE s.dataset_snapshot_id=?""",
                (rs, n) -> new SnapshotFacts(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4), rs.getLong(5), rs.getString(6),
                        rs.getString(7), rs.getLong(8), rs.getLong(9), rs.getLong(10), rs.getLong(11), rs.getLong(12), rs.getLong(13),
                        rs.getLong(14), rs.getLong(15), rs.getLong(16), rs.getLong(17), rs.getLong(18), rs.getLong(19), rs.getLong(20),
                        rs.getLong(21), rs.getLong(22), classificationItems(snapshotId), rs.getLong(23), rs.getLong(24), rs.getLong(25), rs.getLong(26)),
                snapshotId).stream().findFirst();
    }

    /** Transitions an evaluated snapshot into the review state publication accepts; false if its state moved. */
    public boolean markReviewRequired(long snapshotId) {
        return dataPlane.update("UPDATE publication.dataset_snapshot SET status='REVIEW_REQUIRED' WHERE dataset_snapshot_id=? AND status='SEMANTIC_REVIEW'", snapshotId) == 1;
    }

    public Set<Long> activeClassificationItems(Set<Long> candidates) {
        if (candidates.isEmpty()) return Set.of();
        List<Long> ids = List.copyOf(candidates);
        Set<Long> active = new HashSet<>();
        for (int i = 0; i < ids.size(); i += IN_CHUNK) {
            List<Long> chunk = ids.subList(i, Math.min(i + IN_CHUNK, ids.size()));
            String in = String.join(",", Collections.nCopies(chunk.size(), "?"));
            active.addAll(controlPlane.queryForList("SELECT classification_item_id FROM platform.classification_item WHERE status='ACTIVE' AND classification_item_id IN (" + in + ")",
                    Long.class, chunk.toArray()));
        }
        return active;
    }

    private Set<Long> classificationItems(long snapshotId) {
        return new HashSet<>(dataPlane.queryForList("SELECT DISTINCT c.classification_item_id FROM entity.entity_classification c " +
                "JOIN raw.source_record r ON r.source_record_id=c.source_record_id WHERE r.dataset_snapshot_id=?", Long.class, snapshotId));
    }
}
