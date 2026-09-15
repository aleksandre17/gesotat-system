package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Copies the authoritative active classification registry into an immutable publication snapshot mirror. */
@Service
public class PlatformClassificationMirrorService {
    private final JdbcTemplate control;
    private final JdbcTemplate data;
    public PlatformClassificationMirrorService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data){this.control=control;this.data=data;}

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public void mirror(long publicationSnapshotId) {
        List<Map<String,Object>> items=control.queryForList("SELECT ci.classification_item_id,ci.classification_version_id,ci.code,ci.label_ka,ci.label_en,ci.status, " +
                "(SELECT TOP 1 parent_item_id FROM platform.classification_hierarchy h WHERE h.child_item_id=ci.classification_item_id AND (h.valid_to IS NULL OR h.valid_to>=CAST(SYSUTCDATETIME() AS date)) ORDER BY h.parent_item_id) AS parent_item_id " +
                "FROM platform.classification_item ci JOIN platform.classification_version cv ON cv.classification_version_id=ci.classification_version_id WHERE cv.status='APPROVED' AND ci.status='ACTIVE'");
        for(Map<String,Object> item:items) data.update("IF NOT EXISTS (SELECT 1 FROM reference.classification_item_snapshot WHERE snapshot_id=? AND classification_item_id=?) " +
                "INSERT INTO reference.classification_item_snapshot(snapshot_id,classification_item_id,scheme_version_id,code,label_ka,label_en,parent_item_id,status) VALUES(?,?,?,?,?,?,?,?)",
                publicationSnapshotId,((Number)item.get("classification_item_id")).longValue(),publicationSnapshotId,((Number)item.get("classification_item_id")).longValue(),((Number)item.get("classification_version_id")).longValue(),item.get("code"),item.get("label_ka"),item.get("label_en"),item.get("parent_item_id"),item.get("status"));
    }
}
