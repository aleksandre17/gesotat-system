package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactManifestGeneratorTest {
    private static final String A = "a".repeat(64);
    private static final String B = "b".repeat(64);
    private static final String PREFIX = "pkg/r1/";

    private static List<ArtifactManifestGenerator.InventoryEntry> inventory() {
        return List.of(new ArtifactManifestGenerator.InventoryEntry("mainstat/x/Report 1.xlsx", A, 10),
                new ArtifactManifestGenerator.InventoryEntry("mainstat/x/report-2.xls", B, 20));
    }

    @Test
    void manifestIsIndependentOfInventoryOrder() {
        List<ArtifactManifestGenerator.InventoryEntry> shuffled = new ArrayList<>(inventory());
        for (int seed = 0; seed < 20; seed++) {
            Collections.shuffle(shuffled, new Random(seed));
            assertEquals(ArtifactManifestGenerator.generate("P", "geostat-ingest", PREFIX, "src", inventory()),
                    ArtifactManifestGenerator.generate("P", "geostat-ingest", PREFIX, "src", shuffled));
        }
    }

    @Test
    void objectKeysAreRecomputedFromChecksumNotFromSourceNames() {
        ArtifactManifest manifest = ArtifactManifestGenerator.generate("P", "geostat-ingest", PREFIX, "src", inventory());
        assertEquals(PREFIX + A + ".xlsx", manifest.entries().get(0).objectKey());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", manifest.entries().get(0).mediaType());
        assertEquals("application/vnd.ms-excel", manifest.entries().get(1).mediaType());
        assertEquals("Report 1.xlsx", manifest.entries().get(0).originalName());
    }

    @Test
    void checksumChangesWhenContentChanges() {
        ArtifactManifest original = ArtifactManifestGenerator.generate("P", "geostat-ingest", PREFIX, "src", inventory());
        ArtifactManifest changed = ArtifactManifestGenerator.generate("P", "geostat-ingest", PREFIX, "src",
                List.of(inventory().get(0), new ArtifactManifestGenerator.InventoryEntry("mainstat/x/report-2.xls", A, 20)));
        assertNotEquals(original.packageChecksum(), changed.packageChecksum());
    }

    @Test
    void pathsAreNfcNormalizedSoEquivalentSpellingsCollide() {
        String decomposed = "mainstat/café.xlsx";
        String composed = "mainstat/café.xlsx";
        assertEquals(composed, ArtifactManifestGenerator.normalizePath(decomposed));
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.generate("P", "geostat-ingest", PREFIX, "src",
                List.of(new ArtifactManifestGenerator.InventoryEntry(decomposed, A, 1), new ArtifactManifestGenerator.InventoryEntry(composed, B, 1))));
    }

    @Test
    void packageChecksumUsesUnambiguousLengthPrefixedFields() {
        String prefix = "pkg/r1/";
        String media = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String firstKey = prefix + A + ".xlsx";
        String embeddedPath = "mainstat/x.xlsx\t" + A + "\t10\t" + media + "\tgeostat-ingest\t" + firstKey + "\nmainstat/y.xlsx";
        ArtifactManifest ambiguous = ArtifactManifestGenerator.generate("P", "geostat-ingest", prefix, "src",
                List.of(new ArtifactManifestGenerator.InventoryEntry(embeddedPath, B, 20)));
        ArtifactManifest separate = ArtifactManifestGenerator.generate("P", "geostat-ingest", prefix, "src", List.of(
                new ArtifactManifestGenerator.InventoryEntry("mainstat/x.xlsx", A, 10),
                new ArtifactManifestGenerator.InventoryEntry("mainstat/y.xlsx", B, 20)));
        assertNotEquals(ambiguous.packageChecksum(), separate.packageChecksum());
    }

    @Test
    void invalidInventoryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.generate("P", "b", PREFIX, "s", List.of()));
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.generate("P", "b", PREFIX, "s",
                List.of(new ArtifactManifestGenerator.InventoryEntry("../escape.xlsx", A, 1))));
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.normalizePath("/absolute/report.xlsx"));
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.normalizePath("C:\\private\\report.xlsx"));
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.generate("P", "b", PREFIX, "s",
                List.of(new ArtifactManifestGenerator.InventoryEntry("x.xlsx", "not-a-sha", 1))));
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.generate("P", "b", PREFIX, "s",
                List.of(new ArtifactManifestGenerator.InventoryEntry("x.xlsx", A, -1))));
        assertThrows(IllegalArgumentException.class, () -> ArtifactManifestGenerator.generate("bad code", "b", PREFIX, "s", inventory()));
    }
}
