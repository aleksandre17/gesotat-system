package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter.LoadContext;
import org.base.api.service.platform.statistical.ingest.CanonicalObservationWriter.Receipt;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** The writer against the column shape of 002_data_plane.sql (portable subset, H2 in SQL Server mode). */
class CanonicalObservationWriterTest {
    private final InMemoryStatisticalRegistry registry = registry();
    private final SemanticPlan labour = compiler(registry).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
    private final SemanticPlan land = compiler(registry).compile(landDraft(false), LAND, Mode.APPROVAL).plan().orElseThrow();
    private JdbcTemplate jdbc;
    private CanonicalObservationWriter writer;

    private final CanonicalObservationWriter.Binding binding = new CanonicalObservationWriter.Binding() {
        private final Map<String, Long> ids = new HashMap<>();
        private long id(String key) { return ids.computeIfAbsent(key, k -> (long) ids.size() + 1); }
        public OptionalLong metricId(Ref measureRef) { return measureRef.code().equals("UNBOUND") ? OptionalLong.empty() : OptionalLong.of(id("m:" + measureRef.wire())); }
        public OptionalLong dimensionId(String code) { return OptionalLong.of(id("d:" + code)); }
        public OptionalLong classificationItemId(Ref codelist, String code) { return OptionalLong.of(id("c:" + codelist.wire() + code)); }
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

    private List<WideRowNormalizer.Observation> normalise(SemanticPlan plan, org.base.api.service.platform.statistical.registry.StatisticalRegistry.Scope scope, Map<String, Object> row) {
        WideRowNormalizer.Result result = new WideRowNormalizer(plan, (ref, code) -> registry.codelist(ref, scope).orElseThrow().codes().contains(code), "OBS_STATUS").normalize(List.of(row));
        assertTrue(result.issues().isEmpty(), result.issues().toString());
        return result.observations();
    }

    private static Map<String, Object> labourRow(Object employed, String employedStatus) {
        Map<String, Object> row = new HashMap<>(Map.of("TIME_PERIOD", "2025-Q2", "REF_AREA", "GE_TB", "SEX", "F", "UNEMPLOYED", 1800));
        row.put("EMPLOYED", employed);
        row.put("EMPLOYED_STATUS", employedStatus);
        return row;
    }

    @Test void writesWhatTheLegacyWriterLeftAtDefaults() {
        Receipt receipt = writer.write(labour, normalise(labour, LABOUR, labourRow(12000, "P")), binding, new LoadContext(7, Map.of(1L, 500L)));
        assertEquals(new Receipt(2, 2, 0), receipt);
        Map<String, Object> employed = jdbc.queryForMap("SELECT o.* FROM [statistics].observation o WHERE o.observation_status='P'");
        assertEquals(java.sql.Date.valueOf("2025-04-01"), employed.get("period_start"));
        assertEquals(java.sql.Date.valueOf("2025-06-30"), employed.get("period_end"), "period end is stored, not inferred later");
        assertEquals(0, new BigDecimal("12000").compareTo((BigDecimal) employed.get("numeric_value")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM [statistics].observation WHERE observation_status='VALID'", Integer.class), "the other measure keeps its own status");
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM [statistics].observation_dimension", Integer.class), "two non-time dimensions per observation; time lives in the period columns");
        assertEquals("PERSONS", jdbc.queryForObject("SELECT DISTINCT unit_code FROM [statistics].series", String.class));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(DISTINCT metric_id) FROM [statistics].series", Integer.class), "measure identity is the series metric");
    }

    @Test void missingValueKeepsItsStatusAndNoNumber() {
        writer.write(labour, normalise(labour, LABOUR, labourRow(null, "M")), binding, new LoadContext(7, Map.of(1L, 500L)));
        Map<String, Object> missing = jdbc.queryForMap("SELECT numeric_value, observation_status FROM [statistics].observation WHERE observation_status='M'");
        assertNull(missing.get("numeric_value"), "missing is not zero");
    }

    @Test void replayIsANoOpAndAChangedValueIsAConflictThatRollsBack() {
        LoadContext context = new LoadContext(7, Map.of(1L, 500L));
        writer.write(labour, normalise(labour, LABOUR, labourRow(12000, "P")), binding, context);
        assertEquals(new Receipt(0, 0, 2), writer.write(labour, normalise(labour, LABOUR, labourRow(12000, "P")), binding, context));
        assertEquals(new Receipt(0, 0, 2), writer.write(labour, normalise(labour, LABOUR, labourRow("12000.0", "P")), binding, context), "numerically equal is equal");

        Map<String, Object> changed = labourRow(12001, "P");
        changed.put("TIME_PERIOD", "2025-Q3"); // a new period first, then the conflicting one, in one load
        List<WideRowNormalizer.Observation> mixed = new java.util.ArrayList<>(normalise(labour, LABOUR, changed));
        mixed.addAll(normalise(labour, LABOUR, labourRow(12001, "P")));
        assertThrows(CanonicalObservationWriter.WriteConflict.class, () -> writer.write(labour, mixed, binding, context));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM [statistics].observation", Integer.class), "the partial load rolled back");
    }

    @Test void nonTimeDatasetAndExactDecimalSurviveStorage() {
        writer.write(land, normalise(land, LAND, Map.of("LAND_USE", "FOREST", "AREA_SIZE", "999999999999999999.9999999999")), binding, new LoadContext(9, Map.of(1L, 1L)));
        Map<String, Object> stored = jdbc.queryForMap("SELECT period_start, period_end, numeric_value FROM [statistics].observation");
        assertNull(stored.get("period_start"));
        assertNull(stored.get("period_end"));
        assertEquals(new BigDecimal("999999999999999999.9999999999"), stored.get("numeric_value"));
        assertEquals(new Receipt(0, 0, 1), writer.write(land, normalise(land, LAND, Map.of("LAND_USE", "FOREST", "AREA_SIZE", "999999999999999999.9999999999")), binding, new LoadContext(9, Map.of(1L, 1L))), "replay without a period");
    }

    @Test void aLoadWithoutLineageOrBindingIsRefused() {
        List<WideRowNormalizer.Observation> observations = normalise(labour, LABOUR, labourRow(1, null));
        assertThrows(CanonicalObservationWriter.WriteConflict.class, () -> writer.write(labour, observations, binding, new LoadContext(7, Map.of())));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM [statistics].series", Integer.class));
    }

    @Test void loadsAcrossSeveralBatchesAndReplaysWithoutWriting() {
        List<WideRowNormalizer.Observation> all = new java.util.ArrayList<>();
        Map<Long, Long> lineage = new HashMap<>();
        long row = 0;
        for (int year = 1700; year < 2026; year++) for (String area : new String[]{"GE", "GE_TB", "GE_KA"}) for (String sex : new String[]{"F", "M", "_T"}) {
            Map<String, Object> r = new HashMap<>(Map.of("TIME_PERIOD", String.valueOf(year), "REF_AREA", area, "SEX", sex, "EMPLOYED", year, "UNEMPLOYED", 1));
            for (WideRowNormalizer.Observation o : normalise(labour, LABOUR, r)) {
                all.add(new WideRowNormalizer.Observation(++row, o.observationKey(), o.dimensions(), o.period(), o.measureCode(), o.measureRef(), o.unitRef(),
                        o.value(), o.status(), o.statusAttribute(), o.attributes(), o.overriddenConstants()));
                lineage.put(row, row);
            }
        }
        assertTrue(all.size() > 3 * CanonicalObservationWriter.BATCH, "the load must cross batch boundaries");
        Receipt first = writer.write(labour, all, binding, new LoadContext(7, lineage));
        assertEquals(9 * 2, first.seriesCreated(), "one series per measure and non-time tuple");
        assertEquals(all.size(), first.observationsCreated());
        assertEquals(all.size() * 2, jdbc.queryForObject("SELECT COUNT(*) FROM [statistics].observation_dimension", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM [statistics].observation o LEFT JOIN [statistics].observation_dimension d ON d.observation_id=o.observation_id WHERE d.observation_id IS NULL", Integer.class),
                "every observation received its dimensions, whatever batch it was in");
        assertEquals(new Receipt(0, 0, all.size()), writer.write(labour, all, binding, new LoadContext(7, lineage)), "replay writes nothing");
    }
}
