package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ContractClientGeneratorServiceTest {
    @Test void generatedTypeScriptIdentifiersAreSafe() {
        assertEquals("field_name", ContractClientGeneratorService.safeTsIdentifier("field-name"));
        assertEquals("_class", ContractClientGeneratorService.safeTsIdentifier("class"));
        assertEquals("_1value", ContractClientGeneratorService.safeTsIdentifier("1value"));
        assertFalse(ContractClientGeneratorService.safeTsIdentifier("x; drop table").contains(";"));
    }

    @Test void unsupportedSdkLanguageFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new ContractClientGeneratorService(null).sdk("X", 1, "python"));
    }

    @Test void generatesJavaKotlinAndDartFromContractSchema() {
        var discovery = Mockito.mock(ContractIntrospectionService.class);
        Mockito.when(discovery.contract("X", 1)).thenReturn(Map.of("pages", List.of(Map.of(
                "datasetCode", "KIDS_GOAL", "capabilities", Map.of("fields", List.of(
                        Map.of("name", "goal-id", "logicalType", "INTEGER"),
                        Map.of("name", "title", "logicalType", "TEXT")))))));
        var generator = new ContractClientGeneratorService(discovery);
        assertTrue(generator.sdk("X", 1, "java").contains("public record KidsGoal"));
        assertTrue(generator.sdk("X", 1, "kotlin").contains("data class KidsGoal"));
        assertTrue(generator.sdk("X", 1, "dart").contains("class KidsGoal"));
    }

    @Test void streamingExportEmitsBoundedProgressAndPayload() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var stream = new StreamingExportService(new ExportCodecRegistry(mapper), mapper);
        var out = new java.io.ByteArrayOutputStream(); var events = new java.util.ArrayList<StreamingExportService.Progress>();
        stream.write("JSON", Map.of("data", List.of(Map.of("id", 1))), out, events::add);
        assertTrue(out.size() > 0); assertTrue(events.get(events.size()-1).complete());
        assertEquals(events.get(events.size()-1).total(), events.get(events.size()-1).emitted());
    }

    @Test void streamingExportHonorsCancellationAndByteBudget() {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var stream = new StreamingExportService(new ExportCodecRegistry(mapper), mapper);
        assertThrows(java.util.concurrent.CancellationException.class, () -> stream.write("JSON", Map.of("data", List.of(Map.of("id", 1))), new java.io.ByteArrayOutputStream(), p -> {}, () -> true, 10000));
        assertThrows(IllegalStateException.class, () -> stream.write("JSON", Map.of("data", List.of(Map.of("id", 1))), new java.io.ByteArrayOutputStream(), p -> {}, () -> false, 1));
    }
}
