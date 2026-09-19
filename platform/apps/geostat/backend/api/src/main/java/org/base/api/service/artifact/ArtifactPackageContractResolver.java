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
                "SELECT r.contract_code,r.revision,r.contract_checksum,d.dataset_version_id,d.dataset_code,t.access_table_name " +
                        "FROM platform.site_contract_revision r JOIN platform.site_contract_dataset d ON d.site_contract_revision_id=r.site_contract_revision_id " +
                        "JOIN platform.contract_table_definition t ON t.table_definition_id=d.contract_table_definition_id " +
                        "JOIN platform.contract_structure s ON s.structure_id=t.structure_id " +
                        "WHERE r.contract_code=? AND r.revision=? AND r.status='APPROVED' AND d.dataset_code=? " +
                        "AND t.lifecycle_status='APPROVED' AND s.lifecycle_status='APPROVED' " +
                        "AND t.revision=r.revision AND s.revision=r.revision",
                (rs, n) -> new DatasetContract(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getLong(4), rs.getString(5), rs.getString(6)),
                contractCode, revision, datasetCode);
        if (matches.size() != 1) throw new IllegalArgumentException("Dataset is not declared by the requested approved contract revision");
        DatasetContract contract = matches.get(0);
        List<DeclaredField> declared = controlPlane.query(
                "SELECT f.field_name,f.required,f.key_role FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id " +
                        "JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id " +
                "WHERE r.contract_code=? AND r.revision=? AND r.status='APPROVED' AND d.dataset_code=? ORDER BY f.ordinal",
                (rs, n) -> new DeclaredField(rs.getString(1), rs.getBoolean(2), rs.getString(3)), contractCode, revision, datasetCode);
        return new DatasetContract(contract.contractCode(), contract.revision(), contract.contractChecksum(), contract.datasetVersionId(),
                contract.datasetCode(), contract.accessTableName(),
                declared.stream().filter(DeclaredField::required).map(DeclaredField::name).toList(),
                declared.stream().filter(f -> f.keyRole() != null && !f.keyRole().isBlank()).map(DeclaredField::name).toList());
    }

    /** One contract field; a non-null key role makes it part of the row's stable source identity. */
    public record DeclaredField(String name, boolean required, String keyRole) {}

    /** Approved dataset structure: physical Access table, required fields and ordered key fields. */
    public record DatasetContract(String contractCode, int revision, String contractChecksum, long datasetVersionId,
                                  String datasetCode, String accessTableName, List<String> fields, List<String> keyFields) {
        public DatasetContract(String contractCode, int revision, String contractChecksum, long datasetVersionId,
                               String datasetCode, String accessTableName, List<String> fields) {
            this(contractCode, revision, contractChecksum, datasetVersionId, datasetCode, accessTableName, fields, List.of());
        }
        public DatasetContract(String contractCode, int revision, String contractChecksum, long datasetVersionId,
                               String datasetCode, String accessTableName) {
            this(contractCode, revision, contractChecksum, datasetVersionId, datasetCode, accessTableName, List.of());
        }
        public DatasetContract {
            fields = List.copyOf(fields);
            keyFields = List.copyOf(keyFields);
        }
    }
}
