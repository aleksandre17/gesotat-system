package org.base.api.service.artifact;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Resolves package structure exclusively from an approved Control Plane site contract. */
@Component
public class ArtifactPackageContractResolver {
    private final JdbcTemplate controlPlane;

    public ArtifactPackageContractResolver(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        this.controlPlane = controlPlane;
    }

    public DatasetContract resolve(String contractCode, int revision, String datasetCode) {
        if (contractCode == null || contractCode.isBlank() || revision < 1 || datasetCode == null || datasetCode.isBlank())
            throw new IllegalArgumentException("Approved contract code, revision, and dataset code are required");
        List<DatasetContract> matches = controlPlane.query(
                "SELECT r.contract_code,r.revision,r.contract_checksum,d.dataset_version_id,d.dataset_code,d.access_table_name " +
                        "FROM platform.site_contract_revision r JOIN platform.site_contract_dataset d ON d.site_contract_revision_id=r.site_contract_revision_id " +
                        "WHERE r.contract_code=? AND r.revision=? AND r.status='APPROVED' AND d.dataset_code=?",
                (rs, n) -> new DatasetContract(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getLong(4), rs.getString(5), rs.getString(6)),
                contractCode, revision, datasetCode);
        if (matches.size() != 1) throw new IllegalArgumentException("Dataset is not declared by the requested approved contract revision");
        DatasetContract contract = matches.get(0);
        List<String> fields = controlPlane.query(
                "SELECT f.field_name FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id " +
                        "JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id " +
                "WHERE r.contract_code=? AND r.revision=? AND r.status='APPROVED' AND d.dataset_code=? AND f.required=1 ORDER BY f.ordinal",
                (rs, n) -> rs.getString(1), contractCode, revision, datasetCode);
        return new DatasetContract(contract.contractCode(), contract.revision(), contract.contractChecksum(), contract.datasetVersionId(),
                contract.datasetCode(), contract.accessTableName(), fields);
    }

    public record DatasetContract(String contractCode, int revision, String contractChecksum, long datasetVersionId,
                                  String datasetCode, String accessTableName, List<String> fields) {
        public DatasetContract(String contractCode, int revision, String contractChecksum, long datasetVersionId,
                               String datasetCode, String accessTableName) {
            this(contractCode, revision, contractChecksum, datasetVersionId, datasetCode, accessTableName, List.of());
        }
        public DatasetContract { fields = List.copyOf(fields); }
    }
}
