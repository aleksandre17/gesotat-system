package org.base.api.service.mobile_services;

/**
 * Configuration for classification tables.
 */
public record ClassificationTableConfig(
        String tableName,
        String alias,
        String keyColumn,
        boolean hasHexCode
) {
}
