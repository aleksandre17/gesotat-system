package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExportFormatRegistryTest {
    @Test void resolvesCanonicalFormatMetadata() {
        assertEquals("application/x-ndjson", ExportFormatRegistry.require("ndjson").mediaType());
        assertTrue(ExportFormatRegistry.require(null).code().equals("JSON"));
        assertEquals("csv", ExportFormatRegistry.require(" CSV ").extension());
    }
    @Test void unknownFormatFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> ExportFormatRegistry.require("PARQUET"));
        assertEquals(3, ExportFormatRegistry.supportedCodes().size());
    }
}
