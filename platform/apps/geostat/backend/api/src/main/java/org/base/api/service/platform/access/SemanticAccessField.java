package org.base.api.service.platform.access;

public record SemanticAccessField(String datasetCode, String fieldName, String logicalType, String semanticRole, boolean required) {
}
