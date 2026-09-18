package org.base.api.service.artifact;

import com.healthmarketscience.jackcess.DatabaseBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Verifies an Access dataset against field and table names declared by an approved contract. */
@Component
public final class ArtifactAccessPackageValidator {
    public void validate(File file, ArtifactPackageContractResolver.DatasetContract contract) throws IOException {
        try (var database = new DatabaseBuilder(file).setReadOnly(true).open()) {
            var tableNames = database.getTableNames().stream().collect(Collectors.toSet());
            String table = tableNames.stream().filter(n -> n.equalsIgnoreCase(contract.accessTableName())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Access package is missing its contract-declared dataset table"));
            Set<String> columns = database.getTable(table).getColumns().stream()
                    .map(c -> c.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            var missing = contract.fields().stream().filter(f -> !columns.contains(f.toLowerCase(Locale.ROOT))).toList();
            if (!missing.isEmpty()) throw new IllegalArgumentException("Access dataset is missing contract-declared fields: " + String.join(",", missing));
        }
    }
}
