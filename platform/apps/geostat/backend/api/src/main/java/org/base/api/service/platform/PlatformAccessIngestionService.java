package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import org.base.api.service.storage.StoredArtifact;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.base.api.service.platform.access.SemanticAccessMetadataAssertion;
import org.base.api.service.platform.access.SemanticAccessMetadataSchema;
import org.base.api.service.platform.access.SemanticAccessPackage;

/** Access ingestion with independently committed, idempotent staging chunks. */
@Service
public class PlatformAccessIngestionService {
    private static final int CHUNK_SIZE = 500;
    private static final String STAGE_SQL = "IF NOT EXISTS (SELECT 1 FROM ingest.staged_row WHERE dataset_load_id=? AND source_row_number=?) " +
            "INSERT INTO ingest.staged_row(dataset_load_id,source_row_number,source_key,raw_payload_json,payload_hash,validation_status) VALUES(?,?,?,?,?,'PENDING')";
    private final JdbcTemplate controlPlane;
    private final JdbcTemplate dataPlane;
    private final ObjectMapper json;
    private final TransactionTemplate tx;

    public PlatformAccessIngestionService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane,
                                          @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                          @Qualifier("dataPlaneTransactionManager") PlatformTransactionManager dataTransactionManager,
                                          ObjectMapper json) {
        this.controlPlane = controlPlane;
        this.dataPlane = dataPlane;
        this.json = json;
        this.tx = new TransactionTemplate(dataTransactionManager);
    }

    public PlatformIngestReceipt ingest(File accessFile, long contractSourceId, StoredArtifact artifact) throws Exception {
        Source source = source(contractSourceId);
        Load load = null;
        try (Database database = DatabaseBuilder.open(accessFile)) {
            Table table = requiredTable(database, source.tableName());
            load = tx.execute(status -> createLoad(source, accessFile, artifact));
            if (load == null) throw new IllegalStateException("Could not create governed dataset load");
            long rows = stage(table, source, load.loadId(), 0);
            complete(load.loadId(), rows);
            return new PlatformIngestReceipt(load.batchId(), load.artifactId(), load.loadId(), bounded(rows), "STAGING");
        } catch (Exception error) {
            if (load != null) {
                long failedLoadId = load.loadId();
                tx.executeWithoutResult(status -> dataPlane.update("UPDATE ingest.load_checkpoint SET status='FAILED',updated_at=SYSUTCDATETIME() WHERE dataset_load_id=?", failedLoadId));
            }
            throw error;
        }
    }

    /**
     * Stages every active source of one governed semantic package under one batch and one immutable artifact.
     * Each dataset retains its own checkpoint, so a failed dataset can resume without replaying completed siblings.
     */
    public PlatformPackageIngestReceipt ingestPackage(File accessFile, String contractCode, int contractRevision, StoredArtifact artifact) throws Exception {
        if (contractCode == null || contractCode.isBlank()) throw new IllegalArgumentException("contractCode is required");
        List<Source> sources = packageSources(contractCode, contractRevision);
        if (sources.isEmpty()) throw new IllegalArgumentException("No active Access sources exist for contract " + contractCode);
        BatchScope scope = tx.execute(status -> createOrReuseBatch(contractCode, artifact));
        if (scope == null) throw new IllegalStateException("Could not create governed package batch");
        List<PlatformIngestReceipt> receipts = new ArrayList<>();
        try (Database database = DatabaseBuilder.open(accessFile)) {
            for (Source source : sources) {
                Table table = requiredTable(database, source.tableName());
                Load load = tx.execute(status -> createOrReuseLoad(scope, source));
                if (load == null) throw new IllegalStateException("Could not create governed dataset load");
                long checkpoint = checkpoint(load.loadId());
                long rows = stage(table, source, load.loadId(), checkpoint);
                complete(load.loadId(), rows);
                receipts.add(new PlatformIngestReceipt(scope.batchId(), scope.artifactId(), load.loadId(), bounded(rows), "STAGING"));
            }
            tx.executeWithoutResult(status -> dataPlane.update("UPDATE ingest.batch SET status='STAGING',finished_at=SYSUTCDATETIME() WHERE batch_id=?", scope.batchId()));
            return new PlatformPackageIngestReceipt(scope.batchId(), scope.artifactId(), "STAGING", List.copyOf(receipts));
        } catch (Exception error) {
            tx.executeWithoutResult(status -> dataPlane.update("UPDATE ingest.batch SET status='FAILED',finished_at=SYSUTCDATETIME() WHERE batch_id=?", scope.batchId()));
            throw error;
        }
    }

    /** Materializes optional Access metadata into the governed Control Plane after package validation. */
    public void materializeMetadata(SemanticAccessPackage pack) {
        Long revisionId=controlPlane.query("SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=? AND revision=?",r->r.next()?r.getLong(1):null,pack.contractCode(),pack.contractRevision());
        if(revisionId==null) throw new IllegalArgumentException("Metadata package references unknown contract revision");
        for(SemanticAccessMetadataSchema s:pack.metadataSchemas()) {
            Long namespace=controlPlane.query("SELECT metadata_namespace_id FROM platform.metadata_namespace WHERE namespace_code=?",r->r.next()?r.getLong(1):null,s.namespaceCode());
            if(namespace==null) throw new IllegalArgumentException("Unknown metadata namespace: "+s.namespaceCode());
            controlPlane.update("MERGE platform.metadata_schema AS t USING (SELECT ? metadata_namespace_id,? schema_code,? revision,? schema_json,? lifecycle_status) s ON t.metadata_namespace_id=s.metadata_namespace_id AND t.schema_code=s.schema_code AND t.revision=s.revision WHEN MATCHED THEN UPDATE SET schema_json=s.schema_json,lifecycle_status=s.lifecycle_status WHEN NOT MATCHED THEN INSERT(metadata_namespace_id,schema_code,revision,schema_json,lifecycle_status) VALUES(s.metadata_namespace_id,s.schema_code,s.revision,s.schema_json,s.lifecycle_status);",namespace,s.schemaCode(),s.revision(),s.schemaJson(),state(s.approvalState()));
        }
        for(SemanticAccessMetadataAssertion a:pack.metadataAssertions()) {
            Long subject=controlPlane.query("SELECT TOP 1 metadata_subject_id FROM platform.metadata_subject WHERE subject_type=? AND subject_code=? AND subject_revision=?",r->r.next()?r.getLong(1):null,a.subjectType(),a.subjectCode(),a.subjectRevision());
            if(subject==null) { controlPlane.update("INSERT platform.metadata_subject(subject_type,subject_code,subject_revision,contract_revision_id,lifecycle_status,visibility) VALUES(?,?,?,?,'APPROVED','PUBLIC')",a.subjectType(),a.subjectCode(),a.subjectRevision(),revisionId); subject=controlPlane.query("SELECT TOP 1 metadata_subject_id FROM platform.metadata_subject WHERE subject_type=? AND subject_code=? AND subject_revision=?",r->r.next()?r.getLong(1):null,a.subjectType(),a.subjectCode(),a.subjectRevision()); }
            Long schema=controlPlane.query("SELECT TOP 1 s.metadata_schema_id FROM platform.metadata_schema s JOIN platform.metadata_namespace n ON n.metadata_namespace_id=s.metadata_namespace_id WHERE n.namespace_code=? ORDER BY s.revision DESC",r->r.next()?r.getLong(1):null,a.namespaceCode());
            controlPlane.update("MERGE platform.metadata_assertion AS t USING (SELECT ? metadata_subject_id,? metadata_schema_id,? namespace_code,? property_code,? language_tag,? value_type,? value_text,? value_json,? ordinal,? lifecycle_status,? source_reference) s ON t.metadata_subject_id=s.metadata_subject_id AND t.namespace_code=s.namespace_code AND t.property_code=s.property_code AND ISNULL(t.language_tag,'')=ISNULL(s.language_tag,'') AND t.ordinal=s.ordinal WHEN MATCHED THEN UPDATE SET value_type=s.value_type,value_text=s.value_text,value_json=s.value_json,lifecycle_status=s.lifecycle_status,source_reference=s.source_reference WHEN NOT MATCHED THEN INSERT(metadata_subject_id,metadata_schema_id,namespace_code,property_code,language_tag,value_type,value_text,value_json,ordinal,lifecycle_status,source_reference) VALUES(s.metadata_subject_id,s.metadata_schema_id,s.namespace_code,s.property_code,s.language_tag,s.value_type,s.value_text,s.value_json,s.ordinal,s.lifecycle_status,s.source_reference);",subject,schema,a.namespaceCode(),a.propertyCode(),a.languageTag(),a.valueType(),a.valueText(),a.valueJson(),a.ordinal(),state(a.lifecycleStatus()),a.sourceReference());
        }
    }
    private static String state(String s){return "READY".equalsIgnoreCase(s)||"APPROVED".equalsIgnoreCase(s)?"APPROVED":"DRAFT";}

    /** Resumes a failed package strictly from its immutable stored artifact and recorded batch identity. */
    public PlatformPackageIngestReceipt resumePackage(File accessFile, long batchId) throws Exception {
        Map<String,Object> batch = dataPlane.query("SELECT contract_id,checksum FROM ingest.batch WHERE batch_id=? AND status IN ('STAGING','FAILED')", rs -> rs.next() ? Map.of("contractId", rs.getLong(1), "checksum", rs.getString(2)) : null, batchId);
        if (batch == null) throw new IllegalArgumentException("No resumable package batch was found: " + batchId);
        String contractCode = controlPlane.query("SELECT contract_code FROM platform.ingestion_contract WHERE contract_id=?", rs -> rs.next() ? rs.getString(1) : null, batch.get("contractId"));
        if (contractCode == null) throw new IllegalStateException("The package batch no longer has a Control-Plane contract");
        StoredArtifact artifact = dataPlane.query("SELECT TOP 1 object_uri,checksum,byte_size FROM ingest.artifact WHERE batch_id=? ORDER BY artifact_id", rs -> rs.next() ? new StoredArtifact(rs.getString(1), rs.getString(2), rs.getLong(3)) : null, batchId);
        if (artifact == null) throw new IllegalStateException("The package batch has no immutable artifact");
        return ingestPackage(accessFile, contractCode, (int) batch.getOrDefault("contractRevision", 7), artifact);
    }

    public String packageArtifactUri(long batchId) {
        String uri = dataPlane.query("SELECT TOP 1 object_uri FROM ingest.artifact WHERE batch_id=? ORDER BY artifact_id", rs -> rs.next() ? rs.getString(1) : null, batchId);
        if (uri == null || uri.isBlank()) throw new IllegalArgumentException("No immutable artifact exists for package batch " + batchId);
        return uri;
    }

    /** Re-reads the immutable artifact and resumes after its durable load checkpoint. */
    public PlatformIngestReceipt resume(File accessFile, long datasetLoadId) throws Exception {
        Resume state = resumeState(datasetLoadId);
        try (Database database = DatabaseBuilder.open(accessFile)) {
            long rows = stage(requiredTable(database, state.source().tableName()), state.source(), datasetLoadId, state.checkpoint());
            complete(datasetLoadId, rows);
            return new PlatformIngestReceipt(state.batchId(), state.artifactId(), datasetLoadId, bounded(rows), "STAGING");
        } catch (Exception error) {
            tx.executeWithoutResult(status -> dataPlane.update("UPDATE ingest.load_checkpoint SET status='FAILED',updated_at=SYSUTCDATETIME() WHERE dataset_load_id=?", datasetLoadId));
            throw error;
        }
    }

    /** The resume controller uses only this recorded immutable artifact, never a newly supplied file. */
    public String artifactUri(long datasetLoadId) {
        String uri = dataPlane.query("SELECT a.object_uri FROM ingest.dataset_load l JOIN ingest.artifact a ON a.batch_id=l.batch_id WHERE l.dataset_load_id=?", rs -> rs.next() ? rs.getString(1) : null, datasetLoadId);
        if (uri == null || uri.isBlank()) throw new IllegalArgumentException("No immutable artifact exists for dataset load " + datasetLoadId);
        return uri;
    }

    private long stage(Table table, Source source, long loadId, long checkpoint) throws Exception {
        List<RowPayload> chunk = new ArrayList<>(CHUNK_SIZE);
        long rowNumber = 0;
        for (Row row : table) {
            rowNumber++;
            if (rowNumber <= checkpoint) continue;
            String payload = json.writeValueAsString(normalize(row));
            Object key = value(row, source.keyExpression());
            chunk.add(new RowPayload(rowNumber, key == null ? String.valueOf(rowNumber) : String.valueOf(key), payload, sha256(payload)));
            if (chunk.size() == CHUNK_SIZE) { stageChunk(loadId, chunk); chunk.clear(); }
        }
        if (!chunk.isEmpty()) stageChunk(loadId, chunk);
        return rowNumber;
    }

    private void stageChunk(long loadId, List<RowPayload> rows) {
        List<RowPayload> chunk = List.copyOf(rows);
        long checkpoint = chunk.get(chunk.size() - 1).rowNumber();
        tx.executeWithoutResult(status -> {
            dataPlane.batchUpdate(STAGE_SQL, new BatchPreparedStatementSetter() {
                @Override public void setValues(PreparedStatement ps, int i) throws SQLException {
                    RowPayload row = chunk.get(i);
                    ps.setLong(1, loadId); ps.setLong(2, row.rowNumber()); ps.setLong(3, loadId); ps.setLong(4, row.rowNumber());
                    ps.setString(5, row.sourceKey()); ps.setString(6, row.payload()); ps.setString(7, row.hash());
                }
                @Override public int getBatchSize() { return chunk.size(); }
            });
            dataPlane.update("UPDATE ingest.load_checkpoint SET last_source_row_number=?,status='RUNNING',updated_at=SYSUTCDATETIME() WHERE dataset_load_id=?", checkpoint, loadId);
        });
    }

    private void complete(long loadId, long rows) {
        tx.executeWithoutResult(status -> {
            dataPlane.update("UPDATE ingest.dataset_load SET source_row_count=? WHERE dataset_load_id=?", rows, loadId);
            dataPlane.update("UPDATE ingest.load_checkpoint SET status='COMPLETED',updated_at=SYSUTCDATETIME() WHERE dataset_load_id=?", loadId);
        });
    }

    private Load createLoad(Source source, File file, StoredArtifact artifact) {
        long batch = insert("INSERT INTO ingest.batch(contract_id,product_id,status,checksum,started_at) VALUES (?,?,'STAGING',?,SYSUTCDATETIME())", source.contractId(), source.productId(), artifact.checksum());
        long artifactId = insert("INSERT INTO ingest.artifact(batch_id,original_name,format,object_uri,checksum,byte_size) VALUES(?,?,'ACCESS',?,?,?)", batch, file.getName(), artifact.objectUri(), artifact.checksum(), artifact.byteSize());
        long load = insert("INSERT INTO ingest.dataset_load(batch_id,dataset_version_id,source_name,status) VALUES(?,?,?,'STAGING')", batch, source.datasetVersionId(), source.tableName());
        dataPlane.update("INSERT INTO ingest.load_checkpoint(dataset_load_id,last_source_row_number,status) VALUES(?,0,'RUNNING')", load);
        return new Load(batch, artifactId, load);
    }

    private BatchScope createOrReuseBatch(String contractCode, StoredArtifact artifact) {
        Map<String,Object> contract = controlPlane.queryForMap("SELECT c.contract_id,d.product_id FROM platform.ingestion_contract c JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE c.contract_code=? AND (c.status='ACTIVE' OR c.raw_ingest_enabled=1)", contractCode);
        long contractId = ((Number) contract.get("contract_id")).longValue();
        BatchScope existing = dataPlane.query("SELECT TOP 1 b.batch_id,a.artifact_id FROM ingest.batch b JOIN ingest.artifact a ON a.batch_id=b.batch_id WHERE b.contract_id=? AND b.checksum=? AND b.status IN ('STAGING','FAILED') ORDER BY b.batch_id DESC",
                rs -> rs.next() ? new BatchScope(rs.getLong(1), rs.getLong(2)) : null, contractId, artifact.checksum());
        if (existing != null) return existing;
        long batch = insert("INSERT INTO ingest.batch(contract_id,product_id,status,checksum,started_at) VALUES (?,?,'STAGING',?,SYSUTCDATETIME())", contractId, ((Number) contract.get("product_id")).longValue(), artifact.checksum());
        long artifactId = insert("INSERT INTO ingest.artifact(batch_id,original_name,format,object_uri,checksum,byte_size) VALUES(?,?,'ACCESS',?,?,?)", batch, "semantic-access-package.accdb", artifact.objectUri(), artifact.checksum(), artifact.byteSize());
        return new BatchScope(batch, artifactId);
    }

    private Load createOrReuseLoad(BatchScope scope, Source source) {
        Load existing = dataPlane.query("SELECT dataset_load_id FROM ingest.dataset_load WHERE batch_id=? AND dataset_version_id=? AND source_name=?", rs -> rs.next() ? new Load(scope.batchId(), scope.artifactId(), rs.getLong(1)) : null, scope.batchId(), source.datasetVersionId(), source.tableName());
        if (existing != null) return existing;
        long load = insert("INSERT INTO ingest.dataset_load(batch_id,dataset_version_id,source_name,status) VALUES(?,?,?,'STAGING')", scope.batchId(), source.datasetVersionId(), source.tableName());
        dataPlane.update("INSERT INTO ingest.load_checkpoint(dataset_load_id,last_source_row_number,status) VALUES(?,0,'RUNNING')", load);
        return new Load(scope.batchId(), scope.artifactId(), load);
    }

    private long checkpoint(long loadId) {
        Long value = dataPlane.query("SELECT last_source_row_number FROM ingest.load_checkpoint WHERE dataset_load_id=? AND status IN ('RUNNING','FAILED','COMPLETED')", rs -> rs.next() ? rs.getLong(1) : null, loadId);
        if (value == null) throw new IllegalStateException("Dataset load has no resumable checkpoint: " + loadId);
        return value;
    }

    private List<Source> packageSources(String contractCode, int revision) {
        List<Source> sources = controlPlane.query("SELECT rs.source_locator,rs.source_key_expression,rs.target_dataset_version_id,icr.contract_id,d.product_id FROM platform.contract_revision_source rs JOIN platform.ingestion_contract_revision icr ON icr.ingestion_contract_revision_id=rs.ingestion_contract_revision_id JOIN platform.ingestion_contract c ON c.contract_id=icr.contract_id JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE c.contract_code=? AND icr.revision=? ORDER BY rs.load_order,rs.contract_revision_source_id",
                (rs, row) -> new Source(tableName(rs.getString(1)), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)), contractCode, revision);
        if (!sources.isEmpty()) return sources;
        return controlPlane.query("SELECT CONCAT(N'ACCESS.',sc.access_table_name),sc.natural_key_expression,sc.dataset_version_id,c.contract_id,d.product_id FROM platform.site_contract_dataset sc JOIN platform.site_contract_revision scr ON scr.site_contract_revision_id=sc.site_contract_revision_id JOIN platform.ingestion_contract c ON c.contract_code=scr.contract_code JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE scr.contract_code=? AND scr.revision=? ORDER BY sc.load_order,sc.contract_dataset_id",
                (rs, row) -> new Source(tableName(rs.getString(1)), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)), contractCode, revision);
    }

    private Source source(long contractSourceId) {
        if (contractSourceId <= 0) throw new IllegalArgumentException("contractSourceId must be positive");
        Source source = controlPlane.query("SELECT cs.source_locator,cs.source_key_expression,cs.target_dataset_version_id,c.contract_id,d.product_id FROM platform.contract_source cs JOIN platform.ingestion_contract c ON c.contract_id=cs.contract_id JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE cs.contract_source_id=? AND cs.active=1 AND (c.status='ACTIVE' OR c.raw_ingest_enabled=1)", rs -> rs.next() ? new Source(tableName(rs.getString(1)), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)) : null, contractSourceId);
        if (source == null) throw new IllegalArgumentException("Active platform Access contract source not found");
        return source;
    }

    private Resume resumeState(long loadId) {
        LoadCheckpoint checkpoint = dataPlane.query("SELECT b.batch_id,a.artifact_id,cp.last_source_row_number,l.dataset_version_id FROM ingest.dataset_load l JOIN ingest.batch b ON b.batch_id=l.batch_id JOIN ingest.artifact a ON a.batch_id=b.batch_id JOIN ingest.load_checkpoint cp ON cp.dataset_load_id=l.dataset_load_id WHERE l.dataset_load_id=? AND cp.status IN ('RUNNING','FAILED')", rs -> rs.next() ? new LoadCheckpoint(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)) : null, loadId);
        if (checkpoint == null) throw new IllegalArgumentException("No resumable governed Access dataset load was found");
        Source source = controlPlane.query("SELECT cs.source_locator,cs.source_key_expression,cs.target_dataset_version_id,c.contract_id,d.product_id FROM platform.contract_source cs JOIN platform.ingestion_contract c ON c.contract_id=cs.contract_id JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE cs.target_dataset_version_id=? AND cs.active=1 AND (c.status='ACTIVE' OR c.raw_ingest_enabled=1)", rs -> rs.next() ? new Source(tableName(rs.getString(1)), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)) : null, checkpoint.datasetVersionId());
        if (source == null) throw new IllegalArgumentException("The load's governed contract source is no longer active");
        return new Resume(checkpoint.batchId(), checkpoint.artifactId(), checkpoint.rowNumber(), source);
    }

    private static Table requiredTable(Database database, String name) throws Exception { Table table = database.getTable(name); if (table == null) throw new IllegalArgumentException("Contracted Access table not found: " + name); return table; }
    private Map<String,Object> normalize(Row row) { Map<String,Object> output = new LinkedHashMap<>(); row.forEach((key,value) -> output.put(key, value instanceof byte[] bytes ? java.util.Base64.getEncoder().encodeToString(bytes) : value)); return output; }
    private static Object value(Row row, String name) { if (name == null || name.isBlank()) return null; for (Map.Entry<String,Object> entry : row.entrySet()) if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue(); return null; }
    private long insert(String sql, Object... args) { KeyHolder key = new GeneratedKeyHolder(); dataPlane.update(connection -> { var statement = connection.prepareStatement(sql, new String[]{"id"}); for (int i=0;i<args.length;i++) statement.setObject(i+1,args[i]); return statement; }, key); if (key.getKey()==null) throw new IllegalStateException("Database did not return identity key"); return key.getKey().longValue(); }
    private static String tableName(String locator) {
        int dot=locator.lastIndexOf('.'); String name=dot<0 ? locator : locator.substring(dot+1);
        if(name.startsWith("__")) return name;
        // The locator is an immutable contract value.  Never infer a physical
        // table from a site/family name here; migrations must register the
        // canonical Access table explicitly (including any namespace prefix).
        return name;
    }
    private static int bounded(long value) { return (int)Math.min(value,Integer.MAX_VALUE); }
    private static String sha256(String value) { try { byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); StringBuilder result=new StringBuilder(64); for(byte item:digest) result.append(String.format("%02x",item)); return result.toString(); } catch(Exception error) { throw new IllegalStateException("SHA-256 unavailable",error); } }
    private record Source(String tableName,String keyExpression,long datasetVersionId,long contractId,long productId) {}
    private record Load(long batchId,long artifactId,long loadId) {}
    private record Resume(long batchId,long artifactId,long checkpoint,Source source) {}
    private record LoadCheckpoint(long batchId,long artifactId,long rowNumber,long datasetVersionId) {}
    private record BatchScope(long batchId,long artifactId) {}
    private record RowPayload(long rowNumber,String sourceKey,String payload,String hash) {}
}
