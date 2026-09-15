package org.base.api.service.platform.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.StreamSupport;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the real canonical KIDS artifact, including actual transformed source and statistical cells. */
class KidsPortalCanonicalAccessPackageTest {
    @Test
    void canonicalKidsArtifactMatchesItsDeclaredContract() throws Exception {
        File artifact = locateArtifact("kids-portal-v1-canonical-r8-final.accdb");
        assertTrue(artifact.isFile(), "Generate it with :api:generateKidsCanonicalAccess");
        SemanticAccessPackage pack = new SemanticAccessPackageReader().read(artifact);
        SemanticAccessPreview preview = new SemanticAccessPackageValidationService(new ObjectMapper()).validate(artifact, pack);

        assertEquals("KIDS_PORTAL", pack.productCode());
        assertEquals("KIDS_PORTAL_V1", pack.contractCode());
        assertEquals(8, pack.contractRevision());
        assertTrue(preview.valid(), () -> "Invalid canonical package: " + preview.issues());
        assertEquals(15, preview.datasets());
        assertEquals(21, preview.relations());
        assertEquals(6, preview.projections());

        try (var db = new DatabaseBuilder(artifact).setReadOnly(true).open()) {
            assertEquals(122, count(db.getTable("__gs_field")), "every revision-8 field must be declared");
            assertEquals(java.util.List.of("source_row_key", "source_table", "source_primary_key", "extract_sequence", "source_system", "source_feed", "document_identity", "original_filename", "mime_type", "byte_size", "checksum", "received_at", "source_uri", "ingestion_batch", "provenance", "retention_policy", "confidentiality_class", "payload_reference", "encryption_reference", "parser_status", "validation_status", "supersedes_source_row_key"),
                    db.getTable("__raw_document").getColumns().stream().map(c -> c.getName().toLowerCase()).collect(Collectors.toList()),
                    "__raw_document is the immutable source-artifact envelope and must carry provenance and retention metadata");
            assertEquals(456, count(db.getTable("__raw_document")), "raw evidence must include every legacy source row");
            assertEquals(36, count(db.getTable("__ent_kids_goal")));
            assertEquals(225, count(db.getTable("__ent_kids_resource")));
            assertEquals(178, count(db.getTable("__ent_kids_glossary_entry")));
            assertTrue(count(db.getTable("__rel_kids_resource_subcategory_assignment")) > 0);
            assertEquals(43, count(db.getTable("__raw_kids_statistical_carrier")), "every actual parseable JSON array, including escaped arrays, becomes a carrier");
            assertEquals(43, count(db.getTable("__stat_metric")), "every carrier must have one governed metric");
            assertEquals(43, count(db.getTable("__rel_kids_statistical_semantic_binding")), "every carrier must have one semantic binding");
            assertEquals(10, count(db.getTable("__stat_unit")));
            assertEquals(java.util.List.of("carrier_code", "source_resource_id", "payload_checksum", "parse_status", "source_row_key", "operation"),
                    db.getTable("__raw_kids_statistical_carrier").getColumns().stream().map(c -> c.getName().toLowerCase()).collect(Collectors.toList()),
                    "carrier must contain lineage and checksum only; raw JSON duplication is forbidden");
            assertTrue(count(db.getTable("__stat_kids_statistical_input")) > 0, "carrier JSON must yield real lossless input cells");
            assertTrue(count(db.getTable("__cl_scheme")) >= 4);
            assertTrue(count(db.getTable("__cl_item")) >= 17, "source classifier values must travel with Access");
            assertEquals(21, db.getRelationships().size(), "declared canonical relations must also be enforced physically by Access");
            for (String table : java.util.List.of("__gs_package","__gs_page","__gs_dataset","__gs_field","__gs_key","__gs_relation","__gs_projection","__gs_metadata_schema","__gs_metadata","__cl_scheme","__cl_version","__cl_item","__cl_alias","__cl_hierarchy","__stat_unit","__stat_metric","__raw_document","__ent_kids_goal","__ent_kids_resource","__rel_kids_resource_subcategory_assignment","__ent_kids_glossary_entry","__raw_kids_statistical_carrier","__stat_kids_statistical_input","__rel_kids_statistical_semantic_binding")) {
                assertTrue(db.getTable(table).getPrimaryKeyIndex() != null, table + " must have a physical primary key");
            }
            var statisticalProjection = StreamSupport.stream(db.getTable("__gs_projection").spliterator(), false)
                    .filter(row -> "KIDS_STATS_INPUT".equals(String.valueOf(row.get("projection_code"))))
                    .findFirst().orElseThrow();
            var mapping = new ObjectMapper().readTree(String.valueOf(statisticalProjection.get("mapping_json")));
            assertEquals("SDMX_COMPATIBLE_LONG", mapping.path("projectionModel").asText());
            assertEquals(8, mapping.path("contractRevision").asInt());
            assertEquals("KIDS_STATISTICAL_SEMANTIC_BINDING", mapping.path("semanticBindingDataset").asText());
            assertEquals(3, mapping.path("dsd").path("components").size(), "TIME_PERIOD, AGE_GROUP and OBS_VALUE must be explicitly declared");
            assertEquals(5, mapping.path("dsd").path("attributesFromBinding").size());
            assertEquals("NONE", mapping.path("aggregationDefault").asText());
        }
    }

    private static long count(Iterable<?> rows) { return StreamSupport.stream(rows.spliterator(), false).count(); }

    private static File locateArtifact(String name) {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            for (Path candidate : new Path[]{directory.resolve(name), directory.resolve("samples").resolve(name), directory.resolve("api").resolve(name)}) {
                if (candidate.toFile().isFile()) return candidate.toFile();
            }
            directory = directory.getParent();
        }
        return Path.of("samples", name).toFile();
    }
}
