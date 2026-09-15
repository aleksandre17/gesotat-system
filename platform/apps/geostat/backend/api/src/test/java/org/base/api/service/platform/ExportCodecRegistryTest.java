package org.base.api.service.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class ExportCodecRegistryTest {
    @Test void encodesApprovedFormatsAndZipRoundTrip() throws Exception {
        var registry = new ExportCodecRegistry(new ObjectMapper());
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("data", List.of(Map.of("id", 1, "label", "A")));
        assertTrue(registry.supportedCodes().containsAll(Set.of("JSON", "CSV", "NDJSON", "ZIP_JSON", "SDMX_JSON", "SDMX_XML")));
        assertTrue(registry.require("JSON").encode(response, new ObjectMapper()).length > 0);
        assertTrue(new String(registry.require("CSV").encode(response, new ObjectMapper())).contains("id"));
        try (var zip = new ZipInputStream(new ByteArrayInputStream(registry.require("ZIP_JSON").encode(response, new ObjectMapper())))) {
            assertEquals("response.json", zip.getNextEntry().getName());
        }
        String sdmxJson = new String(registry.require("SDMX_JSON").encode(response, new ObjectMapper()));
        assertTrue(sdmxJson.contains("\"header\"") && sdmxJson.contains("\"structure\""));
        String sdmxXml = new String(registry.require("SDMX_XML").encode(response, new ObjectMapper()));
        assertTrue(sdmxXml.startsWith("<?xml") && sdmxXml.contains("GenericData"));
        assertThrows(IllegalArgumentException.class, () -> registry.require("PARQUET"));
    }
}
