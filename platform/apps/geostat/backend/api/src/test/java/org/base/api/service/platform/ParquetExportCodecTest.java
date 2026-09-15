package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ParquetExportCodecTest {
    @Test void writesDeterministicSchemaAndReadsRoundTrip() throws Exception {
        // Hadoop's LocalFileSystem rejects Windows drive-letter URIs; this
        // conformance test executes in the Linux CI/container profile.
        assumeFalse(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"));
        var mapper = new ObjectMapper();
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("data", List.of(Map.of("id", 7, "label", "თბილისი", "tags", List.of("a", "b"))));
        byte[] bytes = ParquetExportCodec.encode(response, mapper);
        assertTrue(bytes.length > 4);
        var file = java.nio.file.Paths.get("parquet-conformance-" + UUID.randomUUID() + ".parquet"); Files.write(file, bytes);
        try (var reader = AvroParquetReader.<GenericRecord>builder(new Path(file.getFileName().toString())).build()) {
            GenericRecord row = reader.read();
            assertNotNull(row); assertEquals("7", row.get("id").toString());
            assertEquals("თბილისი", row.get("label").toString());
            assertTrue(row.get("tags").toString().contains("a"));
            assertNull(reader.read());
        } finally { Files.deleteIfExists(file); }
    }
}
