package org.base.api.service.platform.access;

public record SemanticAccessRelation(String relationshipCode, String fromDatasetCode, String fromField,
                                     String toDatasetCode, String toField, String cardinality, boolean required) {
}
