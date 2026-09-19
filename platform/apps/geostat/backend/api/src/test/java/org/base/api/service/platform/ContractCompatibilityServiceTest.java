package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ContractCompatibilityServiceTest {
    @Test
    void reportsBreakingFieldRemovalAndRequiresApprovalForUnapprovedRevision() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        when(db.queryForMap(anyString(), any(), any())).thenAnswer(inv -> {
            int revision = ((Number) inv.getArgument(2)).intValue();
            return Map.of("revision", revision, "status", revision == 1 ? "APPROVED" : "DRAFT", "contract_checksum", "c" + revision);
        });
        when(db.queryForObject(anyString(), eq(Long.class), any(), any())).thenAnswer(inv -> ((Number) inv.getArgument(3)).longValue());
        doAnswer(inv -> {
            String sql = inv.getArgument(0, String.class);
            long id = ((Number) inv.getArgument(2)).longValue();
            if (sql.contains("site_contract_field")) {
                return id == 1L ? List.of(Map.entry("KIDS_GOAL.id", "INTEGER:true:IDENTIFIER:PRIMARY:::CANONICAL:1:id")) : List.of();
            }
            if (sql.contains("site_contract_dataset")) {
                return id == 1L ? List.of(Map.entry("KIDS_GOAL", "ENTITY:goal:id:CURRENT")) : List.of();
            }
            return List.of();
        }).when(db).query(anyString(), any(RowMapper.class), any(Object[].class));

        Map<String, Object> result = new ContractCompatibilityService(db).compare("KIDS_PORTAL_V1", 1, 2);

        assertEquals("BREAKING", result.get("compatibility"));
        assertTrue(((List<?>) result.get("removedFields")).contains("KIDS_GOAL.id"));
        assertTrue((Boolean) result.get("approvalRequired"));
    }

    @Test
    void rejectsInvalidRevisionPairBeforeDatabaseAccess() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        assertThrows(IllegalArgumentException.class, () -> new ContractCompatibilityService(db).compare("KIDS", 2, 2));
        verifyNoInteractions(db);
    }

    @Test
    void comparesPersistedSemanticDocumentAndReturnsMigrationGuidance() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        org.mockito.stubbing.Answer<Map<String,Object>> semanticAnswer = inv -> {
            Object[] arguments = inv.getArguments();
            Object revisionValue = arguments.length >= 3 ? arguments[2] : ((Object[]) arguments[1])[1];
            int revision = ((Number) revisionValue).intValue();
            String document = revision == 1 ? "{\"metric\":\"COUNT\",\"unit\":\"PERSON\"}" : "{\"metric\":\"COUNT\",\"unit\":\"HOUSEHOLD\"}";
            // Only columns the statement actually selects are returned, so a missing column cannot be masked by the stub.
            String sql = inv.getArgument(0, String.class);
            Map<String,Object> row = new java.util.LinkedHashMap<>(Map.of("revision", revision, "status", "APPROVED", "contract_checksum", "c" + revision));
            if (sql.contains("contract_document_json")) row.put("contract_document_json", document);
            return row;
        };
        when(db.queryForMap(anyString(), any(), any())).thenAnswer(semanticAnswer);
        when(db.queryForMap(anyString(), any(Object[].class))).thenAnswer(semanticAnswer);
        when(db.queryForObject(anyString(), eq(Long.class), any(), any())).thenAnswer(inv -> ((Number) inv.getArgument(3)).longValue());
        doAnswer(inv -> List.of()).when(db).query(anyString(), any(RowMapper.class), any(Object[].class));
        Map<String,Object> result = new ContractCompatibilityService(db).compare("GENERIC", 1, 2);
        assertEquals("BREAKING", result.get("semanticCompatibility"));
        assertTrue((Boolean) result.get("semanticApprovalRequired"));
        assertTrue(String.valueOf(result.get("semanticMigrationGuidance")).contains("approved revision"));
        assertTrue(((List<?>) result.get("breakingChanges")).stream().anyMatch(x -> String.valueOf(x).startsWith("SEMANTIC:unit changed")));
    }
}
