package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conformance of the real KIDS R8 package (719 files / 532 objects) and the 225 KIDS_RESOURCE rows
 * against the match rule exactly as seeded by migration 088. No KIDS code path exists in the engine.
 */
class KidsR8ArtifactBindingConformanceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path SEED = Path.of("core", "src", "main", "resources", "db", "platform", "088_kids_r8_resource_artifact_binding.sql");
    private static final String PREFIX = "kids/r8/resources/kids-files-r8-sanitized/";

    private static Path backendRoot() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent())
            if (Files.isRegularFile(dir.resolve(SEED))) return dir;
        throw new IllegalStateException("backend root not found");
    }

    private static JsonNode resource(String name) throws Exception {
        try (InputStream in = KidsR8ArtifactBindingConformanceTest.class.getResourceAsStream("/artifact/" + name)) {
            return JSON.readTree(in);
        }
    }

    private static String seededRule() throws Exception {
        Matcher m = Pattern.compile("N'(\\{\"type\":\"SOURCE_PATH\".*?\\})'").matcher(Files.readString(backendRoot().resolve(SEED)));
        assertTrue(m.find(), "088 must seed a SOURCE_PATH rule");
        return m.group(1);
    }

    @Test
    void everyResourceRowBindsExactlyToOneVerifiedPackageEntryPerLanguage() throws Exception {
        List<ArtifactManifestGenerator.InventoryEntry> inventory = new ArrayList<>();
        for (JsonNode n : resource("kids-r8-inventory.json"))
            inventory.add(new ArtifactManifestGenerator.InventoryEntry(n.get("originalPath").asText(), n.get("sha256").asText(), n.get("bytes").asLong()));
        ArtifactManifest manifest = ArtifactManifestGenerator.generate("KIDS_R8_RESOURCES", "geostat-ingest", PREFIX, "fixture", inventory);
        assertEquals(719, manifest.entries().size());
        assertEquals(532, manifest.entries().stream().map(ArtifactManifest.Entry::sha256).distinct().count());
        assertTrue(manifest.entries().stream().allMatch(e -> e.objectKey().startsWith(PREFIX)), "keys follow the physical prefix, not the inventory's objectName");

        List<ArtifactMatcher.SourceRow> rows = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        long id = 1;
        for (JsonNode n : resource("kids-r8-resource-rows.json")) {
            assertTrue(keys.add(n.get("source_resource_id").asText()), "stable key must be unique");
            rows.add(new ArtifactMatcher.SourceRow(id, n.get("source_resource_id").asText(), 1000 + id, n));
            id++;
        }
        assertEquals(225, rows.size());

        ArtifactPolicy policy = new ArtifactPolicy("KIDS_PUBLIC_STATISTICAL_FILE", 1, ArtifactPolicy.AccessMode.PUBLIC_WHEN_PUBLISHED, null,
                Set.of(ArtifactMatcherTest.XLSX, "application/vnd.ms-excel"), 52428800, Duration.ofSeconds(300), RetentionClass.RETAIN_INDEFINITE);
        ArtifactRelationDefinition definition = new ArtifactRelationDefinition(1, "PRIMARY_FILE", ArtifactRole.PRIMARY, policy, 1, 1, false,
                ArtifactMatcherTest.RULES.parse(JSON.readTree(seededRule())));

        ArtifactMatcher.Plan plan = ArtifactMatcher.match(definition, rows, manifest.entries());
        assertFalse(plan.blocked(), () -> "unexpected findings: " + plan.issues());
        assertEquals(450, plan.edges().size());
        assertEquals(450, plan.edges().stream().map(e -> e.entry().sha256()).distinct().count());
        assertEquals(115, plan.orphanCount(), "unreferenced mainstat files are reported, not bound");
        assertEquals(1, plan.issues().size());
        assertEquals(ArtifactIssue.Code.ORPHAN_ARTIFACT, plan.issues().get(0).code());
    }
}
