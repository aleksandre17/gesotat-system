package org.base.api.service;

public class ImportStrategyFactory {
    public static DatabaseImportStrategy getStrategy(String dbType) {
        switch (dbType.toLowerCase()) {
            case "sqlserver":
                return new SqlServerImportStrategy();
            case "mysql":
                return new MySqlImportStrategy();
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
}
