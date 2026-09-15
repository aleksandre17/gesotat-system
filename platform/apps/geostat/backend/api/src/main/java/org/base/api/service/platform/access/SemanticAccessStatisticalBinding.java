package org.base.api.service.platform.access;

/** Optional per-carrier statistical evidence; meanings are still resolved only by the Control Plane. */
public record SemanticAccessStatisticalBinding(String sourceDatasetCode, String sourceExternalKey,
                                               String projectionCode, String dataflowCode,
                                               String bindingState, String evidenceKind) {
}
