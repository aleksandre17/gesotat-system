package org.base.api.service.platform.statistical;

import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.export.SdmxCsvExporter;
import org.base.api.service.platform.statistical.export.SdmxCsvExporter.Format;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class SdmxCsvExporterTest {
    private final InMemoryStatisticalRegistry registry = registry();
    private final SemanticPlan labour = compiler(registry).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
    private final SemanticPlan land = compiler(registry).compile(landDraft(false), LAND, Mode.APPROVAL).plan().orElseThrow();

    private List<WideRowNormalizer.Observation> observations(SemanticPlan plan, StatisticalRegistry.Scope scope, List<Map<String, Object>> rows) {
        WideRowNormalizer.Result result = new WideRowNormalizer(plan, (ref, code) -> registry.codelist(ref, scope).orElseThrow().codes().contains(code), "OBS_STATUS").normalize(rows);
        assertTrue(result.issues().isEmpty(), result.issues().toString());
        return result.observations();
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) row.put((String) pairs[i], pairs[i + 1]);
        return row;
    }

    @Test void multiMeasureStructureIsOneRowPerKeyWithEachMeasureAndItsStatusInItsOwnColumn() {
        List<Map<String, Object>> rows = List.of(
                row("TIME_PERIOD", "2025", "REF_AREA", "GE_TB", "SEX", "M", "EMPLOYED", 13000, "UNEMPLOYED", null, "UNEMPLOYED_STATUS", "M"),
                row("TIME_PERIOD", "2025", "REF_AREA", "GE_TB", "SEX", "F", "EMPLOYED", 12000, "EMPLOYED_STATUS", "P", "UNEMPLOYED", 1800));
        String csv = SdmxCsvExporter.export(labour, observations(labour, LABOUR, rows), Format.SDMX_CSV_2_0);
        assertEquals("""
                STRUCTURE,STRUCTURE_ID,ACTION,TIME_PERIOD,REF_AREA,SEX,EMPLOYED,UNEMPLOYED,EMPLOYED_STATUS,UNEMPLOYED_STATUS\r
                datastructure,LABOUR:DSD_LABOUR(1.0.0),I,2025,GE_TB,F,12000,1800,P,\r
                datastructure,LABOUR:DSD_LABOUR(1.0.0),I,2025,GE_TB,M,13000,,,M\r
                """, csv, "dataset-level attributes are structure metadata, not columns; a missing value is empty with its status");
    }

    @Test void outputIsIndependentOfInputOrderAndKeepsExactDecimals() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String use : List.of("URBAN", "ARABLE", "FOREST")) rows.add(row("LAND_USE", use, "AREA_SIZE", "2822400.1234567890"));
        String expected = SdmxCsvExporter.export(land, observations(land, LAND, rows), Format.SDMX_CSV_2_0);
        Collections.reverse(rows);
        assertEquals(expected, SdmxCsvExporter.export(land, observations(land, LAND, rows), Format.SDMX_CSV_2_0));
        assertTrue(expected.contains("datastructure,LAND:DSD_LAND_USE(1.0.0),I,GE,ARABLE,2822400.1234567890\r\n"), "constant dimension expanded; no time column in a non-time structure; no exponent, no rounding");
        assertTrue(expected.indexOf("ARABLE") < expected.indexOf("FOREST") && expected.indexOf("FOREST") < expected.indexOf("URBAN"));
    }

    @Test void whatTheTargetFormatCannotHoldIsRefusedNotPivoted() {
        var observations = observations(labour, LABOUR, List.of(row("TIME_PERIOD", "2025", "REF_AREA", "GE", "SEX", "F", "EMPLOYED", 1, "UNEMPLOYED", 2)));
        SdmxCsvExporter.UnsupportedConversion refused = assertThrows(SdmxCsvExporter.UnsupportedConversion.class,
                () -> SdmxCsvExporter.export(labour, observations, Format.SDMX_ML_2_1_GENERIC));
        assertTrue(refused.getMessage().contains("2 measures"));
    }
}
