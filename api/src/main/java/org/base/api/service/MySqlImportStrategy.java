package org.base.api.service;

import org.base.api.model.request.UploadPayload;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MySqlImportStrategy implements DatabaseImportStrategy {
    @Override
    public void configureDataSource(UploadPayload payload, DriverManagerDataSource dataSource) {
        String[] dbAndTable = parseDatabaseAndTableName(payload.getMetaDatabaseName());
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + payload.getMetaDatabaseUrl() + "/" + dbAndTable[0] +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Tbilisi&rewriteBatchedStatements=true");
        dataSource.setUsername(payload.getMetaDatabaseUser());
        dataSource.setPassword(payload.getMetaDatabasePassword());
    }

    @Override
    public String quoteTableName(String rawTableName) {
        String[] parts = rawTableName.split("/");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(".");
            sb.append("`").append(parts[i]).append("`");
        }

        return sb.toString();
    }

    @Override
    public String quoteColumn(String columnName) {
        return "`" + columnName + "`";
    }
}
