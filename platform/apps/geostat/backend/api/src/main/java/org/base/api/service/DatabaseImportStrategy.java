package org.base.api.service;

import org.base.api.model.request.UploadPayload;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public interface DatabaseImportStrategy {
    void configureDataSource(UploadPayload payload, DriverManagerDataSource dataSource);
    String quoteTableName(String tableName);
    String quoteColumn(String columnName);

    /**
     * Maximum number of parameters allowed per batch execution.
     * SQL Server has a hard limit of 2100; MySQL's limit is much higher.
     * Used to auto-calculate the optimal batch size per table.
     */
    default int getMaxParamsPerBatch() {
        return 2000; // Safe default for SQL Server (2100 hard limit minus safety margin)
    }

    default  String[] parseDatabaseAndTableName(String fileName) {
        if (fileName == null || !fileName.contains("-")) {
            throw new IllegalArgumentException("fileName must be in the format 'database-table'");
        }
        String[] parts = fileName.split("-", 2);
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            throw new IllegalArgumentException("fileName must be in the format 'database-table'");
        }
        return parts;
    }
}
