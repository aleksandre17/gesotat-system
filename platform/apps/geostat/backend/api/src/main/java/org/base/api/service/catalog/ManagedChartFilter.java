package org.base.api.service.catalog;

/** A declarative chart predicate. Raw SQL is intentionally not supported. */
public record ManagedChartFilter(String chartCode, String field, String operator, String value) {
}
