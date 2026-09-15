package org.base.api.service.catalog;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

/** Reads the declarative __gs_* metadata tables from a managed Access package. */
@Service
public class ManagedAccessPackageReader {

    public ManagedAccessPackage read(File accessFile) throws IOException {
        try (Database database = DatabaseBuilder.open(accessFile)) {
            Map<String, String> tableNames = database.getTableNames().stream()
                    .collect(java.util.stream.Collectors.toMap(name -> name.toLowerCase(Locale.ROOT), name -> name));
            require(tableNames, "__gs_package");
            require(tableNames, "__gs_dataset");
            require(tableNames, "__gs_chart");
            require(tableNames, "__gs_chart_filter");

            Row packageRow = rows(database.getTable(tableNames.get("__gs_package"))).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("__gs_package must contain one metadata row"));
            List<ManagedDatasetDefinition> datasets = rows(database.getTable(tableNames.get("__gs_dataset")))
                    .map(this::dataset).toList();
            List<ManagedChartDefinition> charts = rows(database.getTable(tableNames.get("__gs_chart")))
                    .map(this::chart).toList();
            List<ManagedChartFilter> filters = rows(database.getTable(tableNames.get("__gs_chart_filter")))
                    .map(this::filter).toList();

            return new ManagedAccessPackage(required(packageRow, "package_code"), value(packageRow, "package_version"),
                    datasets, charts, filters);
        }
    }

    private ManagedDatasetDefinition dataset(Row row) {
        return new ManagedDatasetDefinition(required(row, "dataset_code"), required(row, "access_table_name"),
                required(row, "profile_code"), required(row, "data_kind"), value(row, "row_key"),
                Boolean.parseBoolean(value(row, "required")));
    }

    private ManagedChartDefinition chart(Row row) {
        return new ManagedChartDefinition(required(row, "chart_code"), required(row, "dataset_code"),
                required(row, "chart_type"), value(row, "x_field"), value(row, "y_field"),
                value(row, "series_field"), value(row, "aggregation"), value(row, "publication_mode"));
    }

    private ManagedChartFilter filter(Row row) {
        return new ManagedChartFilter(required(row, "chart_code"), required(row, "field"),
                required(row, "operator"), value(row, "value"));
    }

    private java.util.stream.Stream<Row> rows(Table table) {
        return StreamSupport.stream(table.spliterator(), false);
    }

    private void require(Map<String, String> tableNames, String requiredName) {
        if (!tableNames.containsKey(requiredName)) {
            throw new IllegalArgumentException("Missing managed package table: " + requiredName);
        }
    }

    private String required(Row row, String field) {
        String value = value(row, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required metadata value: " + field);
        }
        return value;
    }

    private String value(Row row, String field) {
        Object value = row.get(field);
        return value == null ? null : String.valueOf(value).trim();
    }
}
