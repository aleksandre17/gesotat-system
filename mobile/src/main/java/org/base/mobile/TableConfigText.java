package org.base.mobile;

import lombok.Getter;
import org.base.mobile.TableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TableConfigText {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableConfig.class);

    public static final String DEFAULT_TABLE_NAME = "[dbo].[auto_main]";
    public static final String DEFAULT_VEHICLES_TABLE_NAME = "[dbo].[vehicles1000]";
    public static final String DEFAULT_FUEL_TABLE_NAME = "[CL].[cl_fuel]";
    public static final String DEFAULT_ROAD_TABLE_NAME = "[dbo].[road_length]";
    public static final String DEFAULT_ACCIDENTS_TABLE_NAME = "[CL].[cl_accidents]";
    public static final String DEFAULT_LICENSES_TABLE_NAME = "[dbo].[licenses]";
    public static final int DEFAULT_YEAR = 2026;

    public static final Map<String, TableMetadata> TABLES = new HashMap<>();

    static {
        // From TableConfigText
        TABLES.put("[dbo].[auto_main]", new TableMetadata(
                "[dbo].[auto_main]",
                new HashMap<>(Map.of(
                        "year", new ColumnMetadata("year", "year", false, false),
                        "quarter", new ColumnMetadata("quarter", "quarter", true, false)
                )),
                "year"
        ));
        TABLES.put("[dbo].[eoyes]", new TableMetadata(
                "[dbo].[eoyes]",
                new HashMap<>(Map.of(
                        "year", new ColumnMetadata("year", "year", false, false)
                )),
                "year"
        ));
        TABLES.put("[dbo].[vehicles1000]", new TableMetadata(
                "[dbo].[vehicles1000]",
                new HashMap<>(Map.of(
                        "year", new ColumnMetadata("year", "year", false, false)
                )),
                "year"
        ));

        // From TableConfig
        TABLES.put("[CL].[cl_fuel]", new TableMetadata(
                "[CL].[cl_fuel]",
                new HashMap<>(Map.of(
                        "id", new ColumnMetadata("id", "id", false, false),
                        "en", new ColumnMetadata("en", "name", false, true),
                        "ka", new ColumnMetadata("ka", "name", false, true)
                )),
                "id"
        ));
        TABLES.put("[dbo].[road_length]", new TableMetadata(
                "[dbo].[road_length]",
                new HashMap<>(Map.of(
                        "year", new ColumnMetadata("year", "year", false, false)
                )),
                "year"
        ));
        TABLES.put("[CL].[cl_region]", new TableMetadata(
                "[CL].[cl_region]",
                new HashMap<>(Map.of(
                        "id", new ColumnMetadata("id", "id", false, false),
                        "en", new ColumnMetadata("en", "name", false, true),
                        "ka", new ColumnMetadata("ka", "name", false, true)
                )),
                "id"
        ));
        TABLES.put("[CL].[cl_accidents]", new TableMetadata(
                "[CL].[cl_accidents]",
                new HashMap<>(Map.of(
                        "id", new ColumnMetadata("id", "id", false, false),
                        "en", new ColumnMetadata("en", "name", false, true),
                        "ka", new ColumnMetadata("ka", "name", false, true)
                )),
                "id"
        ));
        TABLES.put("[dbo].[licenses]", new TableMetadata(
                "[dbo].[licenses]",
                new HashMap<>(Map.of(
                        "year", new ColumnMetadata("year", "year", false, false)
                )),
                "year"
        ));
    }

    public record TableMetadata(String tableName, Map<String, ColumnMetadata> columns, String maxYearColumn) {
        public Map<String, ColumnMetadata> columns() {
            return new HashMap<>(columns); // Defensive copy
        }
    }

    public record ColumnMetadata(String column, String translationKey, boolean defaultAll, boolean isLanguageColumn) { }

    public static TableMetadata getTableMetadata(String tableName) {
        TableMetadata metadata = TABLES.get(tableName);
        if (metadata == null) {
            LOGGER.warn("Unknown table: {}, falling back to default: {}", tableName, DEFAULT_TABLE_NAME);
            metadata = TABLES.getOrDefault(DEFAULT_TABLE_NAME, null);
        }
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found for table: " + tableName);
        }
        return metadata;
    }

    public static boolean isValidColumn(String tableName, String column) {
        TableMetadata metadata = getTableMetadata(tableName);
        boolean valid = metadata.columns().containsKey(column);
        if (!valid) {
            LOGGER.warn("Invalid column: {} for table: {}", column, tableName);
        }
        return valid;
    }

    public static String getLanguageColumn(String tableName, String lang) {
        TableMetadata metadata = getTableMetadata(tableName);
        String column = "ka".equalsIgnoreCase(lang) ? "ka" : "en";
        if (!metadata.columns().containsKey(column) || !metadata.columns().get(column).isLanguageColumn()) {
            throw new IllegalArgumentException("Invalid language column: " + column + " for table: " + tableName);
        }
        return "name_"+column;
    }
}
