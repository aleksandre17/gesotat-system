package org.base.mobile;

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
