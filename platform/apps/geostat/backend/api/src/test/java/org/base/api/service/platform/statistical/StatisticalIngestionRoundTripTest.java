package org.base.api.service.platform.statistical;

import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.PropertyMap;
import com.healthmarketscience.jackcess.Table;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter.CodeItem;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer.IssueCode;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer.Observation;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class StatisticalIngestionRoundTripTest {
    private static final String STATUS = "OBS_STATUS";
    private final InMemoryStatisticalRegistry registry = registry();
    private final SemanticPlan labour = compiler(registry).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
    private final SemanticPlan land = compiler(registry).compile(landDraft(false), LAND, Mode.APPROVAL).plan().orElseThrow();

    private WideRowNormalizer normalizer(SemanticPlan plan, org.base.api.service.platform.statistical.registry.StatisticalRegistry.Scope scope) {
        return new WideRowNormalizer(plan, (ref, code) -> registry.codelist(ref, scope).orElseThrow().codes().contains(code), STATUS);
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
        return row;
    }

    private static Set<IssueCode> codes(WideRowNormalizer.Result result) {
        return result.issues().stream().map(WideRowNormalizer.RowIssue::code).collect(Collectors.toSet());
    }

    @Test void oneWideRowBecomesOneObservationPerMeasureWithItsOwnStatusAndUnit() {
        WideRowNormalizer.Result result = normalizer(labour, LABOUR).normalize(List.of(
                row("TIME_PERIOD", "2025", "REF_AREA", "GE_TB", "SEX", "F", "EMPLOYED", 12000, "EMPLOYED_STATUS", "P", "UNEMPLOYED", "1800")));
        assertTrue(result.accepted() && result.issues().isEmpty());
        assertEquals(2, result.observations().size());
        Observation employed = result.observations().get(0), unemployed = result.observations().get(1);
        assertEquals(employed.observationKey(), unemployed.observationKey(), "same grain");
        assertEquals("P", employed.status());
        assertNull(unemployed.status(), "a status belongs to its own measure only");
        assertEquals(new BigDecimal("1800"), unemployed.value());
        assertEquals(Ref.parse("unit:SHARED:PERSONS(1.0.0)"), employed.unitRef());
        assertEquals(java.time.LocalDate.of(2025, 12, 31), employed.period().endInclusive());
    }

    @Test void zeroMissingAndUnexplainedEmptyAreDifferentFacts() {
        WideRowNormalizer n = normalizer(labour, LABOUR);
        WideRowNormalizer.Result zero = n.normalize(List.of(row("TIME_PERIOD", "2025", "REF_AREA", "GE", "SEX", "F", "EMPLOYED", 0, "UNEMPLOYED", 0)));
        assertEquals(BigDecimal.ZERO, zero.observations().get(0).value());
        WideRowNormalizer.Result missing = n.normalize(List.of(row("TIME_PERIOD", "2025", "REF_AREA", "GE", "SEX", "F", "EMPLOYED", null, "EMPLOYED_STATUS", "M", "UNEMPLOYED", 5)));
        assertTrue(missing.accepted());
        assertNull(missing.observations().get(0).value());
        assertEquals("M", missing.observations().get(0).status());
        WideRowNormalizer.Result silent = n.normalize(List.of(row("TIME_PERIOD", "2025", "REF_AREA", "GE", "SEX", "F", "EMPLOYED", " ", "UNEMPLOYED", 5)));
        assertFalse(silent.accepted());
        assertEquals(Set.of(IssueCode.VALUE_WITHOUT_STATUS_MISSING), codes(silent));
    }

    @Test void atomicModeRejectsTheWholeLoadAndReportsEveryFinding() {
        WideRowNormalizer.Result result = normalizer(labour, LABOUR).normalize(List.of(
                row("TIME_PERIOD", "2025", "REF_AREA", "GE", "SEX", "F", "EMPLOYED", 1, "UNEMPLOYED", 1),
                row("TIME_PERIOD", "2025-Q5", "REF_AREA", "XX", "SEX", "F", "EMPLOYED", 1, "UNEMPLOYED", 1, "json_path", "$.x"),
                row("TIME_PERIOD", "2025", "REF_AREA", "GE", "EMPLOYED", 1.5d, "UNEMPLOYED", "1,5"),
                row("TIME_PERIOD", "2025", "REF_AREA", "GE", "SEX", "F", "EMPLOYED", 2, "UNEMPLOYED", 2)));
        assertFalse(result.accepted());
        assertTrue(result.observations().isEmpty(), "no silent partial load");
        assertEquals(Set.of(IssueCode.INVALID_PERIOD, IssueCode.INVALID_CODE, IssueCode.UNDECLARED_COLUMN, IssueCode.MISSING_DIMENSION,
                IssueCode.INEXACT_INPUT, IssueCode.INVALID_VALUE, IssueCode.DUPLICATE_OBSERVATION), codes(result));
    }

    @Test void quarantineModeKeepsValidRowsAndNamesTheRest() {
        WideRowNormalizer.Result result = normalizer(land, LAND).normalize(List.of(
                row("LAND_USE", "FOREST", "AREA_SIZE", "2822400.1234567890"),
                row("LAND_USE", "SWAMP", "AREA_SIZE", "1"),
                row("REF_AREA", "GE_KA", "LAND_USE", "FOREST", "AREA_SIZE", "10.5")));
        assertTrue(result.accepted());
        assertEquals(Set.of(2L), result.quarantinedRows());
        assertEquals(2, result.observations().size());
        assertEquals("GE", result.observations().get(0).dimensions().get("REF_AREA"), "constant expanded deterministically");
        assertEquals(Set.of("REF_AREA"), result.observations().get(1).overriddenConstants(), "declared override is recorded");
        assertNull(result.observations().get(0).period(), "non-time dataset");
    }

    @Test void decimalsAreExactAndNeverRounded() {
        WideRowNormalizer n = normalizer(land, LAND);
        String max = "999999999999999999.9999999999"; // 28 digits, scale 10
        assertEquals(new BigDecimal(max), n.normalize(List.of(row("LAND_USE", "URBAN", "AREA_SIZE", max))).observations().get(0).value());
        assertEquals(new BigDecimal("-0.0000000001"), n.normalize(List.of(row("LAND_USE", "URBAN", "AREA_SIZE", "-0.0000000001"))).observations().get(0).value());
        assertEquals(new BigDecimal("1.50"), n.normalize(List.of(row("LAND_USE", "URBAN", "AREA_SIZE", "1.50"))).observations().get(0).value(), "lexical scale preserved");
        assertEquals(Set.of(IssueCode.SCALE_EXCEEDED), codes(n.normalize(List.of(row("LAND_USE", "URBAN", "AREA_SIZE", "0.00000000001")))));
        assertEquals(Set.of(IssueCode.NUMERIC_OVERFLOW), codes(n.normalize(List.of(row("LAND_USE", "URBAN", "AREA_SIZE", "1000000000000000000")))));
        assertEquals(Set.of(IssueCode.INEXACT_INPUT), codes(n.normalize(List.of(row("LAND_USE", "URBAN", "AREA_SIZE", 0.1d)))));
    }

    /** Property: the observation key and the produced set are independent of row order and of replay. */
    @Test void keysAreStableUnderReorderAndReplay() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String area : List.of("GE", "GE_TB", "GE_KA")) for (String sex : List.of("F", "M", "_T")) for (int year = 2015; year < 2025; year++)
            rows.add(row("TIME_PERIOD", String.valueOf(year), "REF_AREA", area, "SEX", sex, "EMPLOYED", year, "UNEMPLOYED", 1));
        WideRowNormalizer n = normalizer(labour, LABOUR);
        Map<String, BigDecimal> baseline = index(n.normalize(rows));
        assertEquals(rows.size() * 2, baseline.size());
        Random random = new Random(20260919L);
        for (int i = 0; i < 25; i++) {
            List<Map<String, Object>> shuffled = new ArrayList<>(rows);
            Collections.shuffle(shuffled, random);
            assertEquals(baseline, index(n.normalize(shuffled)));
        }
    }

    private static Map<String, BigDecimal> index(WideRowNormalizer.Result result) {
        return result.observations().stream().collect(Collectors.toMap(o -> o.observationKey() + "|" + o.measureCode(), Observation::value));
    }

    // ---- Access adapter: emit -> fill -> read -> normalize (Q34, Q36)

    private static final Map<Ref, List<CodeItem>> CODELISTS = Map.of(
            CL_AREA, List.of(new CodeItem("GE", "საქართველო"), new CodeItem("GE_TB", "თბილისი"), new CodeItem("GE_KA", "კახეთი")),
            CL_SEX, List.of(new CodeItem("F", "ქალი"), new CodeItem("M", "კაცი"), new CodeItem("_T", "ჯამი")),
            CL_OBS_STATUS, List.of(new CodeItem("A", "Normal"), new CodeItem("M", "Missing"), new CodeItem("P", "Provisional")),
            CL_LAND_USE, List.of(new CodeItem("ARABLE", "სახნავი"), new CodeItem("FOREST", "ტყე"), new CodeItem("URBAN", "ურბანული")));

    @Test void accessTemplateIsTypedCaptionedAndRoundTripsExactDecimals(@TempDir Path dir) throws Exception {
        AccessAuthoringAdapter adapter = new AccessAuthoringAdapter();
        File file = dir.resolve("land.accdb").toFile();
        adapter.emit(land, CODELISTS, "ka", file);

        String tableName = land.physical().tables().get(0).name();
        try (Database db = DatabaseBuilder.open(file)) {
            assertEquals(Database.FileFormat.V2010, db.getFileFormat(), "a format every supported Access build opens");
            Table table = db.getTable(tableName);
            assertEquals(DataType.NUMERIC, table.getColumn("AREA_SIZE").getType(), "exact decimal, not DOUBLE");
            assertEquals(28, table.getColumn("AREA_SIZE").getPrecision());
            assertEquals(10, table.getColumn("AREA_SIZE").getScale());
            assertEquals("ფართობი", table.getColumn("AREA_SIZE").getProperties().getValue(PropertyMap.CAPTION_PROP));
            assertTrue(String.valueOf(table.getColumn("LAND_USE").getProperties().getValue("RowSource")).contains("__cl_"), "stable code stored, label shown");
            assertTrue(db.getTableNames().stream().noneMatch(n -> n.toLowerCase().contains("carrier") || n.toLowerCase().contains("kids")), "no legacy or site-specific surface");

            // The Navigation Pane shows where data is entered, separately from what only describes the contract.
            Map<String, java.util.List<String>> pane = new java.util.LinkedHashMap<>();
            Map<Integer, String> objectNames = new java.util.HashMap<>();
            for (com.healthmarketscience.jackcess.Row row : db.getSystemTable("MSysNavPaneObjectIDs")) objectNames.put((Integer) row.get("Id"), String.valueOf(row.get("Name")));
            Map<Integer, String> groupNames = new java.util.HashMap<>();
            Integer category = null;
            for (com.healthmarketscience.jackcess.Row row : db.getSystemTable("MSysNavPaneGroupCategories"))
                if (AccessAuthoringAdapter.CATEGORY.equals(row.get("Name"))) category = (Integer) row.get("Id");
            assertNotNull(category, "the generated file declares its own Navigation Pane category");
            for (com.healthmarketscience.jackcess.Row row : db.getSystemTable("MSysNavPaneGroups"))
                if (category.equals(row.get("GroupCategoryID"))) groupNames.put((Integer) row.get("Id"), String.valueOf(row.get("Name")));
            for (com.healthmarketscience.jackcess.Row row : db.getSystemTable("MSysNavPaneGroupToObjects")) {
                String group = groupNames.get((Integer) row.get("GroupID"));
                if (group != null) pane.computeIfAbsent(group, g -> new java.util.ArrayList<>()).add(objectNames.get((Integer) row.get("ObjectID")));
            }
            assertEquals(java.util.List.of(tableName), pane.get(AccessAuthoringAdapter.GROUP_DATA));
            assertEquals(2, pane.get(AccessAuthoringAdapter.GROUP_CODELISTS).size(), "both codelists of the land structure, including the overridable constant dimension");
            assertEquals(java.util.List.of(AccessAuthoringAdapter.STAMP_TABLE), pane.get(AccessAuthoringAdapter.GROUP_CONTRACT));
            table.addRow("GE", "FOREST", new BigDecimal("2822400.1234567890"));
            table.addRow("GE_KA", "ARABLE", new BigDecimal("999999999999999999.9999999999"));
        }

        AccessAuthoringAdapter.ReadResult read = adapter.read(file, land);
        assertTrue(read.accepted(), read.issues().toString());
        WideRowNormalizer.Result result = normalizer(land, LAND).normalize(read.rows());
        assertTrue(result.issues().isEmpty(), result.issues().toString());
        assertEquals(List.of(new BigDecimal("2822400.1234567890"), new BigDecimal("999999999999999999.9999999999")),
                result.observations().stream().map(Observation::value).toList());
    }

    @Test void fileStampedForAnotherRevisionOrWithEditedMetadataIsRefused(@TempDir Path dir) throws Exception {
        AccessAuthoringAdapter adapter = new AccessAuthoringAdapter();
        File file = dir.resolve("labour.accdb").toFile();
        adapter.emit(labour, CODELISTS, "ka", file);
        assertTrue(adapter.read(file, labour).accepted());

        SemanticPlan nextRevision = compiler(registry).compile(labourDraft().replace("[\"YEAR\",\"QUARTER\"]", "[\"YEAR\"]"), LABOUR, Mode.APPROVAL).plan().orElseThrow();
        assertEquals(List.of("CONTRACT_STAMP_MISMATCH"), adapter.read(file, nextRevision).issues());

        try (Database db = DatabaseBuilder.open(file)) {
            Table stamp = db.getTable(AccessAuthoringAdapter.STAMP_TABLE);
            for (var row : stamp) if ("semanticDigest".equals(row.getString("stamp_key"))) { row.put("stamp_value", "0".repeat(64)); stamp.updateRow(row); }
        }
        assertEquals(List.of("CONTRACT_STAMP_MISMATCH"), adapter.read(file, labour).issues(), "edited metadata never becomes authority");
    }

    @Test void splitPartsRejoinByKeyAndAnIncompletePartBlocksTheLoad(@TempDir Path dir) throws Exception {
        ProviderCapabilities.Catalog narrow = profile -> Optional.of(new ProviderCapabilities("ACCESS_ACCDB", 5, 64, 10, 28, true, Set.of()));
        SemanticPlan split = compiler(registry, narrow).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
        assertEquals(2, split.physical().tables().size());
        AccessAuthoringAdapter adapter = new AccessAuthoringAdapter();
        File file = dir.resolve("split.accdb").toFile();
        adapter.emit(split, CODELISTS, "ka", file);
        try (Database db = DatabaseBuilder.open(file)) {
            db.getTable(split.physical().tables().get(0).name()).addRow("2025", "GE", "F", new BigDecimal("10"), "A");
            db.getTable(split.physical().tables().get(1).name()).addRow("2025", "GE", "F", new BigDecimal("3"), null);
        }
        AccessAuthoringAdapter.ReadResult read = adapter.read(file, split);
        assertTrue(read.accepted(), read.issues().toString());
        WideRowNormalizer.Result joined = normalizer(split, LABOUR).normalize(read.rows());
        assertEquals(index(normalizer(labour, LABOUR).normalize(List.of(row("TIME_PERIOD", "2025", "REF_AREA", "GE", "SEX", "F",
                "EMPLOYED", 10, "EMPLOYED_STATUS", "A", "UNEMPLOYED", 3)))), index(joined), "layout is not semantics");

        try (Database db = DatabaseBuilder.open(file)) {
            db.getTable(split.physical().tables().get(0).name()).addRow("2024", "GE", "F", new BigDecimal("9"), "A");
        }
        assertTrue(adapter.read(file, split).issues().get(0).startsWith("SPLIT_PART_INCOMPLETE"));
    }
}
