package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Data Plane access for snapshots, their entity rows and artifact attachment edges. SQL lives only here. */
@Repository
public class ArtifactAttachmentRepository {
    private final JdbcTemplate dataPlane;
    private final ObjectMapper json;

    public record SnapshotState(long datasetVersionId, String status) {}

    public record PublishedEntity(long entityId, long snapshotId, long datasetVersionId) {}

    /** One attachment joined to its content object, as served or reconciled. */
    public record AttachedObject(String externalKey, String relationCode, ArtifactRole role, String language, int ordinal, String originalName,
                                 String mediaType, long byteSize, String sha256, String bucket, String objectKey, VerificationStatus verification) {
        public String subject() {
            return externalKey + "/" + relationCode + "/" + language + "/" + ordinal;
        }
    }

    public record NewAttachment(long entityId, String relationCode, ArtifactRole role, String language, int ordinal, long artifactVersionId, long sourceRecordId) {}

    private static final String ATTACHED_OBJECT = "SELECT e.external_key,a.relation_code,a.artifact_role,a.language_tag,a.ordinal,v.original_name," +
            "o.media_type,o.byte_size,o.sha256,o.bucket,o.object_key,o.verification_status FROM entity.artifact_attachment a " +
            "JOIN entity.entity_record e ON e.entity_id=a.entity_id JOIN ingest.artifact_version v ON v.artifact_version_id=a.artifact_version_id " +
            "JOIN ingest.artifact_object o ON o.artifact_object_id=v.artifact_object_id ";

    public ArtifactAttachmentRepository(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane, ObjectMapper json) {
        this.dataPlane = dataPlane;
        this.json = json;
    }

    public Optional<SnapshotState> snapshot(long datasetSnapshotId) {
        return dataPlane.query("SELECT dataset_version_id,status FROM publication.dataset_snapshot WHERE dataset_snapshot_id=?",
                (rs, n) -> new SnapshotState(rs.getLong(1), rs.getString(2)), datasetSnapshotId).stream().findFirst();
    }

    public List<ArtifactMatcher.SourceRow> sourceRows(long datasetSnapshotId) {
        return dataPlane.query("SELECT entity_id,external_key,source_record_id,payload_json FROM entity.entity_record WHERE dataset_snapshot_id=? AND external_key IS NOT NULL",
                (rs, n) -> {
                    try {
                        return new ArtifactMatcher.SourceRow(rs.getLong(1), rs.getString(2), rs.getLong(3), json.readTree(rs.getString(4)));
                    } catch (java.io.IOException invalid) {
                        throw new IllegalStateException("Entity payload is not JSON: " + rs.getLong(1), invalid);
                    }
                }, datasetSnapshotId);
    }

    /** Existing edges of one relation keyed by {@link #slot}. */
    public Map<String, Long> slots(long datasetSnapshotId, String relationCode) {
        Map<String, Long> slots = new HashMap<>();
        dataPlane.query("SELECT entity_id,language_tag,ordinal,artifact_version_id FROM entity.artifact_attachment WHERE dataset_snapshot_id=? AND relation_code=?",
                (RowCallbackHandler) rs -> slots.put(slot(rs.getLong(1), rs.getString(2), rs.getInt(3)), rs.getLong(4)), datasetSnapshotId, relationCode);
        return slots;
    }

    public static String slot(long entityId, String language, int ordinal) {
        return entityId + "/" + language + "/" + ordinal;
    }

    public void insert(long datasetSnapshotId, NewAttachment edge) {
        dataPlane.update("INSERT INTO entity.artifact_attachment(dataset_snapshot_id,entity_id,relation_code,artifact_role,language_tag,ordinal,artifact_version_id,source_record_id) VALUES(?,?,?,?,?,?,?,?)",
                datasetSnapshotId, edge.entityId(), edge.relationCode(), edge.role().name(), edge.language(), edge.ordinal(), edge.artifactVersionId(), edge.sourceRecordId());
    }

    public int entityCount(long datasetSnapshotId) {
        Integer count = dataPlane.queryForObject("SELECT COUNT(*) FROM entity.entity_record WHERE dataset_snapshot_id=? AND external_key IS NOT NULL", Integer.class, datasetSnapshotId);
        return count == null ? 0 : count;
    }

    /** Attachment count per entity for one relation and language, including entities with none. */
    public Map<String, Integer> countsPerEntity(long datasetSnapshotId, String relationCode, String language) {
        Map<String, Integer> counts = new HashMap<>();
        dataPlane.query("SELECT e.external_key,(SELECT COUNT(*) FROM entity.artifact_attachment a WHERE a.entity_id=e.entity_id AND a.relation_code=? AND a.language_tag=?) " +
                        "FROM entity.entity_record e WHERE e.dataset_snapshot_id=? AND e.external_key IS NOT NULL",
                (RowCallbackHandler) rs -> counts.put(rs.getString(1), rs.getInt(2)), relationCode, language, datasetSnapshotId);
        return counts;
    }

    /** Every attachment of the snapshot in canonical order. */
    public List<AttachedObject> attachedObjects(long datasetSnapshotId) {
        return dataPlane.query(ATTACHED_OBJECT + "WHERE a.dataset_snapshot_id=? ORDER BY e.external_key,a.relation_code,a.language_tag,a.ordinal",
                (rs, n) -> attachedObject(rs), datasetSnapshotId);
    }

    public Optional<PublishedEntity> newestPublished(String recordType, String externalKey) {
        return dataPlane.query("SELECT TOP 1 e.entity_id,s.dataset_snapshot_id,s.dataset_version_id FROM entity.entity_record e " +
                        "JOIN publication.dataset_snapshot s ON s.dataset_snapshot_id=e.dataset_snapshot_id " +
                        "WHERE e.record_type=? AND e.external_key=? AND s.status='PUBLISHED' ORDER BY s.dataset_snapshot_id DESC",
                (rs, n) -> new PublishedEntity(rs.getLong(1), rs.getLong(2), rs.getLong(3)), recordType, externalKey).stream().findFirst();
    }

    public List<AttachedObject> attachedObjects(PublishedEntity entity) {
        return dataPlane.query(ATTACHED_OBJECT + "WHERE a.entity_id=? ORDER BY a.relation_code,a.language_tag,a.ordinal", (rs, n) -> attachedObject(rs), entity.entityId());
    }

    public Optional<AttachedObject> attachedObject(PublishedEntity entity, String relationCode, String language, int ordinal) {
        return dataPlane.query(ATTACHED_OBJECT + "WHERE a.entity_id=? AND a.relation_code=? AND a.language_tag=? AND a.ordinal=?",
                (rs, n) -> attachedObject(rs), entity.entityId(), relationCode, language, ordinal).stream().findFirst();
    }

    private static AttachedObject attachedObject(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AttachedObject(rs.getString(1), rs.getString(2), ArtifactRole.valueOf(rs.getString(3)), rs.getString(4), rs.getInt(5), rs.getString(6),
                rs.getString(7), rs.getLong(8), rs.getString(9), rs.getString(10), rs.getString(11), VerificationStatus.valueOf(rs.getString(12)));
    }
}
