package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.base.api.service.storage.ObjectStorageService;
import org.springframework.beans.factory.ObjectProvider;

/** Technical validation is deterministic and non-destructive; semantic validation is a later contract step. */
@Service
public class PlatformValidationService {
    private final JdbcTemplate controlPlane;
    private final JdbcTemplate dataPlane;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ObjectStorageService> storage;

    public PlatformValidationService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane,
                                     @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane, ObjectMapper objectMapper,
                                     ObjectProvider<ObjectStorageService> storage) {
        this.controlPlane = controlPlane;
        this.dataPlane = dataPlane;
        this.objectMapper = objectMapper;
        this.storage = storage;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public ValidationReceipt validate(long datasetLoadId) {
        if (datasetLoadId <= 0) throw new IllegalArgumentException("datasetLoadId must be positive");
        List<Map<String, Object>> rows = dataPlane.queryForList("SELECT staged_row_id,raw_payload_json FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='PENDING' ORDER BY staged_row_id", datasetLoadId);
        long accepted = 0;
        long rejected = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("staged_row_id")).longValue();
            try {
                objectMapper.readTree((String) row.get("raw_payload_json"));
                dataPlane.update("UPDATE ingest.staged_row SET validation_status='VALID',error_json=NULL WHERE staged_row_id=?", id);
                accepted++;
            } catch (Exception invalidJson) {
                dataPlane.update("UPDATE ingest.staged_row SET validation_status='REJECTED',error_json=? WHERE staged_row_id=?",
                        "{\"code\":\"INVALID_JSON\"}", id);
                issue(id, "INVALID_JSON", "ERROR", "Payload is not valid JSON", "{\"code\":\"INVALID_JSON\"}");
                rejected++;
            }
        }
        List<Long> duplicateRows = dataPlane.query("SELECT staged_row_id FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID' AND source_key IS NOT NULL AND source_key IN " +
                        "(SELECT source_key FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID' AND source_key IS NOT NULL GROUP BY source_key HAVING COUNT(*)>1)",
                (rs, row) -> rs.getLong(1), datasetLoadId, datasetLoadId);
        for (Long rowId : duplicateRows) {
            dataPlane.update("UPDATE ingest.staged_row SET validation_status='REJECTED',error_json=? WHERE staged_row_id=?",
                    "{\"code\":\"DUPLICATE_SOURCE_KEY\"}", rowId);
            issue(rowId, "DUPLICATE_SOURCE_KEY", "ERROR", "source_key must be unique within a dataset load", "{\"code\":\"DUPLICATE_SOURCE_KEY\"}");
        }
        applyDeclaredRules(datasetLoadId);
        Long valid = dataPlane.queryForObject("SELECT COUNT(*) FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID'", Long.class, datasetLoadId);
        Long invalid = dataPlane.queryForObject("SELECT COUNT(*) FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='REJECTED'", Long.class, datasetLoadId);
        // Empty optional datasets (for example an empty classifier hierarchy)
        // are valid; only actual rejected rows make a load rejected.
        String status = invalid != null && invalid > 0 ? "REJECTED" : "VALIDATED";
        dataPlane.update("UPDATE ingest.dataset_load SET accepted_count=?,rejected_count=?,status=? WHERE dataset_load_id=?", valid, invalid, status, datasetLoadId);
        updateBatchStatus(datasetLoadId);
        if (invalid != null && invalid > 0) quarantineRejectedLoadArtifacts(datasetLoadId);
        return new ValidationReceipt(datasetLoadId, valid == null ? 0 : valid, invalid == null ? 0 : invalid, status);
    }

    /** A multi-table package is reviewable only when every member load is technically validated. */
    private void updateBatchStatus(long datasetLoadId) {
        Long batchId=dataPlane.query("SELECT batch_id FROM ingest.dataset_load WHERE dataset_load_id=?",rs->rs.next()?rs.getLong(1):null,datasetLoadId);
        if(batchId==null) throw new IllegalArgumentException("Dataset load not found");
        Map<String,Object> aggregate=dataPlane.query("SELECT COUNT(*) total, SUM(CASE WHEN status='VALIDATED' THEN 1 ELSE 0 END) validated, SUM(CASE WHEN status='REJECTED' THEN 1 ELSE 0 END) rejected FROM ingest.dataset_load WHERE batch_id=?",
                rs->rs.next()?Map.of("total",rs.getLong(1),"validated",rs.getLong(2),"rejected",rs.getLong(3)):null,batchId);
        if(aggregate==null) throw new IllegalStateException("Package batch has no dataset loads");
        long total=((Number)aggregate.get("total")).longValue(), validated=((Number)aggregate.get("validated")).longValue(), rejected=((Number)aggregate.get("rejected")).longValue();
        String batchStatus=rejected>0?"REJECTED":validated==total?"REVIEW_REQUIRED":"STAGING";
        dataPlane.update("UPDATE ingest.batch SET status=? WHERE batch_id=?",batchStatus,batchId);
    }

    /**
     * Invalid source material stays immutable in ingest, while a separately addressed copy is
     * placed in quarantine for restricted review.  A storage failure aborts validation rather
     * than allowing a rejected file to silently escape the documented custody path.
     */
    private void quarantineRejectedLoadArtifacts(long datasetLoadId) {
        ObjectStorageService objectStorage = storage.getIfAvailable();
        if (objectStorage == null) return; // Local/unit-test mode has no S3 plane.
        try {
            objectStorage.provision();
            List<Map<String, Object>> artifacts = dataPlane.queryForList(
                    "SELECT a.artifact_id,a.object_uri FROM ingest.artifact a " +
                            "JOIN ingest.dataset_load l ON l.batch_id=a.batch_id " +
                            "WHERE l.dataset_load_id=? AND a.quarantine_uri IS NULL", datasetLoadId);
            for (Map<String, Object> artifact : artifacts) {
                String quarantineUri = objectStorage.quarantine((String) artifact.get("object_uri"));
                dataPlane.update("UPDATE ingest.artifact SET quarantine_uri=? WHERE artifact_id=?", quarantineUri,
                        ((Number) artifact.get("artifact_id")).longValue());
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Could not quarantine rejected source artifact", failure);
        }
    }

    private void issue(long stagedRowId, String code, String severity, String message, String details) {
        dataPlane.update("IF NOT EXISTS (SELECT 1 FROM ingest.validation_issue WHERE staged_row_id=? AND rule_code=?) " +
                        "INSERT INTO ingest.validation_issue(staged_row_id,rule_code,severity,message,details_json) VALUES(?,?,?,?,?)",
                stagedRowId, code, stagedRowId, code, severity, message, details);
    }

    private void applyDeclaredRules(long datasetLoadId) {
        Long version = dataPlane.query("SELECT dataset_version_id FROM ingest.dataset_load WHERE dataset_load_id=?", rs -> rs.next() ? rs.getLong(1) : null, datasetLoadId);
        if (version == null) throw new IllegalArgumentException("Dataset load not found");
        List<Map<String,Object>> rules = controlPlane.queryForList("SELECT rule_code,rule_type,severity,expression_json FROM platform.validation_rule WHERE dataset_version_id=? AND active=1", version);
        if (rules.isEmpty()) return;
        List<Map<String,Object>> rows = dataPlane.queryForList("SELECT staged_row_id,raw_payload_json FROM ingest.staged_row WHERE dataset_load_id=? AND validation_status='VALID'", datasetLoadId);
        for (Map<String,Object> row : rows) {
            long rowId=((Number)row.get("staged_row_id")).longValue();
            try {
                JsonNode payload=objectMapper.readTree((String)row.get("raw_payload_json"));
                for(Map<String,Object> rule:rules) {
                    JsonNode expression=objectMapper.readTree((String)rule.get("expression_json"));
                    String failure=evaluate(payload,(String)rule.get("rule_type"),expression);
                    if(failure==null)continue;
                    String code=(String)rule.get("rule_code"), severity=(String)rule.get("severity");
                    issue(rowId,code,severity,failure,expression.toString());
                    if("ERROR".equalsIgnoreCase(severity)) dataPlane.update("UPDATE ingest.staged_row SET validation_status='REJECTED',error_json=? WHERE staged_row_id=?", "{\"code\":\""+code.replace("\"","")+"\"}",rowId);
                }
            } catch(Exception failure) { throw new IllegalStateException("Declared validation rule execution failed for staged row " + rowId, failure); }
        }
    }

    private static String evaluate(JsonNode payload,String type,JsonNode expression) {
        String path=expression.path("path").asText();
        JsonNode value=at(payload,path);
        return switch(type) {
            case "REQUIRED_PATH" -> value==null||value.isMissingNode()||value.isNull()||value.asText().isBlank() ? "Required path is absent: "+path : null;
            case "REGEX_PATH" -> {
                if(value==null||value.isMissingNode()||value.isNull()) yield null;
                String regex=expression.path("pattern").asText(); if(regex.isBlank()) throw new IllegalArgumentException("REGEX_PATH requires pattern");
                yield Pattern.matches(regex,value.asText())?null:"Value does not match pattern at: "+path;
            }
            case "NUMERIC_RANGE" -> {
                if(value==null||value.isMissingNode()||value.isNull()) yield null;
                BigDecimal number=new BigDecimal(value.asText());
                if(expression.has("min")&&number.compareTo(new BigDecimal(expression.path("min").asText()))<0) yield "Value is below minimum at: "+path;
                if(expression.has("max")&&number.compareTo(new BigDecimal(expression.path("max").asText()))>0) yield "Value exceeds maximum at: "+path;
                yield null;
            }
            default -> throw new IllegalArgumentException("Unsupported validation rule type: "+type);
        };
    }

    private static JsonNode at(JsonNode root,String path) {
        if(path==null||path.isBlank()) throw new IllegalArgumentException("Validation expression requires path");
        JsonNode cursor=root; for(String part:path.replace("$.","").split("\\.")) cursor=cursor.path(part); return cursor;
    }
}
