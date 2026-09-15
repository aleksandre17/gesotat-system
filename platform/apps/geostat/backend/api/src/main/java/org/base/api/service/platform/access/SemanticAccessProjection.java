package org.base.api.service.platform.access;

/** Mapping JSON is declarative only and is never interpreted as SQL. */
public record SemanticAccessProjection(String projectionCode, String datasetCode, String projectionFamily,
                                       String mappingJson, String approvalState) {
}
