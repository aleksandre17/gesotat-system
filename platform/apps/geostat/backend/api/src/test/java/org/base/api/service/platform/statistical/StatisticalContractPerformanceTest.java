package org.base.api.service.platform.statistical;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter.CodeItem;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.export.SdmxCsvExporter;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Budget check of decision register Q14 at the design ceiling: 200 000 wide rows, two measures with their
 * statuses, inside the 1 GiB heap the test task is given. Off by default; run it off the shared host with
 * {@code -Dstat.performance=true}. It measures the file path a load takes: fill, read, normalise, export.
 * The database write is measured separately on a real SQL Server, not here.
 */
@EnabledIfSystemProperty(named = "stat.performance", matches = "true")
class StatisticalContractPerformanceTest {
    private static final int ROWS = 200_000;
    private static final long VALIDATION_BUDGET_MS = 120_000;   // Q14: validation p95 <= 120 s
    private static final long FILE_BUDGET_BYTES = 250L << 20;    // Q14: artifact <= 250 MB

    @Test void designCeilingFitsTheTimeSizeAndHeapBudgets(@TempDir Path dir) throws Exception {
        InMemoryStatisticalRegistry registry = registry();
        // A wide code space so that 200 000 distinct keys exist: 2 000 periods x 100 areas x 1 sex is not realistic,
        // so the area list is widened instead and periods stay plausible.
        Set<String> areas = new java.util.TreeSet<>();
        for (int i = 0; i < 2_000; i++) areas.add("A" + i);
        registry.approved(CL_AREA, new org.base.api.service.platform.statistical.registry.StatisticalRegistry.Codelist(CL_AREA, areas));
        SemanticPlan plan = compiler(registry).compile(labourDraft().replace("[\"YEAR\",\"QUARTER\"]", "[\"YEAR\"]"), LABOUR, Mode.APPROVAL).plan().orElseThrow();

        Map<Ref, List<CodeItem>> codelists = new HashMap<>();
        for (Ref ref : List.of(CL_AREA, CL_SEX, CL_OBS_STATUS)) codelists.put(ref, registry.codelist(ref, LABOUR).orElseThrow().codes().stream().sorted().map(c -> new CodeItem(c, c)).toList());
        File file = dir.resolve("ceiling.accdb").toFile();
        AccessAuthoringAdapter adapter = new AccessAuthoringAdapter();
        adapter.emit(plan, codelists, "ka", file);

        long t0 = System.nanoTime();
        try (Database db = DatabaseBuilder.open(file)) {
            Table table = db.getTable(plan.physical().tables().get(0).name());
            List<Object[]> batch = new java.util.ArrayList<>(5_000);
            String[] sexes = {"F", "M"};
            int written = 0;
            outer:
            for (int year = 1975; year < 2025; year++) for (String area : areas) for (String sex : sexes) {
                batch.add(new Object[]{String.valueOf(year), area, sex, BigDecimal.valueOf(written), written % 97 == 0 ? "P" : null, written % 89 == 0 ? null : BigDecimal.valueOf(written % 1000), written % 89 == 0 ? "M" : null});
                if (batch.size() == 5_000) { table.addRows(batch); batch.clear(); }
                if (++written == ROWS) break outer;
            }
            if (!batch.isEmpty()) table.addRows(batch);
        }
        long fillMs = (System.nanoTime() - t0) / 1_000_000;
        long bytes = Files.size(file.toPath());

        Runtime runtime = Runtime.getRuntime();
        long t1 = System.nanoTime();
        AccessAuthoringAdapter.ReadResult read = adapter.read(file, plan);
        long readMs = (System.nanoTime() - t1) / 1_000_000;
        assertTrue(read.accepted(), read.issues().toString());
        assertEquals(ROWS, read.rows().size());

        long t2 = System.nanoTime();
        WideRowNormalizer.Result result = new WideRowNormalizer(plan, (ref, code) -> registry.codelist(ref, LABOUR).orElseThrow().codes().contains(code), "OBS_STATUS").normalize(read.rows());
        long normaliseMs = (System.nanoTime() - t2) / 1_000_000;
        assertTrue(result.accepted() && result.issues().isEmpty(), () -> result.issues().subList(0, Math.min(5, result.issues().size())).toString());
        assertEquals(ROWS * 2, result.observations().size());

        long t3 = System.nanoTime();
        String csv = SdmxCsvExporter.export(plan, result.observations(), SdmxCsvExporter.Format.SDMX_CSV_2_0);
        long exportMs = (System.nanoTime() - t3) / 1_000_000;
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) >> 20, maxMb = runtime.maxMemory() >> 20;

        System.out.printf("STAT_PERF rows=%d observations=%d fileBytes=%d fillMs=%d readMs=%d normaliseMs=%d exportMs=%d csvChars=%d heapUsedMb=%d heapMaxMb=%d%n",
                ROWS, result.observations().size(), bytes, fillMs, readMs, normaliseMs, exportMs, csv.length(), usedMb, maxMb);
        assertTrue(readMs + normaliseMs <= VALIDATION_BUDGET_MS, "validation (read + normalise) took " + (readMs + normaliseMs) + " ms");
        assertTrue(bytes <= FILE_BUDGET_BYTES, "file is " + bytes + " bytes");
    }
}
