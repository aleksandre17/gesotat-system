package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

/** Explicit stewardship gate before a source contract may execute. */
@Service
public class PlatformContractGovernanceService {
    private final JdbcTemplate controlPlane;
    private final ObjectMapper json;

    public PlatformContractGovernanceService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane, ObjectMapper json) {
        this.controlPlane = controlPlane;
        this.json = json;
    }

    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public ContractApprovalReceipt approve(long contractId) {
        if (contractId <= 0) throw new IllegalArgumentException("contractId must be positive");
        Map<String, Object> contract = controlPlane.query("SELECT d.product_id,c.status FROM platform.ingestion_contract c JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE c.contract_id=?", rs ->
                rs.next() ? Map.of("product",rs.getLong(1),"status",rs.getString(2)) : null, contractId);
        if (contract == null) throw new IllegalArgumentException("Ingestion contract not found");
        if ("ACTIVE".equals(contract.get("status"))) throw new IllegalStateException("Contract is already active");
        List<Map<String, Object>> sources = controlPlane.queryForList("SELECT contract_source_id,target_dataset_version_id,source_locator,mapping_spec_json FROM platform.contract_source WHERE contract_id=? AND active=1 ORDER BY load_order", contractId);
        if (sources.isEmpty()) throw new IllegalStateException("A contract must contain at least one active source mapping");
        Set<Long> versions = new LinkedHashSet<>();
        for (Map<String, Object> source : sources) {
            if (source.get("source_locator") == null || String.valueOf(source.get("source_locator")).isBlank()) throw new IllegalStateException("Contract source locator is missing");
            try {
                var mapping = json.readTree((String) source.get("mapping_spec_json"));
                String approvalState = mapping.path("approvalState").asText();
                if (!approvalState.isBlank() && !"READY".equalsIgnoreCase(approvalState)) {
                    throw new IllegalStateException("Semantic mapping is not ready for source " + source.get("contract_source_id") + ": " + approvalState);
                }
                if (mapping.path("projections").isArray() && mapping.path("projections").isEmpty()) {
                    throw new IllegalStateException("Semantic projection list is empty for source " + source.get("contract_source_id"));
                }
            } catch (IllegalStateException error) { throw error; }
            catch (Exception error) { throw new IllegalStateException("Contract mapping is invalid for source " + source.get("contract_source_id"), error); }
            versions.add(((Number) source.get("target_dataset_version_id")).longValue());
        }
        for (long version : versions) {
            Integer belongs = controlPlane.queryForObject("SELECT COUNT(*) FROM platform.dataset_version v JOIN platform.dataset d ON d.dataset_id=v.dataset_id WHERE v.dataset_version_id=? AND d.product_id=?", Integer.class, version, contract.get("product"));
            if (belongs == null || belongs == 0) throw new IllegalStateException("Contract source version does not belong to the contract product");
            controlPlane.update("UPDATE platform.dataset_version SET status='APPROVED',effective_from=COALESCE(effective_from,SYSUTCDATETIME()) WHERE dataset_version_id=?", version);
        }
        controlPlane.update("UPDATE platform.ingestion_contract SET status='ACTIVE',auto_publish=0 WHERE contract_id=?", contractId);
        controlPlane.update("INSERT INTO platform.outbox_event(aggregate_type,aggregate_id,event_type,payload_json,status) VALUES('INGESTION_CONTRACT',?,'CONTRACT_APPROVED',?,'PENDING')", contractId, "{\"contractId\":" + contractId + ",\"datasetVersionIds\":" + versions + "}");
        return new ContractApprovalReceipt(contractId, List.copyOf(versions), "ACTIVE", "APPROVED");
    }

    /** Idempotently persists the immutable approval evidence after gate evaluation. */
    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public void persistApprovalEvidence(long contractId, int revision, String document, String evidenceJson) {
        if (contractId <= 0 || revision <= 0) throw new IllegalArgumentException("contract identity is required");
        if (!ContractChecksumBinding.matches(document, ContractChecksumBinding.sha256(document))) throw new IllegalStateException("checksum binding failed");
        String checksum = ContractChecksumBinding.sha256(document);
        controlPlane.update("IF NOT EXISTS (SELECT 1 FROM platform.contract_approval_receipt WHERE contract_id=? AND contract_revision=? AND contract_checksum=?) INSERT platform.contract_approval_receipt(contract_id,contract_revision,contract_checksum,decision_code,evidence_json) VALUES(?,?,?,'APPROVED',?)", contractId, revision, checksum, contractId, revision, checksum, evidenceJson == null ? "{}" : evidenceJson);
    }
}
