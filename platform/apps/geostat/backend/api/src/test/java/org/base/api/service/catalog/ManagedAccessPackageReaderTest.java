package org.base.api.service.catalog;

import com.healthmarketscience.jackcess.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ManagedAccessPackageReaderTest {

    @Test
    void readsManagedPackageMetadataFromAnActualAccessFile() throws Exception {
        File file = Files.createTempFile("managed-package-", ".accdb").toFile();
        try (Database database = DatabaseBuilder.create(Database.FileFormat.V2010, file)) {
            table(database, "__gs_package", "package_code", "package_version").addRow("pilot", "1.0");
            table(database, "__gs_dataset", "dataset_code", "access_table_name", "profile_code", "data_kind", "row_key", "required")
                    .addRow("MAIN", "main_data", "main-economic-indicator", "STATISTICAL", "id", "true");
            table(database, "__gs_chart", "chart_code", "dataset_code", "chart_type", "x_field", "y_field", "series_field", "aggregation", "publication_mode")
                    .addRow("gdp-by-year", "MAIN", "LINE", "year", "gdp", null, "SUM", "DRAFT");
            table(database, "__gs_chart_filter", "chart_code", "field", "operator", "value")
                    .addRow("gdp-by-year", "country_id", "EQ", "1");
            table(database, "main_data", "id", "year", "gdp").addRow("1", "2024", "100");
        }

        AccessCatalog catalog = new AccessCatalogService().catalog(file);
        ManagedAccessPackage accessPackage = new ManagedAccessPackageReader().read(file);

        assertTrue(catalog.managedPackage());
        assertEquals("pilot", accessPackage.packageCode());
        assertEquals(1, accessPackage.datasets().size());
        assertEquals("main_data", accessPackage.datasets().get(0).accessTableName());
        assertEquals("gdp-by-year", accessPackage.charts().get(0).chartCode());
        assertEquals("EQ", accessPackage.chartFilters().get(0).operator());
    }

    private Table table(Database database, String name, String... columns) throws Exception {
        TableBuilder builder = new TableBuilder(name);
        for (String column : columns) builder.addColumn(new ColumnBuilder(column, DataType.TEXT));
        return builder.toTable(database);
    }
}
