package org.base.api.service.catalog;

import java.util.List;

/** Metadata extracted from a managed Access file. */
public record ManagedAccessPackage(String packageCode, String packageVersion,
                                   List<ManagedDatasetDefinition> datasets,
                                   List<ManagedChartDefinition> charts,
                                   List<ManagedChartFilter> chartFilters) {
}
