import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;

import java.io.File;

/**
 * Creates a small self-describing GeoStat Managed Access Package v1 fixture.
 * Usage: java ... AccessPackageFixtureGenerator [output.accdb]
 */
public final class AccessPackageFixtureGenerator {
    private AccessPackageFixtureGenerator() { }

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "samples/managed-access-package-pilot.accdb" : args[0]);
        if (output.exists()) {
            throw new IllegalStateException("Refusing to overwrite existing file: " + output.getAbsolutePath());
        }
        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create directory: " + parent);
        }

        try (Database db = DatabaseBuilder.create(Database.FileFormat.V2010, output)) {
            textTable(db, "__gs_package", "package_code", "package_version")
                    .addRow("international-ratings-2026", "2026.1");
            textTable(db, "__gs_dataset", "dataset_code", "access_table_name", "profile_code", "data_kind", "row_key", "required")
                    .addRow("MAIN_ECONOMIC_INDICATOR", "main_economic_indicator", "main-economic-indicator", "STATISTICAL", "country_id,year", "true");
            textTable(db, "__gs_chart", "chart_code", "dataset_code", "chart_type", "x_field", "y_field", "series_field", "aggregation", "publication_mode")
                    .addRow("gdp-by-year", "MAIN_ECONOMIC_INDICATOR", "LINE", "year", "gdp", "country_id", "SUM", "DRAFT");
            textTable(db, "__gs_chart_filter", "chart_code", "field", "operator", "value")
                    .addRow("gdp-by-year", "country_id", "EQ", "1");

            Table data = new TableBuilder("main_economic_indicator")
                    .addColumn(new ColumnBuilder("id", DataType.LONG))
                    .addColumn(new ColumnBuilder("country_id", DataType.LONG))
                    .addColumn(new ColumnBuilder("year", DataType.LONG))
                    .addColumn(new ColumnBuilder("gdp", DataType.DOUBLE))
                    .addColumn(new ColumnBuilder("gdp_per_capita", DataType.DOUBLE))
                    .addColumn(new ColumnBuilder("inflation", DataType.DOUBLE))
                    .addColumn(new ColumnBuilder("population", DataType.DOUBLE))
                    .addColumn(new ColumnBuilder("group_id", DataType.LONG))
                    .addColumn(new ColumnBuilder("gdp_per_capita_ppp", DataType.DOUBLE))
                    .toTable(db);
            data.addRow(900001, 1, 2023, 30_500_000_000d, 8_100d, 2.5d, 3_700_000d, 1, 22_000d);
            data.addRow(900002, 1, 2024, 33_000_000_000d, 8_700d, 1.8d, 3_710_000d, 1, 23_100d);
            data.addRow(900003, 1, 2025, 35_500_000_000d, 9_300d, 2.1d, 3_720_000d, 1, 24_400d);
        }
        System.out.println("Created: " + output.getAbsolutePath());
    }

    private static Table textTable(Database db, String name, String... columns) throws Exception {
        TableBuilder builder = new TableBuilder(name);
        for (String column : columns) {
            builder.addColumn(new ColumnBuilder(column, DataType.TEXT));
        }
        return builder.toTable(db);
    }
}
