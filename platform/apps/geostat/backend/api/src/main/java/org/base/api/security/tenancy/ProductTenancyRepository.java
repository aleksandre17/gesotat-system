package org.base.api.security.tenancy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the owning data product of a governed object, from the Control Plane and — for objects the
 * Data Plane addresses by id — through their {@code dataset_version_id}. Resolution only; the decision
 * belongs to {@link TenantAccessPolicy}.
 */
@Repository
public class ProductTenancyRepository {
    private static final String PRODUCT_COLUMNS = "p.product_id,p.product_code,p.tenant_key";
    private static final RowMapper<ProductTenancy> MAPPER =
            (rs, n) -> new ProductTenancy(rs.getLong(1), rs.getString(2), rs.getString(3));

    private final JdbcTemplate controlPlane;
    private final JdbcTemplate dataPlane;

    public ProductTenancyRepository(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane,
                                    @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane) {
        this.controlPlane = controlPlane;
        this.dataPlane = dataPlane;
    }

    public Optional<ProductTenancy> byProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) return Optional.empty();
        return first(controlPlane.query("SELECT " + PRODUCT_COLUMNS + " FROM platform.data_product p WHERE p.product_code=?",
                MAPPER, productCode));
    }

    public Optional<ProductTenancy> byProductId(long productId) {
        return first(controlPlane.query("SELECT " + PRODUCT_COLUMNS + " FROM platform.data_product p WHERE p.product_id=?",
                MAPPER, productId));
    }

    /** A site contract code belongs to one product; the newest revision carries the current binding. */
    public Optional<ProductTenancy> byContractCode(String contractCode) {
        if (contractCode == null || contractCode.isBlank()) return Optional.empty();
        return first(controlPlane.query("SELECT TOP 1 " + PRODUCT_COLUMNS + " FROM platform.site_contract_revision r"
                + " JOIN platform.data_product p ON p.product_id=r.product_id WHERE r.contract_code=?"
                + " ORDER BY r.revision DESC,r.site_contract_revision_id DESC", MAPPER, contractCode));
    }

    public Optional<ProductTenancy> byDatasetVersionId(Long datasetVersionId) {
        if (datasetVersionId == null) return Optional.empty();
        return first(controlPlane.query("SELECT TOP 1 " + PRODUCT_COLUMNS + " FROM platform.dataset_version v"
                + " JOIN platform.dataset d ON d.dataset_id=v.dataset_id"
                + " JOIN platform.data_product p ON p.product_id=d.product_id WHERE v.dataset_version_id=?", MAPPER, datasetVersionId));
    }

    public Optional<ProductTenancy> byManifestId(long manifestId) {
        return byDatasetVersionId(dataPlaneDatasetVersion(
                "SELECT dataset_version_id FROM ingest.artifact_manifest WHERE artifact_manifest_id=?", manifestId));
    }

    public Optional<ProductTenancy> bySnapshotId(long datasetSnapshotId) {
        return byDatasetVersionId(dataPlaneDatasetVersion(
                "SELECT dataset_version_id FROM publication.dataset_snapshot WHERE dataset_snapshot_id=?", datasetSnapshotId));
    }

    public Optional<ProductTenancy> byPackageRunId(long packageRunId) {
        return byDatasetVersionId(dataPlaneDatasetVersion(
                "SELECT dataset_version_id FROM ingest.artifact_package_run WHERE package_run_id=?", packageRunId));
    }

    public Optional<ProductTenancy> byUploadSessionId(UUID uploadSessionId) {
        if (uploadSessionId == null) return Optional.empty();
        return byDatasetVersionId(dataPlaneDatasetVersion(
                "SELECT dataset_version_id FROM ingest.artifact_upload_session WHERE upload_session_id=?", uploadSessionId));
    }

    public Optional<ProductTenancy> byDatasetLoadId(long datasetLoadId) {
        return byDatasetVersionId(dataPlaneDatasetVersion(
                "SELECT dataset_version_id FROM ingest.dataset_load WHERE dataset_load_id=?", datasetLoadId));
    }

    /** An ingest batch names its product directly; it is created for one product. */
    public Optional<ProductTenancy> byIngestBatchId(long batchId) {
        Long productId = dataPlane.query("SELECT product_id FROM ingest.batch WHERE batch_id=?",
                rs -> rs.next() ? (Long) rs.getObject(1) : null, batchId);
        return productId == null ? Optional.empty() : byProductId(productId);
    }

    public Optional<ProductTenancy> byIngestionContractId(long contractId) {
        return first(controlPlane.query("SELECT TOP 1 " + PRODUCT_COLUMNS + " FROM platform.ingestion_contract c"
                + " JOIN platform.dataset d ON d.dataset_id=c.dataset_id"
                + " JOIN platform.data_product p ON p.product_id=d.product_id WHERE c.contract_id=?", MAPPER, contractId));
    }

    public Optional<ProductTenancy> byContractSourceId(long contractSourceId) {
        return first(controlPlane.query("SELECT TOP 1 " + PRODUCT_COLUMNS + " FROM platform.contract_source s"
                + " JOIN platform.ingestion_contract c ON c.contract_id=s.contract_id"
                + " JOIN platform.dataset d ON d.dataset_id=c.dataset_id"
                + " JOIN platform.data_product p ON p.product_id=d.product_id WHERE s.contract_source_id=?", MAPPER, contractSourceId));
    }

    /** An async operation carries the contract it was submitted for; without one it names no product. */
    public Optional<ProductTenancy> byAsyncOperationId(long operationId) {
        String contractCode = controlPlane.query("SELECT contract_code FROM platform.api_operation WHERE operation_id=?",
                rs -> rs.next() ? rs.getString(1) : null, operationId);
        return byContractCode(contractCode);
    }

    /** Current owner plus the append-only assignment history head, for the governed assignment API. */
    public Optional<ProductTenancy> lockByProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) return Optional.empty();
        return first(controlPlane.query("SELECT " + PRODUCT_COLUMNS + " FROM platform.data_product p WITH (UPDLOCK, HOLDLOCK)"
                + " WHERE p.product_code=?", MAPPER, productCode));
    }

    public void assign(long productId, String previousTenantKey, String tenantKey, boolean transfer, String reason, String assignedBy) {
        controlPlane.update("UPDATE platform.data_product SET tenant_key=?,updated_at=SYSUTCDATETIME() WHERE product_id=?", tenantKey, productId);
        controlPlane.update("INSERT INTO platform.data_product_tenant_assignment"
                + "(product_id,previous_tenant_key,tenant_key,transfer,reason,assigned_by) VALUES(?,?,?,?,?,?)",
                productId, previousTenantKey, tenantKey, transfer ? 1 : 0, reason, assignedBy);
    }

    private Long dataPlaneDatasetVersion(String sql, Object key) {
        return dataPlane.query(sql, rs -> rs.next() ? (Long) rs.getObject(1) : null, key);
    }

    private static Optional<ProductTenancy> first(java.util.List<ProductTenancy> rows) {
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
