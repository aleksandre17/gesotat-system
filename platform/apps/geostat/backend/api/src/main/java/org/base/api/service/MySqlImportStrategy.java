package org.base.api.service;

import org.base.api.model.request.UploadPayload;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class MySqlImportStrategy implements DatabaseImportStrategy {
    private static final java.util.regex.Pattern IDENTIFIER = java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_-]{0,127}");
    private static final java.util.regex.Pattern ENDPOINT = java.util.regex.Pattern.compile("[A-Za-z0-9._-]+(?::[0-9]{1,5})?");
    @Override
    public void configureDataSource(UploadPayload payload, DriverManagerDataSource dataSource) {

        String database = payload.getMetaTargetDatabase();
        if (database == null || database.isBlank()) {
            database = parseDatabaseAndTableName(payload.getMetaDatabaseName())[0];
        }
        if (database.equalsIgnoreCase("international_ratings")) {
            database = "international-ratings";
        }
        validateEndpoint(payload.getMetaDatabaseUrl()); validate(database);
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + payload.getMetaDatabaseUrl() + "/" + database +
                "?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=false&serverTimezone=Asia/Tbilisi&rewriteBatchedStatements=true");
        dataSource.setUsername(payload.getMetaDatabaseUser());
        dataSource.setPassword(payload.getMetaDatabasePassword());
    }

    @Override
    public String quoteTableName(String rawTableName) {

        if (rawTableName.equalsIgnoreCase("international_ratings/international_ratings")) {
            rawTableName = "international-ratings/international_ratings";
        }

        if (rawTableName.equalsIgnoreCase("international_ratings/main_economic_indicator")) {
            rawTableName = "international-ratings/main_economic_indicator";
        }

        String[] parts = rawTableName.split("/");
        for (String part : parts) validate(part);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(".");
            sb.append("`").append(parts[i]).append("`");
        }

        return sb.toString();
    }

    @Override
    public String quoteColumn(String columnName) {
        validate(columnName);
        return "`" + columnName + "`";
    }

    @Override
    public int getMaxParamsPerBatch() {
        return 65535; // MySQL supports much larger batches; rewriteBatchedStatements handles optimization
    }
    private static void validate(String value) { if (value == null || !IDENTIFIER.matcher(value).matches()) throw new IllegalArgumentException("Unsafe SQL identifier"); }
    private static void validateEndpoint(String value) { if (value == null || !ENDPOINT.matcher(value).matches()) throw new IllegalArgumentException("Invalid database endpoint"); }
}
