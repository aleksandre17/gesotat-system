package org.base.api.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Controlled authoring entry point for a reviewed semantic mapping; active contracts are immutable. */
@Service
public class PlatformContractMappingService {
    private final JdbcTemplate controlPlane;
    private final ObjectMapper json;

    public PlatformContractMappingService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane, ObjectMapper json) {
        this.controlPlane = controlPlane;
        this.json = json;
    }

    @Transactional(transactionManager = "primaryJdbcTransactionManager")
    public ContractMappingUpdateReceipt replace(long contractId, long contractSourceId, JsonNode mapping) {
        if (contractId <= 0 || contractSourceId <= 0) throw new IllegalArgumentException("Contract and contract source identifiers must be positive");
        if (mapping == null || !mapping.isObject()) throw new IllegalArgumentException("Semantic mapping must be a JSON object");
        String state = mapping.path("approvalState").asText();
        if (!"DRAFT".equalsIgnoreCase(state) && !"READY".equalsIgnoreCase(state)) {
            throw new IllegalArgumentException("Semantic mapping approvalState must be DRAFT or READY");
        }
        validateProjections(mapping);
        Map<String, Object> contract = controlPlane.query("SELECT c.status FROM platform.ingestion_contract c JOIN platform.contract_source s ON s.contract_id=c.contract_id WHERE c.contract_id=? AND s.contract_source_id=? AND s.active=1",
                rs -> rs.next() ? Map.of("status", rs.getString(1)) : null, contractId, contractSourceId);
        if (contract == null) throw new IllegalArgumentException("Active contract source does not belong to this contract");
        if ("ACTIVE".equals(contract.get("status"))) throw new IllegalStateException("Active contracts are immutable; create a new reviewed contract version");
        String serialized;
        try { serialized = json.writeValueAsString(mapping); }
        catch (Exception error) { throw new IllegalArgumentException("Semantic mapping cannot be serialized", error); }
        controlPlane.update("UPDATE platform.contract_source SET mapping_spec_json=? WHERE contract_source_id=?", serialized, contractSourceId);
        controlPlane.update("UPDATE platform.ingestion_contract SET status='REVIEW_REQUIRED' WHERE contract_id=? AND status='DRAFT'", contractId);
        controlPlane.update("INSERT INTO platform.outbox_event(aggregate_type,aggregate_id,event_type,payload_json,status) VALUES('CONTRACT_SOURCE',?,'SEMANTIC_MAPPING_UPDATED',?,'PENDING')",
                contractSourceId, "{\"contractId\":" + contractId + ",\"contractSourceId\":" + contractSourceId + ",\"approvalState\":\"" + state.toUpperCase() + "\"}");
        return new ContractMappingUpdateReceipt(contractId, contractSourceId, state.toUpperCase(), "REVIEW_REQUIRED");
    }

    private static void validateProjections(JsonNode mapping) {
        JsonNode projections = mapping.path("projections");
        if (projections.isMissingNode()) return; // A single-family mapping remains supported.
        if (!projections.isArray() || projections.isEmpty()) throw new IllegalArgumentException("projections must be a non-empty array");
        for (JsonNode projection : projections) {
            String family = projection.path("family").asText();
            if (family.isBlank()) throw new IllegalArgumentException("Every semantic projection requires family");
            if ("ENTITY".equalsIgnoreCase(family) && (projection.path("recordType").asText().isBlank() || projection.path("key").asText().isBlank())) {
                throw new IllegalArgumentException("ENTITY projection requires recordType and key");
            }
            if ("STATISTICAL_WIDE_JSON".equalsIgnoreCase(family) &&
                    (projection.path("arrayPath").asText().isBlank() || projection.path("periodField").asText().isBlank() ||
                            projection.path("metric").path("externalSystemCode").asText().isBlank() || projection.path("metric").path("code").asText().isBlank() ||
                            projection.path("pivotDimension").path("dimensionId").asLong(0) <= 0 || projection.path("pivotDimension").path("externalSystemCode").asText().isBlank())) {
                throw new IllegalArgumentException("STATISTICAL_WIDE_JSON projection has incomplete metric/dimension metadata");
            }
        }
    }
}
