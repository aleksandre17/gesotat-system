package org.base.api.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.base.api.service.storage.ObjectStorageService;
import org.base.api.service.storage.StoredArtifact;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Map;
import java.util.regex.Pattern;

/** Whitelist-only SQL source adapter. It exports an exact NDJSON source artifact before staging rows. */
@Service
public class PlatformSqlIngestionService {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}");
    private static final Pattern ENDPOINT = Pattern.compile("[A-Za-z0-9._-]+(?::[0-9]{1,5})?");
    private final JdbcTemplate controlPlane;
    private final JdbcTemplate dataPlane;
    private final ObjectMapper json;

    public PlatformSqlIngestionService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane,
                                       @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                       ObjectMapper json) {
        this.controlPlane = controlPlane;
        this.dataPlane = dataPlane;
        this.json = json;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public PlatformIngestReceipt ingest(long contractSourceId, long sourceConnectionId, ObjectStorageService storage) throws Exception {
        Map<String, Object> contract = controlPlane.query("SELECT cs.source_locator,cs.source_key_expression,cs.target_dataset_version_id,c.contract_id,d.product_id,c.source_system_id " +
                        "FROM platform.contract_source cs JOIN platform.ingestion_contract c ON c.contract_id=cs.contract_id JOIN platform.dataset d ON d.dataset_id=c.dataset_id " +
                        "WHERE cs.contract_source_id=? AND cs.active=1 AND (c.status='ACTIVE' OR c.raw_ingest_enabled=1)",
                rs -> rs.next() ? Map.of("locator", rs.getString(1),"key",rs.getString(2),"version",rs.getLong(3),"contract",rs.getLong(4),"product",rs.getLong(5),"sourceSystem",rs.getLong(6)) : null, contractSourceId);
        Map<String, Object> connection = controlPlane.query("SELECT source_system_id,connection_kind,endpoint,database_name,username,secret_reference FROM platform.source_connection WHERE source_connection_id=? AND enabled=1",
                rs -> rs.next() ? Map.of("sourceSystem",rs.getLong(1),"kind",rs.getString(2),"endpoint",rs.getString(3),"database",rs.getString(4),"username",rs.getString(5),"secret",rs.getString(6)) : null, sourceConnectionId);
        if (contract == null || connection == null) throw new IllegalArgumentException("Active contract source or source connection was not found");
        if (!contract.get("sourceSystem").equals(connection.get("sourceSystem"))) throw new IllegalArgumentException("Connection belongs to another source system");
        if (!"SQL_SERVER".equals(connection.get("kind"))) throw new IllegalArgumentException("Only SQL_SERVER source connections are supported");
        String password = System.getenv((String) connection.get("secret"));
        if (password == null || password.isBlank()) throw new IllegalStateException("Configured source secret is unavailable in runtime environment");
        File extract = File.createTempFile("platform-sql-extract-", ".ndjson");
        try {
            long rows = extract((String) connection.get("endpoint"), (String) connection.get("database"), (String) connection.get("username"), password, (String) contract.get("locator"), extract);
            StoredArtifact artifact;
            try (var input = Files.newInputStream(extract.toPath())) {
                artifact = storage.storeOriginal(extract.getName(), "application/x-ndjson", input, Files.size(extract.toPath()));
            }
            return stage(extract, contract, artifact, rows);
        } finally { Files.deleteIfExists(extract.toPath()); }
    }

    private long extract(String endpoint, String database, String username, String password, String locator, File output) throws Exception {
        if (!ENDPOINT.matcher(endpoint == null ? "" : endpoint).matches() || !IDENTIFIER.matcher(database == null ? "" : database).matches()) {
            throw new IllegalArgumentException("Invalid SQL source connection endpoint");
        }
        String query = "SELECT * FROM " + qualified(locator);
        String url = "jdbc:sqlserver://" + endpoint + ";databaseName=" + database + ";encrypt=true;trustServerCertificate=false";
        long count = 0;
        try (var source = DriverManager.getConnection(url, username, password); var statement = source.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            statement.setFetchSize(1000);
            try (ResultSet rows = statement.executeQuery(query); BufferedWriter writer = Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8)) {
                int columns = rows.getMetaData().getColumnCount();
                while (rows.next()) {
                    ObjectNode object = json.createObjectNode();
                    for (int i = 1; i <= columns; i++) object.set(rows.getMetaData().getColumnLabel(i), json.valueToTree(rows.getObject(i)));
                    writer.write(json.writeValueAsString(object)); writer.newLine(); count++;
                }
            }
        }
        return count;
    }

    private PlatformIngestReceipt stage(File ndjson, Map<String, Object> contract, StoredArtifact artifact, long rowCount) throws Exception {
        long batchId = insert("INSERT INTO ingest.batch(contract_id,product_id,status,checksum,started_at) VALUES(?,?,'STAGING',?,SYSUTCDATETIME())", contract.get("contract"), contract.get("product"), artifact.checksum());
        long artifactId = insert("INSERT INTO ingest.artifact(batch_id,original_name,format,object_uri,checksum,byte_size) VALUES(?,?,'SQL_NDJSON',?,?,?)", batchId, ndjson.getName(), artifact.objectUri(), artifact.checksum(), artifact.byteSize());
        long loadId = insert("INSERT INTO ingest.dataset_load(batch_id,dataset_version_id,source_name,source_row_count,status) VALUES(?,?,?,?, 'STAGING')", batchId, contract.get("version"), contract.get("locator"), rowCount);
        String sql = "INSERT INTO ingest.staged_row(dataset_load_id,source_row_number,source_key,raw_payload_json,payload_hash,validation_status) VALUES(?,?,?,?,?,'PENDING')";
        var dbConnection = DataSourceUtils.getConnection(dataPlane.getDataSource());
        try (var statement = dbConnection.prepareStatement(sql); var reader = Files.newBufferedReader(ndjson.toPath(), StandardCharsets.UTF_8)) {
            String line; long lineNo = 0; int pending = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++; JsonNode node = json.readTree(line); String key = field(node, (String) contract.get("key"));
                statement.setLong(1, loadId); statement.setLong(2, lineNo); statement.setString(3, key == null ? String.valueOf(lineNo) : key);
                statement.setString(4, line); statement.setString(5, sha256(line)); statement.addBatch();
                if (++pending == 500) { statement.executeBatch(); pending = 0; }
            }
            if (pending > 0) statement.executeBatch();
        } finally { DataSourceUtils.releaseConnection(dbConnection, dataPlane.getDataSource()); }
        return new PlatformIngestReceipt(batchId, artifactId, loadId, (int) Math.min(rowCount, Integer.MAX_VALUE), "STAGING");
    }

    private long insert(String sql, Object... values) {
        KeyHolder key = new GeneratedKeyHolder();
        dataPlane.update(connection -> { var statement = connection.prepareStatement(sql, new String[]{"id"}); for (int i=0;i<values.length;i++) statement.setObject(i+1, values[i]); return statement; }, key);
        if (key.getKey() == null) throw new IllegalStateException("Database did not return identity key"); return key.getKey().longValue();
    }

    private static String qualified(String locator) {
        String[] parts = locator.split("\\."); if (parts.length < 1 || parts.length > 2) throw new IllegalArgumentException("Invalid contract source locator");
        StringBuilder sql = new StringBuilder(); for (String part : parts) { if (!IDENTIFIER.matcher(part).matches()) throw new IllegalArgumentException("Unsafe contract source locator"); if (sql.length()>0) sql.append('.'); sql.append('[').append(part).append(']'); } return sql.toString();
    }
    private static String field(JsonNode node, String path) { if (path == null) return null; for (String part : path.replace("$.","").split("\\.")) node=node.path(part); return node.isMissingNode()||node.isNull()?null:node.asText(); }
    private static String sha256(String text) { try { byte[] digest=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)); StringBuilder result=new StringBuilder(64); for(byte b:digest) result.append(String.format("%02x",b)); return result.toString(); } catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);} }
}
