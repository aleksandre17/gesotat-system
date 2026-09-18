package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactMatcherTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    static final ArtifactMatchRules RULES = new ArtifactMatchRules(List.of(new SourcePathMatchRuleParser()));
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    static ArtifactPolicy policy() {
        return new ArtifactPolicy("P", 1, ArtifactPolicy.AccessMode.PUBLIC_WHEN_PUBLISHED, null, Set.of(XLSX), 1000, Duration.ofSeconds(300), RetentionClass.RETAIN_INDEFINITE);
    }

    static ArtifactRelationDefinition definition(String ruleJson) throws Exception {
        return new ArtifactRelationDefinition(1, "PRIMARY_FILE", ArtifactRole.PRIMARY, policy(), 1, 1, false, RULES.parse(JSON.readTree(ruleJson)));
    }

    private static final String RULE = "{\"type\":\"SOURCE_PATH\",\"normalization\":\"NFC\",\"stripPrefix\":\"files/\",\"packageRoot\":\"mainstat/\"," +
            "\"bindings\":[{\"language\":\"ka\",\"field\":\"path_ka\",\"required\":true},{\"language\":\"en\",\"field\":\"path_en\",\"required\":false}]}";

    private static ArtifactManifest.Entry entry(String path, String sha, long bytes, String media) {
        return new ArtifactManifest.Entry(path, sha, bytes, media, "geostat-ingest", "p/" + sha + ".xlsx");
    }

    private static ArtifactMatcher.SourceRow row(long id, String ka, String en) {
        ObjectNode payload = JSON.createObjectNode();
        if (ka != null) payload.put("path_ka", ka);
        if (en != null) payload.put("path_en", en);
        return new ArtifactMatcher.SourceRow(id, "r|" + id, 100 + id, payload);
    }

    private final List<ArtifactManifest.Entry> entries = List.of(
            entry("mainstat/a/ფაილი.xlsx", "1".repeat(64), 10, XLSX),
            entry("mainstat/a/File.xlsx", "2".repeat(64), 10, XLSX),
            entry("mainstat/a/unused.xlsx", "3".repeat(64), 10, XLSX),
            entry("goals/other.xlsx", "4".repeat(64), 10, XLSX));

    @Test
    void exactPathsBindPerLanguageSlot() throws Exception {
        ArtifactMatcher.Plan plan = ArtifactMatcher.match(definition(RULE), List.of(row(1, "files/a/ფაილი.xlsx", "files/a/File.xlsx")), entries);
        assertFalse(plan.blocked());
        assertEquals(2, plan.edges().size());
        assertEquals(Set.of("ka", "en"), Set.copyOf(plan.edges().stream().map(ArtifactMatcher.PlannedEdge::language).toList()));
        assertEquals(1, plan.orphanCount(), "only entries under packageRoot count as orphans");
    }

    @Test
    void caseOnlyMatchIsNeverAcceptedSilently() throws Exception {
        ArtifactMatcher.Plan plan = ArtifactMatcher.match(definition(RULE), List.of(row(1, "files/a/ფაილი.xlsx", "files/a/file.xlsx")), entries);
        assertTrue(plan.blocked());
        assertEquals(ArtifactIssue.Code.CASE_MISMATCH, plan.issues().get(0).code());
    }

    @Test
    void unmatchedAndOutOfPrefixValuesBlock() throws Exception {
        ArtifactMatcher.Plan missing = ArtifactMatcher.match(definition(RULE), List.of(row(1, "files/a/none.xlsx", null)), entries);
        assertTrue(missing.blocked());
        assertEquals(ArtifactIssue.Code.UNMATCHED_ROW, missing.issues().get(0).code());
        ArtifactMatcher.Plan outside = ArtifactMatcher.match(definition(RULE), List.of(row(1, "static/a/ფაილი.xlsx", null)), entries);
        assertEquals(ArtifactIssue.Code.UNMATCHED_ROW, outside.issues().get(0).code());
        ArtifactMatcher.Plan traversal = ArtifactMatcher.match(definition(RULE), List.of(row(1, "files/../mainstat/a/ფაილი.xlsx", null)), entries);
        assertTrue(traversal.blocked());
    }

    @Test
    void requiredEmptyValueBlocksButOptionalEmptyValueDoesNot() throws Exception {
        ArtifactMatcher.Plan required = ArtifactMatcher.match(definition(RULE), List.of(row(1, null, "files/a/File.xlsx")), entries);
        assertEquals(ArtifactIssue.Code.MISSING_SOURCE_VALUE, required.issues().get(0).code());
        ArtifactMatcher.Plan optional = ArtifactMatcher.match(definition(RULE), List.of(row(1, "files/a/ფაილი.xlsx", null)), entries);
        assertFalse(optional.blocked());
        assertEquals(1, optional.edges().size());
    }

    @Test
    void policyViolationsBlock() throws Exception {
        List<ArtifactManifest.Entry> bad = List.of(entry("mainstat/a/big.xlsx", "5".repeat(64), 5000, XLSX), entry("mainstat/a/doc.pdf", "6".repeat(64), 1, "application/pdf"));
        ArtifactMatcher.Plan plan = ArtifactMatcher.match(definition(RULE), List.of(row(1, "files/a/big.xlsx", "files/a/doc.pdf")), bad);
        assertEquals(Set.of(ArtifactIssue.Code.POLICY_MAX_BYTES, ArtifactIssue.Code.POLICY_MEDIA_TYPE),
                Set.copyOf(plan.issues().stream().map(ArtifactIssue::code).toList()));
    }

    @Test
    void planIsIndependentOfRowAndEntryOrder() throws Exception {
        List<ArtifactMatcher.SourceRow> rows = new ArrayList<>(List.of(row(1, "files/a/ფაილი.xlsx", null), row(2, "files/a/File.xlsx", "files/a/unused.xlsx"), row(3, "files/a/x.xlsx", null)));
        List<ArtifactManifest.Entry> shuffledEntries = new ArrayList<>(entries);
        ArtifactMatcher.Plan expected = ArtifactMatcher.match(definition(RULE), rows, entries);
        for (int seed = 0; seed < 25; seed++) {
            Collections.shuffle(rows, new Random(seed));
            Collections.shuffle(shuffledEntries, new Random(seed + 100));
            assertEquals(expected, ArtifactMatcher.match(definition(RULE), rows, shuffledEntries));
        }
    }

    @Test
    void invalidRulesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RULES.parse(JSON.readTree("{\"type\":\"FILENAME_GUESS\",\"bindings\":[{\"field\":\"f\"}]}")));
        assertThrows(IllegalArgumentException.class, () -> RULES.parse(JSON.readTree("{\"type\":\"SOURCE_PATH\",\"bindings\":[]}")));
        assertThrows(IllegalArgumentException.class, () -> RULES.parse(JSON.readTree("{\"type\":\"SOURCE_PATH\",\"bindings\":[{\"field\":\"a;drop\"}]}")));
        assertThrows(IllegalArgumentException.class, () -> RULES.parse(JSON.readTree(
                "{\"type\":\"SOURCE_PATH\",\"bindings\":[{\"language\":\"ka\",\"field\":\"a\"},{\"language\":\"ka\",\"field\":\"b\"}]}")));
        assertThrows(IllegalArgumentException.class, () -> RULES.parse(JSON.readTree("{\"type\":\"SOURCE_PATH\",\"packageRoot\":\"root\",\"bindings\":[{\"field\":\"a\"}]}")));
    }

    @Test
    void arrayValuesBindOneToManyWithSourceOrderOrdinals() throws Exception {
        String rule = "{\"type\":\"SOURCE_PATH\",\"stripPrefix\":\"files/\",\"packageRoot\":\"mainstat/\",\"bindings\":[{\"language\":\"und\",\"field\":\"files\"}]}";
        ArtifactRelationDefinition many = new ArtifactRelationDefinition(1, "SUPPORTING_FILES", ArtifactRole.SUPPORTING, policy(), 1, 3, true, RULES.parse(JSON.readTree(rule)));
        ObjectNode payload = JSON.createObjectNode();
        payload.putArray("files").add("files/a/File.xlsx").add("files/a/unused.xlsx");
        ArtifactMatcher.Plan plan = ArtifactMatcher.match(many, List.of(new ArtifactMatcher.SourceRow(1, "r|1", 101, payload)), entries);
        assertFalse(plan.blocked());
        assertEquals(List.of(1, 2), plan.edges().stream().map(ArtifactMatcher.PlannedEdge::ordinal).toList());
        assertEquals("mainstat/a/unused.xlsx", plan.edges().get(1).entry().originalPath());

        payload.withArray("files").add("files/a/ფაილი.xlsx").add("files/a/File.xlsx");
        ArtifactMatcher.Plan tooMany = ArtifactMatcher.match(many, List.of(new ArtifactMatcher.SourceRow(1, "r|1", 101, payload)), entries);
        assertTrue(tooMany.blocked());
        assertEquals(ArtifactIssue.Code.CARDINALITY_VIOLATION, tooMany.issues().get(0).code());
    }

    @Test
    void unorderedArrayValuesUseCanonicalPathOrder() throws Exception {
        String rule = "{\"type\":\"SOURCE_PATH\",\"stripPrefix\":\"files/\",\"packageRoot\":\"mainstat/\",\"bindings\":[{\"language\":\"und\",\"field\":\"files\"}]}";
        ArtifactRelationDefinition unordered = new ArtifactRelationDefinition(1, "SUPPORTING_FILES", ArtifactRole.SUPPORTING,
                policy(), 1, 3, false, RULES.parse(JSON.readTree(rule)));
        ObjectNode first = JSON.createObjectNode();
        first.putArray("files").add("files/a/unused.xlsx").add("files/a/File.xlsx");
        ObjectNode second = JSON.createObjectNode();
        second.putArray("files").add("files/a/File.xlsx").add("files/a/unused.xlsx");

        ArtifactMatcher.Plan left = ArtifactMatcher.match(unordered,
                List.of(new ArtifactMatcher.SourceRow(1, "r|1", 101, first)), entries);
        ArtifactMatcher.Plan right = ArtifactMatcher.match(unordered,
                List.of(new ArtifactMatcher.SourceRow(1, "r|1", 101, second)), entries);

        assertEquals(left, right);
        assertEquals(List.of("mainstat/a/File.xlsx", "mainstat/a/unused.xlsx"),
                left.edges().stream().map(edge -> edge.entry().originalPath()).toList());
    }
}
