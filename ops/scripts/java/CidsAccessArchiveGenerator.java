import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Creates a lossless Access archive from the Children's Portal chartjson.dat source. */
public final class CidsAccessArchiveGenerator {
    private CidsAccessArchiveGenerator() { }

    public static void main(String[] args) throws Exception {
        File source = new File(args.length > 0 ? args[0] : "samples/chartjson.dat");
        File output = new File(args.length > 1 ? args[1] : "samples/cids-children-portal-full-data.accdb");
        if (!source.isFile()) throw new IllegalArgumentException("Source file not found: " + source.getAbsolutePath());
        if (output.exists()) throw new IllegalStateException("Refusing to overwrite existing file: " + output.getAbsolutePath());
        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Cannot create: " + parent);

        String raw = Files.readString(source.toPath(), StandardCharsets.UTF_8).replace("\\n", "\n");
        List<String> datasets = extractArrays(raw);
        // The supplied historical source has a few trailing commas; preserve all valid values.
        ObjectMapper json = new ObjectMapper().enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());
        try (Database db = DatabaseBuilder.create(Database.FileFormat.V2010, output)) {
            Table metadata = table(db, "cids_export_metadata",
                    column("source_file", DataType.TEXT), column("created_utc", DataType.TEXT),
                    column("dataset_count", DataType.LONG), column("format", DataType.TEXT));
            metadata.addRow(source.getName(), Instant.now().toString(), datasets.size(), "CIDS_CHILDREN_PORTAL_ARCHIVE_V1");

            Table catalog = table(db, "cids_chart_catalog",
                    column("chart_id", DataType.LONG), column("source_array_no", DataType.LONG),
                    column("row_count", DataType.LONG), column("measures_json", DataType.MEMO));
            Table values = table(db, "cids_chart_values",
                    column("chart_id", DataType.LONG), column("observation_no", DataType.LONG),
                    column("year", DataType.TEXT), column("measure_code", DataType.TEXT),
                    column("numeric_value", DataType.DOUBLE), column("value_text", DataType.MEMO));
            Table payloads = table(db, "cids_chart_payload",
                    column("chart_id", DataType.LONG), column("payload_json", DataType.MEMO));

            for (int index = 0; index < datasets.size(); index++) {
                int chartId = index + 1;
                JsonNode rows = json.readTree(datasets.get(index));
                if (!rows.isArray()) throw new IllegalStateException("Dataset is not an array: " + chartId);
                LinkedHashSet<String> measures = new LinkedHashSet<>();
                for (JsonNode row : rows) row.fieldNames().forEachRemaining(field -> { if (!"year".equals(field)) measures.add(field); });
                catalog.addRow(chartId, chartId, rows.size(), json.writeValueAsString(measures));
                payloads.addRow(chartId, json.writeValueAsString(rows));
                int observation = 0;
                for (JsonNode row : rows) {
                    observation++;
                    String year = row.path("year").isMissingNode() || row.path("year").isNull() ? null : row.path("year").asText();
                    Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        if ("year".equals(entry.getKey())) continue;
                        JsonNode value = entry.getValue();
                        values.addRow(chartId, observation, year, entry.getKey(),
                                value.isNumber() ? value.asDouble() : null,
                                value.isNull() ? null : value.asText());
                    }
                }
            }
        }
        System.out.println("Created archive: " + output.getAbsolutePath() + " (datasets=" + datasets.size() + ")");
    }

    private static ColumnBuilder column(String name, DataType type) { return new ColumnBuilder(name, type); }

    private static Table table(Database db, String name, ColumnBuilder... columns) throws Exception {
        TableBuilder builder = new TableBuilder(name);
        for (ColumnBuilder column : columns) builder.addColumn(column);
        return builder.toTable(db);
    }

    /** Extracts consecutive JSON arrays without assuming they are wrapped in one root JSON document. */
    private static List<String> extractArrays(String source) {
        List<String> arrays = new ArrayList<>();
        int depth = 0, start = -1;
        boolean inString = false, escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char character = source.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') inString = false;
                continue;
            }
            if (character == '"') inString = true;
            else if (character == '[') { if (depth++ == 0) start = i; }
            else if (character == ']' && --depth == 0 && start >= 0) {
                arrays.add(source.substring(start, i + 1));
                start = -1;
            }
        }
        if (depth != 0 || arrays.isEmpty()) throw new IllegalArgumentException("Invalid concatenated JSON-array source");
        return arrays;
    }
}
