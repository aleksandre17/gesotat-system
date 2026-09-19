package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract §27: 1000 business rows and 1500 files, many files per row and files shared between rows, declared
 * in package tables (§4.2, §4.3). The edges derived at admission, the accepted manifest document and the edges
 * replayed at snapshot binding must be the same set.
 */
class RelationTableMatchRuleAcceptanceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ArtifactMatchRules RULES = new ArtifactMatchRules(List.of(new SourcePathMatchRuleParser(), new RelationTableMatchRuleParser()));
    private static final String CSV = MediaTypes.forFileName("a.csv");
    private static final ArtifactPackageContractResolver.DatasetContract CONTRACT = new ArtifactPackageContractResolver.DatasetContract(
            "SITE_B", 1, "c".repeat(64), 91, "RECORDS", "__ent_records", List.of("record_id"), List.of("record_id"));
    private static final AccessDatasetCarrier CARRIER = new AccessDatasetCarrier(new ArtifactAccessPackageValidator());

    private Path access;

    @AfterEach
    void cleanUp() throws Exception {
        if (access != null) Files.deleteIfExists(access);
    }

    private static ArtifactRelationDefinition definition(int max) throws Exception {
        return new ArtifactRelationDescriptor(91, "SUPPORTING_FILES", "SUPPORTING", 1, max, true, JSON.readTree("""
                {"type":"RELATION_TABLE","relationTable":"__rel_entity_artifact","entityKeyField":"entity_source_row_key",
                 "artifactKeyField":"artifact_source_row_key","ordinalField":"ordinal","roleField":"role_code","role":"SUPPORTING_DOCUMENT",
                 "artifactTable":"__raw_document","artifactTableKeyField":"source_row_key","fileNameField":"original_filename",
                 "packageRoot":"files/","languages":["und"]}"""),
                new ArtifactRelationDescriptor.Policy("POLICY", 1, "AUTHENTICATED", null, Set.of(CSV), 1024, 300, "RETAIN_INDEFINITE")).toDefinition(RULES);
    }

    /** rows × (entityKey, artifactKey, role, ordinal) */
    private File database(int rows, int artifacts, List<Object[]> relations) throws Exception {
        access = Files.createTempFile("relation-table-", ".accdb");
        try (Database database = DatabaseBuilder.create(Database.FileFormat.V2010, access.toFile())) {
            Table entities = new TableBuilder("__ent_records").addColumn(new ColumnBuilder("record_id", DataType.TEXT)).toTable(database);
            for (int row = 1; row <= rows; row++) entities.addRow("row|" + row);
            Table documents = new TableBuilder("__raw_document").addColumn(new ColumnBuilder("source_row_key", DataType.TEXT))
                    .addColumn(new ColumnBuilder("original_filename", DataType.TEXT)).toTable(database);
            for (int file = 1; file <= artifacts; file++) documents.addRow("file|" + file, "report-" + file + ".csv");
            Table edges = new TableBuilder("__rel_entity_artifact").addColumn(new ColumnBuilder("entity_source_row_key", DataType.TEXT))
                    .addColumn(new ColumnBuilder("artifact_source_row_key", DataType.TEXT)).addColumn(new ColumnBuilder("role_code", DataType.TEXT))
                    .addColumn(new ColumnBuilder("ordinal", DataType.LONG)).toTable(database);
            for (Object[] relation : relations) edges.addRow(relation);
        }
        return access.toFile();
    }

    private static List<ArtifactManifest.Entry> entries(int artifacts) {
        List<ArtifactManifestGenerator.InventoryEntry> inventory = new ArrayList<>();
        for (int file = 1; file <= artifacts; file++)
            inventory.add(new ArtifactManifestGenerator.InventoryEntry("files/report-" + file + ".csv", Sha256.ofUtf8("content-" + file), 10));
        return ArtifactManifestGenerator.generate("PKG", "bucket-a", "pool/", "test", inventory, "SITE_B", 1, 91L).entries();
    }

    private static Set<String> edgeSet(ArtifactMatcher.Plan plan) {
        Set<String> edges = new TreeSet<>();
        plan.edges().forEach(edge -> edges.add(edge.externalKey() + "|" + edge.language() + "|" + edge.ordinal() + "|" + edge.entry().originalPath()));
        return edges;
    }

    @Test
    void thousandRowsFifteenHundredFilesGiveTheSameEdgesAtAdmissionInTheDocumentAndAtBinding() throws Exception {
        List<Object[]> relations = new ArrayList<>();
        for (int row = 1; row <= 1000; row++) relations.add(new Object[]{"row|" + row, "file|" + row, "SUPPORTING_DOCUMENT", 1});
        for (int row = 1; row <= 500; row++) relations.add(new Object[]{"row|" + row, "file|" + (1000 + row), "SUPPORTING_DOCUMENT", 2});
        relations.add(new Object[]{"row|999", "file|1", "SUPPORTING_DOCUMENT", 2});   // one file shared by three rows
        relations.add(new Object[]{"row|1000", "file|1", "SUPPORTING_DOCUMENT", 2});
        relations.add(new Object[]{"row|7", "file|9", "OTHER_ROLE", 1});               // another relation's edge: out of scope
        File file = database(1000, 1500, relations);
        List<ArtifactRelationDefinition> definitions = List.of(definition(3));
        List<ArtifactManifest.Entry> entries = entries(1500);

        var admission = ArtifactRelationPreview.evaluate(definitions, DeclaredRowValues.packageRows(definitions, CARRIER, file, CONTRACT), entries);

        assertFalse(admission.blocked(), admission.errors().toString());
        assertEquals(1502, admission.relations().get(0).edgeCount());
        assertEquals(0, admission.relations().get(0).orphanCount());

        PackageManifestDocument document = JSON.readValue(JSON.writeValueAsBytes(
                PackageManifestDocument.describe(CONTRACT, "data.accdb", entries, admission.plans())), PackageManifestDocument.class);
        assertEquals(1502, document.edges().size());

        // Snapshot binding sees payload rows only; the declared values come back from the accepted document.
        var snapshotRows = CARRIER.rows(file, CONTRACT);
        var binding = ArtifactMatcher.match(definitions.get(0),
                DeclaredRowValues.apply(definitions, snapshotRows, DeclaredRowValues.fromDocument(definitions, document)), entries);

        assertFalse(binding.blocked());
        assertEquals(edgeSet(admission.plans().get(0)), edgeSet(binding));
        assertTrue(edgeSet(binding).contains("row|1|und|2|files/report-1001.csv"));
        assertTrue(edgeSet(binding).contains("row|1000|und|2|files/report-1.csv"));
    }

    @Test
    void cardinalityMissingAttachmentsAndUnreferencedFilesAreReportedByTheSameMatcher() throws Exception {
        File file = database(3, 4, List.of(new Object[]{"row|1", "file|1", "SUPPORTING_DOCUMENT", 1}, new Object[]{"row|1", "file|2", "SUPPORTING_DOCUMENT", 2}));
        List<ArtifactRelationDefinition> definitions = List.of(definition(1));

        var report = ArtifactRelationPreview.evaluate(definitions, DeclaredRowValues.packageRows(definitions, CARRIER, file, CONTRACT), entries(4));

        assertTrue(report.blocked());
        Set<ArtifactIssue.Code> codes = new java.util.HashSet<>();
        report.relations().get(0).issues().forEach(issue -> codes.add(issue.code()));
        assertTrue(codes.containsAll(Set.of(ArtifactIssue.Code.CARDINALITY_VIOLATION, ArtifactIssue.Code.MISSING_SOURCE_VALUE, ArtifactIssue.Code.ORPHAN_ARTIFACT)), codes.toString());
    }

    @Test
    void danglingOrAmbiguousDeclarationsAreRefusedNotGuessed() throws Exception {
        List<ArtifactRelationDefinition> definitions = List.of(definition(3));
        File dangling = database(1, 1, List.<Object[]>of(new Object[]{"row|1", "file|404", "SUPPORTING_DOCUMENT", 1}));
        assertThrows(IllegalArgumentException.class, () -> DeclaredRowValues.packageRows(definitions, CARRIER, dangling, CONTRACT));
        Files.deleteIfExists(access);

        File gap = database(1, 2, List.of(new Object[]{"row|1", "file|1", "SUPPORTING_DOCUMENT", 1}, new Object[]{"row|1", "file|2", "SUPPORTING_DOCUMENT", 3}));
        assertThrows(IllegalArgumentException.class, () -> DeclaredRowValues.packageRows(definitions, CARRIER, gap, CONTRACT));
        Files.deleteIfExists(access);

        File twoOnOneOrdinal = database(1, 2, List.of(new Object[]{"row|1", "file|1", "SUPPORTING_DOCUMENT", 1}, new Object[]{"row|1", "file|2", "SUPPORTING_DOCUMENT", 1}));
        assertThrows(IllegalArgumentException.class, () -> DeclaredRowValues.packageRows(definitions, CARRIER, twoOnOneOrdinal, CONTRACT));
        Files.deleteIfExists(access);

        File unknownRow = database(1, 1, List.<Object[]>of(new Object[]{"row|77", "file|1", "SUPPORTING_DOCUMENT", 1}));
        assertThrows(IllegalArgumentException.class, () -> DeclaredRowValues.packageRows(definitions, CARRIER, unknownRow, CONTRACT));
    }

    @Test
    void aFormatWithoutTablesCannotSupplyPackageDeclaredEdges() throws Exception {
        PackageDatasetCarrier flat = new PackageDatasetCarrier() {
            @Override public boolean carries(String extension) { return true; }
            @Override public void validate(File file, ArtifactPackageContractResolver.DatasetContract contract) { }
            @Override public List<ArtifactMatcher.SourceRow> rows(File file, ArtifactPackageContractResolver.DatasetContract contract) { return List.of(); }
        };
        List<ArtifactRelationDefinition> definitions = List.of(definition(3));
        assertThrows(IllegalArgumentException.class, () -> DeclaredRowValues.packageRows(definitions, flat, new File("unused"), CONTRACT));
    }

    @Test
    void ruleDeclarationIsValidatedWhenTheContractIsParsed() {
        assertThrows(IllegalArgumentException.class, () -> RULES.parse(JSON.readTree("{\"type\":\"RELATION_TABLE\",\"packageRoot\":\"files/\",\"languages\":[\"und\"]}")));
        assertThrows(IllegalArgumentException.class, () -> RULES.parse(JSON.readTree(
                "{\"type\":\"RELATION_TABLE\",\"relationTable\":\"t; DROP\",\"entityKeyField\":\"a\",\"artifactKeyField\":\"b\",\"ordinalField\":\"c\","
                        + "\"artifactTable\":\"d\",\"artifactTableKeyField\":\"e\",\"fileNameField\":\"f\",\"packageRoot\":\"files/\",\"languages\":[\"und\"]}")));
    }
}
