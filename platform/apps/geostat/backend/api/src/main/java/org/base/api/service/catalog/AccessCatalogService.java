package org.base.api.service.catalog;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Discovers every table in an Access file before any write occurs. Managed
 * packages are recognised only when all mandatory {@code __gs_*} tables exist.
 */
@Service
public class AccessCatalogService {

    static final Set<String> REQUIRED_MANAGED_METADATA = Set.of(
            "__gs_package", "__gs_dataset", "__gs_chart", "__gs_chart_filter"
    );

    public AccessCatalog catalog(File accessFile) throws IOException {
        try (Database database = DatabaseBuilder.open(accessFile)) {
            List<AccessTableCatalog> tables = database.getTableNames().stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(tableName -> describeTable(database, tableName))
                    .toList();

            Set<String> names = tables.stream()
                    .map(AccessTableCatalog::name)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toSet());
            List<String> missing = REQUIRED_MANAGED_METADATA.stream()
                    .filter(name -> !names.contains(name))
                    .sorted(Comparator.naturalOrder())
                    .toList();
            return new AccessCatalog(missing.isEmpty(), missing, tables);
        }
    }

    static boolean isManagedPackage(Set<String> tableNames) {
        Set<String> normalized = tableNames.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        return normalized.containsAll(REQUIRED_MANAGED_METADATA);
    }

    private AccessTableCatalog describeTable(Database database, String tableName) {
        try {
            Table table = database.getTable(tableName);
            List<AccessColumnCatalog> columns = table.getColumns().stream()
                    .map(this::describeColumn)
                    .toList();
            return new AccessTableCatalog(tableName, table.getRowCount(), columns,
                    tableName.regionMatches(true, 0, "MSys", 0, 4));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Access table metadata: " + tableName, e);
        }
    }

    private AccessColumnCatalog describeColumn(Column column) {
        try {
            Object required = column.getProperties().getValue("Required");
            return new AccessColumnCatalog(column.getName(), column.getType().name(), Boolean.TRUE.equals(required));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Access column metadata: " + column.getName(), e);
        }
    }
}
