package org.base.api.service.platform.access;

public record SemanticAccessKey(String datasetCode, String fieldName, String keyRole, int keyOrder) {
}
