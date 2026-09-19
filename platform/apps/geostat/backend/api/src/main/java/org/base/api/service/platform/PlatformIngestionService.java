package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.base.api.security.tenancy.TenantAccessGuard;

/**
 * The first write boundary of the canonical platform. It only creates a
 * traceable batch, artifact and immutable staging rows. It never publishes,
 * invents a schema, or writes to a legacy child database.
 */
@Service
public class PlatformIngestionService {
    private final JdbcTemplate controlPlane;
    private final JdbcTemplate dataPlane;
    private final TenantAccessGuard tenants;

    public PlatformIngestionService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane,
                                    @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                    TenantAccessGuard tenants) {
        this.controlPlane = controlPlane;
        this.dataPlane = dataPlane;
        this.tenants = tenants;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public PlatformIngestReceipt stage(PlatformIngestRequest request) {
        validate(request);
        // The body names the product; this is the tenancy enforcement point of the staging route.
        tenants.requireProductId(request.productId());
        Integer approved = controlPlane.queryForObject("SELECT COUNT(*) FROM platform.ingestion_contract c " +
                "JOIN platform.dataset d ON d.dataset_id=c.dataset_id JOIN platform.dataset_version v ON v.dataset_id=d.dataset_id " +
                "WHERE c.contract_id=? AND d.product_id=? AND v.dataset_version_id=? AND c.status='ACTIVE'", Integer.class,
                request.contractId(), request.productId(), request.datasetVersionId());
        if (approved == null || approved == 0) throw new IllegalStateException("Only an ACTIVE ingestion contract matching product and dataset version can stage data");
        long batchId = insert("INSERT INTO ingest.batch(contract_id,product_id,status,checksum,started_at) VALUES (?,?, 'STAGING', ?, SYSUTCDATETIME())",
                request.contractId(), request.productId(), request.checksum());
        long artifactId = insert("INSERT INTO ingest.artifact(batch_id,original_name,format,object_uri,checksum,byte_size) VALUES (?,?,?,?,?,?)",
                batchId, request.artifactName(), request.artifactFormat(), request.objectUri(), request.checksum(), request.byteSize());
        long loadId = insert("INSERT INTO ingest.dataset_load(batch_id,dataset_version_id,source_name,source_row_count,accepted_count,rejected_count,status) VALUES (?,?,?,?,0,0,'STAGING')",
                batchId, request.datasetVersionId(), request.sourceName(), request.rows().size());

        List<PlatformIngestRow> rows = request.rows();
        dataPlane.batchUpdate("INSERT INTO ingest.staged_row(dataset_load_id,source_row_number,source_key,raw_payload_json,payload_hash,validation_status) VALUES (?,?,?,?,?,'PENDING')",
                rows, 500, (ps, row) -> {
                    ps.setLong(1, loadId);
                    ps.setLong(2, row.sourceRowNumber());
                    ps.setString(3, row.sourceKey());
                    ps.setString(4, row.payloadJson());
                    ps.setString(5, sha256(row.payloadJson()));
                });
        return new PlatformIngestReceipt(batchId, artifactId, loadId, rows.size(), "STAGING");
    }

    private long insert(String sql, Object... args) {
        KeyHolder key = new GeneratedKeyHolder();
        dataPlane.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int index = 0; index < args.length; index++) statement.setObject(index + 1, args[index]);
            return statement;
        }, key);
        if (key.getKey() == null) throw new IllegalStateException("Database did not return an identity key");
        return key.getKey().longValue();
    }

    private static void validate(PlatformIngestRequest request) {
        if (request.contractId() <= 0 || request.productId() <= 0 || request.datasetVersionId() <= 0) throw new IllegalArgumentException("contractId, productId and datasetVersionId must be positive");
        if (request.rows() == null || request.rows().isEmpty()) throw new IllegalArgumentException("At least one source row is required");
        if (request.objectUri() == null || !request.objectUri().startsWith("s3://")) throw new IllegalArgumentException("Artifact must be in private object storage (s3:// URI)");
        if (request.checksum() == null || !request.checksum().matches("[A-Fa-f0-9]{64}")) throw new IllegalArgumentException("checksum must be SHA-256 hex");
        for (PlatformIngestRow row : request.rows()) if (row.sourceRowNumber() < 1 || row.payloadJson() == null || row.payloadJson().isBlank()) throw new IllegalArgumentException("Each row needs a positive sourceRowNumber and payloadJson");
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
