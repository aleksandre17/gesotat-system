package org.base.api.service;

import org.base.api.model.request.UploadPayload;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedImportStrategyTest {

    @Test
    void mysqlManagedTargetKeepsHyphenatedDatabaseNameIntact() {
        UploadPayload payload = payload("international-ratings");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        new MySqlImportStrategy().configureDataSource(payload, dataSource);

        assertTrue(dataSource.getUrl().contains("/international-ratings?"));
    }

    @Test
    void sqlServerManagedTargetUsesExplicitTargetDatabase() {
        UploadPayload payload = payload("geomap");
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        new SqlServerImportStrategy().configureDataSource(payload, dataSource);

        assertTrue(dataSource.getUrl().contains("databaseName=geomap;"));
    }

    private UploadPayload payload(String database) {
        UploadPayload payload = new UploadPayload();
        payload.setMetaDatabaseUrl("localhost:3306");
        payload.setMetaDatabaseName("legacy-name-table");
        payload.setMetaTargetDatabase(database);
        return payload;
    }
}
