package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter.CodeItem;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.export.SdmxCsvExporter;
import org.base.api.service.platform.statistical.export.SdmxCsvExporter.Format;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter.LoadContext;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer.Observation;
import org.base.api.service.platform.statistical.model.PeriodValue;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The last requirement of the simplification plan: what a person typed into the authoring file, what the
 * canonical model stores and what the export publishes must be the same facts.
 *
 * The chain is walked for real — Access file → normalised observations → canonical tables → read back from
 * those tables → SDMX-CSV — and the export built from the database is compared with the export built from the
 * observations. Values keep their exact decimals, periods keep their calendar dates, a missing value keeps its
 * status instead of becoming a zero, and each measure keeps its own status.
 */
class SemanticEquivalenceTest {
    private final InMemoryStatisticalRegistry registry = registry();
    private final SemanticPlan plan = compiler(registry).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
    private JdbcTemplate jdbc;
    private CanonicalObservationWriter writer;
    @TempDir Path dir;

    private final Map<String, Long> ids = new HashMap<>();
    private long id(String key) { return ids.computeIfAbsent(key, k -> (long) ids.size() + 1); }

    private final CanonicalObservationWriter.Binding binding = new CanonicalObservationWriter.Binding() {
        public OptionalLong metricId(Ref measureRef) { return OptionalLong.of(id("m:" + measureRef.wire())); }
        public OptionalLong dimensionId(String code) { return OptionalLong.of(id("d:" + code)); }
        public OptionalLong classificationItemId(Ref codelist, String code) { return OptionalLong.of(id("c:" + codelist.wire() + "|" + code)); }
    };

    @BeforeEach void schema() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MSSQLServer;CASE_INSENSITIVE_IDENTIFIERS=TRUE", true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA [statistics]");
        jdbc.execute("CREATE TABLE [statistics].series(series_id BIGINT IDENTITY PRIMARY KEY, dataset_snapshot_id BIGINT NOT NULL, metric_id BIGINT NOT NULL, series_key_hash CHAR(64) NOT NULL, unit_code VARCHAR(64), status VARCHAR(24) NOT NULL, source_record_id BIGINT, UNIQUE(dataset_snapshot_id,metric_id,series_key_hash))");
        jdbc.execute("CREATE TABLE [statistics].observation(observation_id BIGINT IDENTITY PRIMARY KEY, series_id BIGINT NOT NULL, period_start DATE, period_end DATE, observation_status VARCHAR(24) DEFAULT 'VALID' NOT NULL, numeric_value DECIMAL(28,10), source_record_id BIGINT NOT NULL, is_current BIT DEFAULT 1 NOT NULL)");
        jdbc.execute("CREATE TABLE [statistics].observation_dimension(observation_id BIGINT NOT NULL, dimension_id BIGINT NOT NULL, classification_item_id BIGINT, scalar_code VARCHAR(255), PRIMARY KEY(observation_id,dimension_id))");
        jdbc.execute("CREATE TABLE [statistics].observation_attribute(observation_id BIGINT NOT NULL, attribute_code VARCHAR(128) NOT NULL, value_json VARCHAR(4000) NOT NULL, PRIMARY KEY(observation_id,attribute_code))");
        writer = new CanonicalObservationWriter(jdbc, new TransactionTemplate(new DataSourceTransactionManager(dataSource)), new ObjectMapper());
    }

    @Test void whatWasTypedIsWhatIsStoredIsWhatIsExported() throws Exception {
        // 1. the authoring file, filled the way a person fills it
        Map<Ref, List<CodeItem>> codelists = new LinkedHashMap<>();
        for (Ref ref : List.of(CL_AREA, CL_SEX, CL_OBS_STATUS)) codelists.put(ref, registry.codelist(ref, LABOUR).orElseThrow().codes().stream().sorted().map(c -> new CodeItem(c, c)).toList());
        File file = dir.resolve("filled.accdb").toFile();
        AccessAuthoringAdapter adapter = new AccessAuthoringAdapter();
        adapter.emit(plan, codelists, "ka", file);
        try (Database db = DatabaseBuilder.open(file)) {
            Table table = db.getTable(plan.physical().tables().get(0).name());
            table.addRow("2025-Q2", "GE_TB", "F", new BigDecimal("12000"), "P", new BigDecimal("1800.0000"), null);
            table.addRow("2025-Q2", "GE_TB", "M", null, "M", new BigDecimal("0"), "A");
        }

        // 2. read back and normalise
        AccessAuthoringAdapter.ReadResult read = adapter.read(file, plan);
        assertTrue(read.accepted(), read.issues().toString());
        WideRowNormalizer.Result normalised = new WideRowNormalizer(plan,
                (ref, code) -> registry.codelist(ref, LABOUR).orElseThrow().codes().contains(code), "OBS_STATUS").normalize(read.rows());
        assertTrue(normalised.issues().isEmpty(), normalised.issues().toString());
        assertEquals(4, normalised.observations().size());

        // 3. store in the canonical model
        Map<Long, Long> lineage = Map.of(1L, 501L, 2L, 502L);
        writer.write(plan, normalised.observations(), binding, new LoadContext(9, lineage));

        // 4. read the facts back out of the canonical tables alone
        List<Observation> fromDatabase = readBack();
        assertEquals(sorted(normalised.observations()), sorted(fromDatabase), "the canonical model gives back the same facts it was given");

        // 5. the export is identical whether it is built from the file or from the database
        String fromFile = SdmxCsvExporter.export(plan, normalised.observations(), Format.SDMX_CSV_2_0);
        String fromStorage = SdmxCsvExporter.export(plan, fromDatabase, Format.SDMX_CSV_2_0);
        assertEquals(fromFile, fromStorage);
        assertTrue(fromFile.contains(",2025-Q2,GE_TB,F,12000,1800,P,"), "published with the decimals the contract declares");
        assertTrue(fromFile.contains(",2025-Q2,GE_TB,M,,0,M,A"), "a missing value keeps its status and never becomes a zero:\n" + fromFile);
    }

    /** Rebuilds observations from the canonical tables, using nothing but the plan and what was stored. */
    private List<Observation> readBack() {
        Map<Long, Ref> measureByMetric = new HashMap<>();
        Map<String, Ref> unitByCode = new HashMap<>();
        for (SemanticPlan.PlannedComponent m : plan.withRole(org.base.api.service.platform.statistical.model.Component.Role.MEASURE)) {
            measureByMetric.put(id("m:" + m.measureRef().wire()), m.measureRef());
            if (m.unitRef() != null) unitByCode.put(m.unitRef().code(), m.unitRef());
        }
        Map<Long, String> dimensionByCode = new HashMap<>();
        Map<Long, String> codeByItem = new HashMap<>();
        for (SemanticPlan.PlannedComponent d : plan.withRole(org.base.api.service.platform.statistical.model.Component.Role.DIMENSION)) {
            dimensionByCode.put(id("d:" + d.code()), d.code());
            if (d.representation() instanceof org.base.api.service.platform.statistical.model.Representation.Coded coded)
                for (String code : registry.codelist(coded.codelistRef(), LABOUR).orElseThrow().codes())
                    codeByItem.put(id("c:" + coded.codelistRef().wire() + "|" + code), code);
        }
        String timeCode = plan.withRole(org.base.api.service.platform.statistical.model.Component.Role.DIMENSION).stream()
                .filter(d -> d.representation() instanceof org.base.api.service.platform.statistical.model.Representation.TimePeriod)
                .map(SemanticPlan.PlannedComponent::code).findFirst().orElse(null);
        Set<org.base.api.service.platform.statistical.model.Representation.TimeFormat> formats = plan.withRole(org.base.api.service.platform.statistical.model.Component.Role.DIMENSION).stream()
                .map(SemanticPlan.PlannedComponent::representation)
                .filter(r -> r instanceof org.base.api.service.platform.statistical.model.Representation.TimePeriod)
                .map(r -> ((org.base.api.service.platform.statistical.model.Representation.TimePeriod) r).formats()).findFirst().orElse(Set.of());

        List<Observation> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                SELECT o.observation_id, o.period_start, o.period_end, o.observation_status, o.numeric_value, o.source_record_id, s.metric_id, s.unit_code
                FROM [statistics].observation o JOIN [statistics].series s ON s.series_id = o.series_id
                WHERE s.dataset_snapshot_id = 9 AND o.is_current = 1 ORDER BY o.observation_id""")) {
            long observationId = ((Number) row.get("observation_id")).longValue();
            Map<String, String> dimensions = new TreeMap<>();
            for (Map<String, Object> d : jdbc.queryForList("SELECT dimension_id, classification_item_id, scalar_code FROM [statistics].observation_dimension WHERE observation_id=?", observationId))
                dimensions.put(dimensionByCode.get(((Number) d.get("dimension_id")).longValue()),
                        d.get("classification_item_id") == null ? String.valueOf(d.get("scalar_code")) : codeByItem.get(((Number) d.get("classification_item_id")).longValue()));
            PeriodValue period = null;
            if (row.get("period_start") != null && timeCode != null) {
                LocalDate start = ((java.sql.Date) row.get("period_start")).toLocalDate();
                // The lexical period is the one the contract declares for this interval; both sides derive it the same way.
                for (String candidate : List.of(start.getYear() + "-Q" + ((start.getMonthValue() - 1) / 3 + 1), String.valueOf(start.getYear()))) {
                    PeriodValue parsed = PeriodValue.parse(candidate, formats).orElse(null);
                    if (parsed != null && parsed.start().equals(start) && parsed.endInclusive().equals(((java.sql.Date) row.get("period_end")).toLocalDate())) { period = parsed; break; }
                }
                assertNotNull(period, "a stored interval maps back to exactly one declared period");
                dimensions.put(timeCode, period.lexical());
            }
            Ref measureRef = measureByMetric.get(((Number) row.get("metric_id")).longValue());
            String measureCode = plan.withRole(org.base.api.service.platform.statistical.model.Component.Role.MEASURE).stream()
                    .filter(m -> m.measureRef().equals(measureRef)).map(SemanticPlan.PlannedComponent::code).findFirst().orElseThrow();
            String status = String.valueOf(row.get("observation_status"));
            String statusAttribute = plan.withRole(org.base.api.service.platform.statistical.model.Component.Role.ATTRIBUTE).stream()
                    .filter(a -> measureCode.equals(a.attachment().measure())).map(SemanticPlan.PlannedComponent::code).findFirst().orElse(null);
            Map<String, String> attributes = new TreeMap<>();
            for (Map<String, Object> a : jdbc.queryForList("SELECT attribute_code, value_json FROM [statistics].observation_attribute WHERE observation_id=?", observationId))
                attributes.put(String.valueOf(a.get("attribute_code")), String.valueOf(a.get("value_json")).replaceAll("^\"|\"$", ""));
            out.add(new Observation(0, "", Map.copyOf(dimensions), period, measureCode, measureRef,
                    unitByCode.get(String.valueOf(row.get("unit_code"))), (BigDecimal) row.get("numeric_value"),
                    CanonicalObservationWriter.STATUS_NORMAL.equals(status) ? null : status, statusAttribute, attributes, Set.of()));
        }
        return out;
    }

    /** Compares the facts, not the bookkeeping: source row numbers and keys belong to one load, not to the data. */
    private static List<String> sorted(List<Observation> observations) {
        return observations.stream().map(o -> o.dimensions() + "|" + o.measureCode() + "|" + o.measureRef() + "|" + o.unitRef()
                + "|" + (o.value() == null ? "-" : o.value().stripTrailingZeros().toPlainString())
                // A value with no declared status is a normal value; storage writes that fact as VALID.
                + "|" + (o.status() == null ? "VALID" : o.status()) + "|" + o.attributes()
                + "|" + (o.period() == null ? "-" : o.period().lexical())).sorted().toList();
    }
}
