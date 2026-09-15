package org.base.api.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import java.nio.file.Files;
import java.util.*;

/** Contract-response to deterministic Avro/Parquet encoder. */
public final class ParquetExportCodec {
    private ParquetExportCodec() { }

    public static byte[] encode(Map<String,Object> response, ObjectMapper mapper) throws Exception {
        List<Map<String,Object>> rows = rows(response);
        SortedSet<String> names = new TreeSet<>();
        rows.forEach(row -> row.keySet().forEach(k -> names.add(normalize(k))));
        if (names.isEmpty()) names.add("payload_json");
        Schema schema = schema(names);
        java.nio.file.Path temp = java.nio.file.Paths.get("geostat-export-" + UUID.randomUUID() + ".parquet");
        try {
            Configuration conf = new Configuration(false);
            try (var writer = AvroParquetWriter.<GenericData.Record>builder(new Path(temp.toString()))
                    .withSchema(schema).withConf(conf).withCompressionCodec(CompressionCodecName.SNAPPY).build()) {
                for (Map<String,Object> row : rows) {
                    GenericData.Record record = new GenericData.Record(schema);
                    for (String name : names) record.put(name, scalar(row, name, mapper));
                    writer.write(record);
                }
            }
            return Files.readAllBytes(temp);
        } finally { Files.deleteIfExists(temp); }
    }

    public static Schema schema(Set<String> fields) {
        SchemaBuilder.FieldAssembler<Schema> assembler = SchemaBuilder.record("GeostatExport").namespace("org.geostat.export").fields();
        for (String field : fields) assembler.name(field).type().unionOf().nullType().and().stringType().endUnion().nullDefault();
        return assembler.endRecord();
    }

    private static List<Map<String,Object>> rows(Map<String,Object> response) {
        Object data = response.get("data"); List<Map<String,Object>> rows = new ArrayList<>();
        if (data instanceof Iterable<?> iterable) for (Object value : iterable) if (value instanceof Map<?,?> map) {
            Map<String,Object> row = new LinkedHashMap<>(); map.forEach((k,v)->row.put(String.valueOf(k),v)); rows.add(row);
        }
        if (rows.isEmpty()) rows.add(Map.of("payload_json", response));
        return rows;
    }
    private static String scalar(Map<String,Object> row, String field, ObjectMapper mapper) throws Exception {
        Object value = row.entrySet().stream().filter(e -> normalize(e.getKey()).equals(field)).map(Map.Entry::getValue).findFirst().orElse(null);
        return value == null ? null : (value instanceof String || value instanceof Number || value instanceof Boolean ? String.valueOf(value) : mapper.writeValueAsString(value));
    }
    private static String normalize(String key) { String value = key == null ? "field" : key.replaceAll("[^A-Za-z0-9_]", "_"); return value.matches("[A-Za-z_].*") ? value : "field_" + value; }
}
