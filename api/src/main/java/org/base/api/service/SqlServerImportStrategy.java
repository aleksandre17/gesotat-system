package org.base.api.service;

import org.base.api.model.request.UploadPayload;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SqlServerImportStrategy implements DatabaseImportStrategy {
    @Override
    public void configureDataSource(UploadPayload payload, DriverManagerDataSource dataSource) {
        String[] dbAndTable = parseDatabaseAndTableName(payload.getMetaDatabaseName());
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl("jdbc:sqlserver://" + payload.getMetaDatabaseUrl() +
                ";databaseName=" + dbAndTable[0] +
                ";encrypt=true;useBulkCopyForBatchInsert=true;trustServerCertificate=true;serverTimezone=Asia/Tbilisi;cachePrepStmts=true;reWriteBatchedInserts=true");
        dataSource.setUsername(payload.getMetaDatabaseUser());
        dataSource.setPassword(payload.getMetaDatabasePassword());
    }

    @Override
    public String quoteTableName(String rawTableName) {
        String[] parts = rawTableName.split("/");
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
        return "[" + columnName + "]";
    }
}
