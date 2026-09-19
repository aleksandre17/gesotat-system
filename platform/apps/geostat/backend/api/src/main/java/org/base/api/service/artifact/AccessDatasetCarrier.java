package org.base.api.service.artifact;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Microsoft Access as a dataset carrier. */
@Component
public class AccessDatasetCarrier implements PackageDatasetCarrier, PackageTableReader {
    private static final Set<String> EXTENSIONS = Set.of("accdb", "mdb");

    private final ArtifactAccessPackageValidator validator;

    public AccessDatasetCarrier(ArtifactAccessPackageValidator validator) {
        this.validator = validator;
    }

    @Override
    public boolean carries(String extension) {
        return EXTENSIONS.contains(extension);
    }

    @Override
    public void validate(File file, ArtifactPackageContractResolver.DatasetContract contract) throws IOException {
        validator.validate(file, contract);
    }

    @Override
    public List<Map<String, String>> rows(File file, String table, List<String> fields) throws IOException {
        try (Database database = new DatabaseBuilder(file).setReadOnly(true).open()) {
            String name = database.getTableNames().stream().filter(candidate -> candidate.equalsIgnoreCase(table)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Package is missing its contract-declared table " + table));
            Table source = database.getTable(name);
            Map<String, String> columns = new HashMap<>();
            for (Column column : source.getColumns()) columns.put(column.getName().toLowerCase(Locale.ROOT), column.getName());
            for (String field : fields)
                if (!columns.containsKey(field.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Table " + table + " is missing contract-declared field " + field);
            List<Map<String, String>> rows = new ArrayList<>(source.getRowCount());
            for (Row row : source) {
                Map<String, String> values = new HashMap<>();
                for (String field : fields) {
                    Object value = row.get(columns.get(field.toLowerCase(Locale.ROOT)));
                    values.put(field, value == null ? null : value.toString());
                }
                rows.add(values);
            }
            return rows;
        }
    }

    @Override
    public List<ArtifactMatcher.SourceRow> rows(File file, ArtifactPackageContractResolver.DatasetContract contract) throws IOException {
        return ArtifactAccessRowReader.read(file, contract);
    }
}
