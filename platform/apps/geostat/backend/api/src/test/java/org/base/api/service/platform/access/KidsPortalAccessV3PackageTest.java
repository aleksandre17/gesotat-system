package org.base.api.service.platform.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Guards the real KIDS pilot package, not merely the generator's Java compilation. */
class KidsPortalAccessV3PackageTest {
    @Test
    void fullDataPackageIsAValidV3SemanticPackage() throws Exception {
        File artifact = Path.of("../../../../samples", "kids-portal-v1-access-v3-contract-r5-full-data.accdb").toFile();
        assertTrue(artifact.isFile(), "The complete KIDS Access v3 sample must be present");
        SemanticAccessPackage pack = new SemanticAccessPackageReader().read(artifact);
        SemanticAccessPreview preview = new SemanticAccessPackageValidationService(new ObjectMapper()).validate(artifact, pack);
        assertEquals("KIDS_PORTAL", pack.productCode());
        assertEquals("KIDS_PORTAL_V1", pack.contractCode());
        assertEquals(2, pack.contractRevision());
        assertTrue(preview.valid(), () -> "Invalid package: " + preview.issues());
        assertEquals(4, preview.datasets());
        assertEquals(21, preview.fields());
        assertEquals(2, preview.relations());
        assertEquals(5, preview.projections());
        assertEquals(32, pack.statisticalBindings().size());
        try (var database = DatabaseBuilder.open(artifact)) {
            long draftStatisticalBindings = 0;
            Map<String, String> chartDataByFileId = new HashMap<>();
            for (var file : database.getTable("files")) chartDataByFileId.put(String.valueOf(file.get("ID")), String.valueOf(file.get("chartdata")));
            ObjectMapper mapper = new ObjectMapper();
            for (var binding : database.getTable("__gs_statistical_binding")) {
                assertEquals("KIDS_FILE_RESOURCE", String.valueOf(binding.get("source_dataset_code")));
                assertEquals("KIDS_STATS", String.valueOf(binding.get("projection_code")));
                assertEquals("KIDS_FILES_STATISTICS", String.valueOf(binding.get("dataflow_code")));
                assertEquals("DRAFT", String.valueOf(binding.get("binding_state")));
                String sourceId = String.valueOf(binding.get("source_external_key"));
                String payload = chartDataByFileId.get(sourceId);
                try { assertTrue(payload != null && decodeEmbeddedArray(mapper, payload).isArray(), "A binding must refer to a real JSON array carrier: " + sourceId); }
                catch (Exception error) { fail("A binding has invalid JSON evidence for files.ID=" + sourceId, error); }
                draftStatisticalBindings++;
            }
            assertEquals(32, draftStatisticalBindings, "Every observed JSON chart carrier must be explicit evidence, not an implied chart");
        }
    }

    private static com.fasterxml.jackson.databind.JsonNode decodeEmbeddedArray(ObjectMapper mapper, String raw) throws Exception {
        try { return mapper.readTree(raw); }
        catch (Exception directFailure) { return mapper.readTree(mapper.readValue("\"" + raw.replace("\"", "\\\"") + "\"", String.class)); }
    }
}
