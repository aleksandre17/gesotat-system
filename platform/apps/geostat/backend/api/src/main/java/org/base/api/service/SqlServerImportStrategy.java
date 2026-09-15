package org.base.api.service;

import org.base.api.model.request.UploadPayload;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SqlServerImportStrategy implements DatabaseImportStrategy {
    private static final java.util.regex.Pattern IDENTIFIER = java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_-]{0,127}");
    private static final java.util.regex.Pattern ENDPOINT = java.util.regex.Pattern.compile("[A-Za-z0-9._-]+(?::[0-9]{1,5})?");
    @Override
    public void configureDataSource(UploadPayload payload, DriverManagerDataSource dataSource) {
        String database = payload.getMetaTargetDatabase();
        if (database == null || database.isBlank()) {
            database = parseDatabaseAndTableName(payload.getMetaDatabaseName())[0];
        }
        validateEndpoint(payload.getMetaDatabaseUrl()); validate(database);
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl("jdbc:sqlserver://" + payload.getMetaDatabaseUrl() +
                ";databaseName=" + database +
                ";encrypt=true;useBulkCopyForBatchInsert=true;trustServerCertificate=false;serverTimezone=Asia/Tbilisi;cachePrepStmts=true;reWriteBatchedInserts=true");
        dataSource.setUsername(payload.getMetaDatabaseUser());
        dataSource.setPassword(payload.getMetaDatabasePassword());
    }

    @Override
    public String quoteTableName(String rawTableName) {
        String[] parts = rawTableName.split("/");
        for (String part : parts) validate(part);
        StringBuilder sb = new StringBuilder();

        if (parts.length == 2) {
            // For 2 parts: insert [dbo] in the middle
            sb.append("[").append(parts[0]).append("].");
            sb.append("[dbo].");
            sb.append("[").append(parts[1]).append("]");
        } else {
            // For 1 part or 3+ parts: just wrap all parts
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(".");
                sb.append("[").append(parts[i]).append("]");
            }
        }
        return sb.toString();
    }

    @Override
    public String quoteColumn(String columnName) {
        validate(columnName);
        return "[" + columnName + "]";
    }

    private static void validate(String value) { if (value == null || !IDENTIFIER.matcher(value).matches()) throw new IllegalArgumentException("Unsafe SQL identifier"); }
    private static void validateEndpoint(String value) { if (value == null || !ENDPOINT.matcher(value).matches()) throw new IllegalArgumentException("Invalid database endpoint"); }
}
