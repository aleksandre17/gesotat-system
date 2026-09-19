package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Reads the contract-declared Access dataset table of a package into matcher rows. The row identity is
 * the contract's ordered key fields joined by {@code |}, i.e. the same external key the materialized
 * entity carries. A dataset without declared keys, or a blank/duplicate key, fails closed (no guessed identity).
 */
public final class ArtifactAccessRowReader {
    private static final ObjectMapper JSON = new ObjectMapper();

    private ArtifactAccessRowReader() {}

    public static List<ArtifactMatcher.SourceRow> read(File accessFile, ArtifactPackageContractResolver.DatasetContract contract) throws IOException {
        if (contract.keyFields().isEmpty())
            throw new IllegalArgumentException("Dataset " + contract.datasetCode() + " declares no key fields; row identity cannot be established");
        try (var database = new DatabaseBuilder(accessFile).setReadOnly(true).open()) {
            String tableName = database.getTableNames().stream().filter(n -> n.equalsIgnoreCase(contract.accessTableName())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Access package is missing its contract-declared dataset table"));
            Table table = database.getTable(tableName);
            List<ArtifactMatcher.SourceRow> rows = new ArrayList<>(table.getRowCount());
            Set<String> keys = new HashSet<>();
            long ordinal = 0;
            for (Row row : table) {
                ordinal++;
                ObjectNode payload = JSON.createObjectNode();
                for (Column column : table.getColumns()) {
                    Object value = row.get(column.getName());
                    if (value == null) payload.putNull(column.getName());
                    else payload.put(column.getName(), value.toString());
                }
                String key = key(payload, contract.keyFields(), ordinal);
                if (!keys.add(key)) throw new IllegalArgumentException("Duplicate row key " + key + " in " + tableName);
                rows.add(new ArtifactMatcher.SourceRow(ordinal, key, ordinal, payload));
            }
            return List.copyOf(rows);
        }
    }

    private static String key(ObjectNode payload, List<String> keyFields, long ordinal) {
        StringJoiner key = new StringJoiner("|");
        for (String field : keyFields) {
            var node = payload.get(field);
            if (node == null) node = payload.properties().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(field)).map(java.util.Map.Entry::getValue).findFirst().orElse(null);
            if (node == null || node.isNull() || node.asText().isBlank())
                throw new IllegalArgumentException("Row " + ordinal + " has no value for key field " + field);
            key.add(node.asText());
        }
        return key.toString();
    }
}
