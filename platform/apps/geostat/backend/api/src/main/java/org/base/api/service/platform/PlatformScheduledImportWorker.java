package org.base.api.service.platform;

import org.base.api.service.storage.ObjectStorageService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Monthly controlled source polling; it never executes drafts or duplicates a successful monthly batch. */
@Service
public class PlatformScheduledImportWorker {
    private final JdbcTemplate control;
    private final JdbcTemplate data;
    private final PlatformSqlIngestionService sql;
    private final ObjectProvider<ObjectStorageService> storage;
    private final PlatformJobLeaseService lease;
    public PlatformScheduledImportWorker(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data,PlatformSqlIngestionService sql,ObjectProvider<ObjectStorageService> storage,PlatformJobLeaseService lease){this.control=control;this.data=data;this.sql=sql;this.storage=storage;this.lease=lease;}

    @Scheduled(cron = "${platform.import.schedule.cron:0 0 3 1 * *}")
    public void runMonthly() {
        if(!lease.acquire("monthly-platform-import",180))return;
        try {
        ObjectStorageService configured=storage.getIfAvailable();
        if(configured==null)return;
        List<Map<String,Object>> jobs=control.queryForList("SELECT cs.contract_source_id,sc.source_connection_id,c.contract_id FROM platform.ingestion_contract c " +
                "JOIN platform.contract_source cs ON cs.contract_id=c.contract_id AND cs.active=1 " +
                "JOIN platform.source_connection sc ON sc.source_system_id=c.source_system_id AND sc.enabled=1 " +
                "WHERE c.status='ACTIVE' AND c.ingestion_method='SCHEDULED' AND c.auto_publish=0");
        for(Map<String,Object> job:jobs) {
            long contractId=((Number)job.get("contract_id")).longValue();
            Integer recent=data.queryForObject("SELECT COUNT(*) FROM ingest.batch WHERE contract_id=? AND status IN ('REVIEW_REQUIRED','VALIDATED','PREPARED','PUBLISHED') AND started_at>=DATEADD(MONTH,-1,SYSUTCDATETIME())",Integer.class,contractId);
            if(recent!=null && recent>0)continue;
            try { sql.ingest(((Number)job.get("contract_source_id")).longValue(),((Number)job.get("source_connection_id")).longValue(),configured); }
            catch(Exception failure) { control.update("INSERT INTO platform.outbox_event(aggregate_type,aggregate_id,event_type,payload_json,status,last_error) VALUES('INGESTION_CONTRACT',?,'SCHEDULED_IMPORT_FAILED','{}','FAILED',?)",contractId,truncate(failure.getMessage())); }
        }
        } finally { lease.release("monthly-platform-import"); }
    }
    private static String truncate(String value){if(value==null)return "unknown failure";return value.length()>2000?value.substring(0,2000):value;}
}
