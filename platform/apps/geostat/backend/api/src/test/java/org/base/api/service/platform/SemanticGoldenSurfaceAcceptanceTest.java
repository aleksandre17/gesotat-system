package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Golden semantic corpus covering every contract compatibility surface. */
class SemanticGoldenSurfaceAcceptanceTest {
    @Test void additiveSurfaceChangesRemainCompatible() {
        Map<String,Object> from = new LinkedHashMap<>();
        from.put("metric", "COUNT"); from.put("unit", "PERSON"); from.put("aggregation", "SUM");
        from.put("dimensions", List.of("TIME")); from.put("codeLists", List.of("AGE"));
        from.put("allowedFilters", List.of("period")); from.put("allowedIncludes", List.of("classifier"));
        from.put("projection", Map.of("fields", Map.of("value", "INTEGER")));
        from.put("responseSchema", Map.of("data", List.of("value"))); from.put("policies", List.of("PUBLIC"));
        Map<String,Object> to = new LinkedHashMap<>(from);
        to.put("dimensions", List.of("TIME", "SEX")); to.put("codeLists", List.of("AGE", "SEX"));
        to.put("allowedFilters", List.of("period", "carrier")); to.put("allowedIncludes", List.of("classifier", "lineage"));
        to.put("projection", Map.of("fields", Map.of("value", "INTEGER", "unit", "CODE")));
        to.put("responseSchema", Map.of("data", List.of("value"))); to.put("policies", List.of("PUBLIC", "ANONYMOUS_READ"));
        var result = SemanticCompatibilityAnalyzer.compare(from, to);
        assertFalse(result.breaking());
    }

    @Test void removalsAndScalarChangesAreBreakingAndRequireApproval() {
        Map<String,Object> from = Map.of("metric", "COUNT", "unit", "PERSON", "aggregation", "SUM",
                "dimensions", List.of("TIME", "SEX"), "codeLists", List.of("AGE", "SEX"),
                "allowedFilters", List.of("period", "carrier"), "allowedIncludes", List.of("classifier", "lineage"),
                "projection", Map.of("fields", Map.of("value", "INTEGER", "unit", "CODE")),
                "responseSchema", Map.of("data", List.of("value", "unit")), "policies", List.of("PUBLIC"));
        Map<String,Object> to = Map.of("metric", "COUNT", "unit", "HOUSEHOLD", "aggregation", "SUM",
                "dimensions", List.of("TIME"), "codeLists", List.of("AGE"),
                "allowedFilters", List.of("period"), "allowedIncludes", List.of("classifier"),
                "projection", Map.of("fields", Map.of("value", "DECIMAL")),
                "responseSchema", Map.of("data", List.of("value")), "policies", List.of("PUBLIC"));
        var result = SemanticCompatibilityAnalyzer.compare(from, to);
        assertTrue(result.breaking());
        assertTrue(result.approvalRequired());
        assertFalse(result.breakingChanges().isEmpty());
        assertTrue(result.migrationGuidance().contains("approved revision"));
    }
}
