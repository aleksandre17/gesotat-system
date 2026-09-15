package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ContractOpenApiValidatorTest {
    @Test void acceptsGeneratedOpenApiSurface() {
        var doc = Map.<String,Object>of("openapi", "3.1.0", "info", Map.of("version", "8"),
                "paths", Map.of("/x", Map.of("post", Map.of("operationId", "queryX"))));
        assertDoesNotThrow(() -> ContractOpenApiValidator.validate(doc));
    }
    @Test void rejectsInvalidVersionPathAndDuplicateOperation() {
        assertThrows(IllegalArgumentException.class, () -> ContractOpenApiValidator.validate(Map.of("openapi", "3.0.0", "info", Map.of("version", "1"), "paths", Map.of())));
        var duplicate = Map.of("openapi", "3.1.0", "info", Map.of("version", "1"), "paths", Map.of(
                "/a", Map.of("post", Map.of("operationId", "same")), "/b", Map.of("post", Map.of("operationId", "same"))));
        assertThrows(IllegalArgumentException.class, () -> ContractOpenApiValidator.validate(duplicate));
    }
}
