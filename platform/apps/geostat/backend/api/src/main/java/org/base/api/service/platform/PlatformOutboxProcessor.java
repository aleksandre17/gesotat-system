package org.base.api.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Polling transactional-outbox dispatcher. A Data-plane write is idempotent by release id. */
@Service
@ConditionalOnProperty(name = "platform.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class PlatformOutboxProcessor {
    private final JdbcTemplate control;
    private final JdbcTemplate data;
    private final DataPlanePublicationWriter writer;
    private final PublicationControlStore controlStore;
    private final PlatformPublicationArchiveService publicationArchive;
    private final ObjectMapper json;
    private final PlatformSchemaReadiness schemaReadiness;

    public PlatformOutboxProcessor(@Qualifier("primaryJdbcTemplate") JdbcTemplate control, @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data,
                                  DataPlanePublicationWriter writer, PublicationControlStore controlStore, PlatformPublicationArchiveService publicationArchive, ObjectMapper json,
                                  PlatformSchemaReadiness schemaReadiness) {
        this.control=control; this.data=data; this.writer=writer; this.controlStore=controlStore; this.publicationArchive=publicationArchive; this.json=json; this.schemaReadiness=schemaReadiness;
    }

    @Scheduled(fixedDelayString = "${platform.outbox.fixed-delay-ms:30000}")
    public void dispatch() {
        if (!schemaReadiness.isReady()) return;
        List<Map<String,Object>> events=control.queryForList("SELECT TOP 20 event_id,aggregate_id,event_type,payload_json,attempts FROM platform.outbox_event WHERE status='PENDING' AND event_type IN ('PUBLISH_SNAPSHOT','ARCHIVE_PUBLICATION') AND available_at<=SYSUTCDATETIME() ORDER BY event_id");
        for (Map<String,Object> event:events) process(event);
    }

    private void process(Map<String,Object> event) {
        long id=((Number)event.get("event_id")).longValue();
        int claimed=control.update("UPDATE platform.outbox_event SET status='PROCESSING' WHERE event_id=? AND status='PENDING'",id);
        if(claimed==0)return;
        try {
            JsonNode payload=json.readTree((String)event.get("payload_json"));
            if("PUBLISH_SNAPSHOT".equals(event.get("event_type"))) {
                PublishSnapshotRequest request=new PublishSnapshotRequest(payload.path("productId").asLong(),payload.path("datasetVersionId").asLong(),payload.path("datasetSnapshotId").asLong(),payload.path("checksum").asText());
                Long rows=data.query("SELECT row_count FROM publication.dataset_snapshot WHERE dataset_snapshot_id=? AND dataset_version_id=?",rs->rs.next()?rs.getLong(1):null,request.datasetSnapshotId(),request.datasetVersionId());
                if(rows==null)throw new IllegalStateException("Prepared dataset snapshot is absent");
                writer.publish(request,((Number)event.get("aggregate_id")).longValue(),rows);
                controlStore.complete(((Number)event.get("aggregate_id")).longValue());
            } else {
                publicationArchive.archivePublication(payload.path("productId").asLong(),payload.path("publicationSnapshotId").asLong());
                controlStore.markProcessed(id);
            }
        } catch(Exception error) {
            int attempts=((Number)event.get("attempts")).intValue()+1;
            String message=error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();
            control.update("UPDATE platform.outbox_event SET status=?,attempts=?,available_at=DATEADD(MINUTE,5,SYSUTCDATETIME()),last_error=? WHERE event_id=?",
                    attempts>=10?"FAILED":"PENDING",attempts,message.length()>2000?message.substring(0,2000):message,id);
        }
    }
}
