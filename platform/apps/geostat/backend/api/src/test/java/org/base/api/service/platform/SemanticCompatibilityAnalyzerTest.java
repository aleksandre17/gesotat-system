package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SemanticCompatibilityAnalyzerTest {
    @Test void persistedGoldenCorpusCoversCompatibilityOutcomes() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/semantic-golden-corpus.json")) {
            assertNotNull(input, "golden corpus must be packaged");
            var cases = new ObjectMapper().readValue(input, new TypeReference<List<Map<String,Object>>>() {});
            assertEquals(3, cases.size());
            for (var fixture : cases) {
                var result = SemanticCompatibilityAnalyzer.compare(
                        (Map<String,Object>) fixture.get("from"),
                        (Map<String,Object>) fixture.get("to"));
                assertEquals(fixture.get("expected"), result.compatibility(), String.valueOf(fixture.get("name")));
            }
        }
    }
    @Test void additiveDimensionsAreCompatibleButRemovalBreaks() {
        Map<String,Object> oldModel = new HashMap<>(); oldModel.put("metric", "COUNT"); oldModel.put("unit", "PERSON"); oldModel.put("dimensions", List.of("TIME", "AGE"));
        Map<String,Object> newModel = new HashMap<>(oldModel); newModel.put("dimensions", List.of("TIME", "AGE", "SEX"));
        assertFalse(SemanticCompatibilityAnalyzer.compare(oldModel, newModel).breaking());
        newModel.put("dimensions", List.of("TIME"));
        assertTrue(SemanticCompatibilityAnalyzer.compare(oldModel, newModel).breaking());
    }

    @Test void scalarSemanticChangeIsBreaking() {
        var oldModel = Map.<String,Object>of("metric", "COUNT", "unit", "PERSON", "aggregation", "SUM");
        var newModel = Map.<String,Object>of("metric", "COUNT", "unit", "HOUSEHOLD", "aggregation", "SUM");
        var result = SemanticCompatibilityAnalyzer.compare(oldModel, newModel);
        assertEquals("BREAKING", result.compatibility());
        assertTrue(result.breakingChanges().contains("unit changed"));
        assertTrue(result.approvalRequired());
        assertTrue(result.migrationGuidance().contains("approved revision"));
    }

    @Test void invalidSemanticArrayFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> SemanticCompatibilityAnalyzer.compare(Map.of("dimensions", List.of("TIME")), Map.of("dimensions", "TIME")));
    }

    @Test void nestedProjectionAndResponseSchemaChangesAreClassified() {
        var oldModel = Map.<String,Object>of("projection", Map.of("fields", Map.of("value", "DECIMAL")),
                "responseSchema", Map.of("observations", List.of("value")));
        var additive = Map.<String,Object>of("projection", Map.of("fields", Map.of("value", "DECIMAL", "unit", "CODE")),
                "responseSchema", Map.of("observations", List.of("value"), "metadata", List.of("unit")));
        assertFalse(SemanticCompatibilityAnalyzer.compare(oldModel, additive).breaking());
        var breaking = Map.<String,Object>of("projection", Map.of("fields", Map.of("value", "INTEGER")),
                "responseSchema", Map.of("observations", List.of("value")));
        var result = SemanticCompatibilityAnalyzer.compare(oldModel, breaking);
        assertTrue(result.breaking());
        assertTrue(result.breakingChanges().stream().anyMatch(x -> x.contains("projection.fields.value changed")));
    }

    @Test void goldenSetCompatibilityCoversAllContractSurfaceCollections() {
        var oldModel = Map.<String,Object>of(
                "dimensions", List.of("TIME"), "codeLists", List.of("AGE"),
                "allowedFilters", List.of("period"), "allowedIncludes", List.of("lineage"),
                "policies", List.of("PUBLIC"), "projectionFields", List.of("value"),
                "responseFields", List.of("observations"));
        var additive = new HashMap<>(oldModel);
        additive.put("dimensions", List.of("TIME", "SEX"));
        additive.put("codeLists", List.of("AGE", "SEX"));
        additive.put("allowedFilters", List.of("period", "carrier"));
        additive.put("allowedIncludes", List.of("lineage", "classifier"));
        additive.put("policies", List.of("PUBLIC", "ANONYMOUS_READ"));
        additive.put("projectionFields", List.of("value", "unit"));
        additive.put("responseFields", List.of("observations", "metadata"));
        assertFalse(SemanticCompatibilityAnalyzer.compare(oldModel, additive).breaking());
        var removal = new HashMap<>(additive);
        removal.put("codeLists", List.of("AGE"));
        var result = SemanticCompatibilityAnalyzer.compare(additive, removal);
        assertTrue(result.breaking());
        assertTrue(result.approvalRequired());
        assertTrue(result.breakingChanges().stream().anyMatch(x -> x.startsWith("codeLists removed:")));
    }
}
