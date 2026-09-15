package org.base.api.service.catalog;

/** One row of the package's __gs_dataset metadata table. */
public record ManagedDatasetDefinition(String datasetCode, String accessTableName, String profileCode,
                                       String dataKind, String rowKey, boolean required) {
}
