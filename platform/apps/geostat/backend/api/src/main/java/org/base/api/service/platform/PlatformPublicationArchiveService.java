package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Archives every dataset member of a superseded publication snapshot. */
@Service
public class PlatformPublicationArchiveService {
    private final JdbcTemplate data;
    private final PlatformArchiveService archive;
    public PlatformPublicationArchiveService(@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data, PlatformArchiveService archive) { this.data=data; this.archive=archive; }
    public void archivePublication(long productId,long publicationSnapshotId) {
        List<Map<String,Object>> members=data.queryForList("SELECT dataset_version_id,dataset_snapshot_id,checksum FROM publication.snapshot_member WHERE snapshot_id=?",publicationSnapshotId);
        if(members.isEmpty()) throw new IllegalStateException("Publication snapshot has no dataset members");
        for(Map<String,Object> member:members) archive.archive(new ArchiveSnapshotRequest(productId,publicationSnapshotId,((Number)member.get("dataset_version_id")).longValue(),((Number)member.get("dataset_snapshot_id")).longValue(),(String)member.get("checksum")));
    }
}
