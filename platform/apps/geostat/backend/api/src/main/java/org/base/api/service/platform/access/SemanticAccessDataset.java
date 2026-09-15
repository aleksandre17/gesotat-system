package org.base.api.service.platform.access;

/** One logical dataset declared by a v3 Access semantic package. */
public record SemanticAccessDataset(String datasetCode, String accessTableName, String dataFamily, String businessGrain) {
}
