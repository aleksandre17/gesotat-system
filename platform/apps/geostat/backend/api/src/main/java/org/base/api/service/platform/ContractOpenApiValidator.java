package org.base.api.service.platform;

import java.util.*;

/** Validates generated OpenAPI surface without knowing a product or provider. */
public final class ContractOpenApiValidator {
    private ContractOpenApiValidator() { }

    public static void validate(Map<String, Object> document) {
        if (document == null || !"3.1.0".equals(document.get("openapi"))) throw new IllegalArgumentException("OpenAPI 3.1 document is required");
        if (!(document.get("info") instanceof Map<?, ?> info) || String.valueOf(info.get("version")).isBlank()) throw new IllegalArgumentException("OpenAPI info.version is required");
        if (!(document.get("paths") instanceof Map<?, ?> paths)) throw new IllegalArgumentException("OpenAPI paths are required");
        Set<String> operationIds = new HashSet<>();
        for (var entry : paths.entrySet()) {
            if (!(entry.getKey() instanceof String path) || !path.startsWith("/")) throw new IllegalArgumentException("OpenAPI path is invalid");
            if (!(entry.getValue() instanceof Map<?, ?> item) || !(item.get("post") instanceof Map<?, ?> operation)) throw new IllegalArgumentException("OpenAPI path must declare POST operation");
            String operationId = String.valueOf(operation.get("operationId"));
            if (operationId.isBlank() || !operationIds.add(operationId)) throw new IllegalArgumentException("OpenAPI operationId must be unique");
        }
    }
}
