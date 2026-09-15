package org.base.api.service.catalog;

/** One row of the package's __gs_chart metadata table. */
public record ManagedChartDefinition(String chartCode, String datasetCode, String chartType,
                                     String xField, String yField, String seriesField,
                                     String aggregation, String publicationMode) {
}
