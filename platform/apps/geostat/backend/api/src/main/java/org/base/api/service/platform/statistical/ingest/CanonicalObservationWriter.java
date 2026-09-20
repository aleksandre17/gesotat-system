package org.base.api.service.platform.statistical.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.canonical.CanonicalJson;
import org.base.api.service.platform.statistical.ingest.WideRowNormalizer.Observation;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.TreeMap;

/**
 * Data-plane materialisation of normalised observations into the existing canonical model
 * ({@code statistics.series / observation / observation_dimension / observation_attribute}); no table is added.
 * A series is one measure over one non-time dimension tuple; an observation is that series at one period.
 *
 * Contract-driven: nothing here names a site, a dataset or a measure. It writes what the legacy writer left
 * at defaults — the observation status, the period end and the attributes (register §4.3).
 *
 * Idempotent: replaying the same load changes nothing; the same key with another value is a conflict and the
 * whole load rolls back, because a silent overwrite and a silent duplicate are both data loss.
 */
public final class CanonicalObservationWriter {
    public static final String SERIES_DOMAIN = "geostat.stat-series-key.v1";
    /**
     * Stored when the author declared no status: the value is simply a normal one. It is the platform's marker,
     * never a code of a contract codelist, so reading it back means "no declared status" and it is not published.
     */
    public static final String STATUS_NORMAL = "VALID";

    /** Port to the identifiers the canonical tables use. Absent means the contract is not bound for loading. */
    public interface Binding {
        OptionalLong metricId(Ref measureRef);
        OptionalLong dimensionId(String componentCode);
        OptionalLong classificationItemId(Ref codelistRef, String code);
    }

    public record LoadContext(long datasetSnapshotId, Map<Long, Long> sourceRecordIdByRow) { }

    public record Receipt(int seriesCreated, int observationsCreated, int observationsUnchanged) { }

    public static final class WriteConflict extends RuntimeException {
        public WriteConflict(String message) { super(message); }
    }

    private final JdbcTemplate dataPlane;
    private final TransactionTemplate transaction;
    private final ObjectMapper mapper;

    public CanonicalObservationWriter(JdbcTemplate dataPlane, TransactionTemplate transaction, ObjectMapper mapper) {
        this.dataPlane = dataPlane;
        this.transaction = transaction;
        this.mapper = mapper;
    }

    /** Rows per JDBC batch: large enough to amortise round trips, small enough for the driver's parameter budget. */
    public static final int BATCH = 1_000;

    /** One observation, fully resolved before anything is written. */
    private record Planned(Observation source, long metricId, String seriesKey, String unitCode, LocalDate start, LocalDate end,
                           String status, long sourceRecord, List<Object[]> dimensionRows) {
        String seriesIdentity() { return metricId + "|" + seriesKey; }
    }

    /**
     * Resolve everything, then write in set-based phases: series, observations, dimensions, attributes.
     * Every binding and every conflict is decided before the first INSERT, so a refused load writes nothing even
     * before the transaction rolls back; a load of N observations costs a constant number of reads and N / BATCH
     * round trips per table instead of several statements per observation.
     */
    public Receipt write(SemanticPlan plan, List<Observation> observations, Binding binding, LoadContext context) {
        return transaction.execute(status -> {
            List<Planned> planned = resolve(plan, observations, binding, context);
            long snapshot = context.datasetSnapshotId();

            // Series: existing ones are reused, missing ones are inserted once per identity.
            Map<String, Long> seriesIds = seriesIds(snapshot);
            Map<String, Planned> newSeries = new LinkedHashMap<>();
            for (Planned p : planned) if (!seriesIds.containsKey(p.seriesIdentity())) newSeries.putIfAbsent(p.seriesIdentity(), p);
            batch("INSERT INTO [statistics].series(dataset_snapshot_id,metric_id,series_key_hash,unit_code,status,source_record_id) VALUES(?,?,?,?,'VALID',?)",
                    newSeries.values().stream().map(p -> new Object[]{snapshot, p.metricId(), p.seriesKey(), p.unitCode(), p.sourceRecord()}).toList());
            if (!newSeries.isEmpty()) seriesIds = seriesIds(snapshot);

            // Observations: identical replays are counted, a changed value or status refuses the whole load.
            Map<String, Object[]> existing = currentObservations(snapshot);
            List<Planned> inserts = new ArrayList<>();
            int unchanged = 0;
            for (Planned p : planned) {
                Object[] stored = existing.get(observationKey(seriesIds.get(p.seriesIdentity()), p.start(), p.end()));
                if (stored == null) { inserts.add(p); continue; }
                BigDecimal value = (BigDecimal) stored[1];
                boolean sameValue = value == null ? p.source().value() == null : p.source().value() != null && value.compareTo(p.source().value()) == 0;
                if (!sameValue || !p.status().equals(stored[2]))
                    throw new WriteConflict("observation already exists with another value or status: " + p.source().measureCode() + " " + p.source().dimensions());
                unchanged++;
            }
            final Map<String, Long> series = seriesIds;
            batch("INSERT INTO [statistics].observation(series_id,period_start,period_end,observation_status,numeric_value,source_record_id) VALUES(?,?,?,?,?,?)",
                    inserts.stream().map(p -> new Object[]{series.get(p.seriesIdentity()), p.start(), p.end(), p.status(), p.source().value(), p.sourceRecord()}).toList());

            // Dimensions and attributes need the generated observation ids: one read maps key -> id.
            Map<String, Object[]> written = inserts.isEmpty() ? Map.of() : currentObservations(snapshot);
            List<Object[]> dimensionRows = new ArrayList<>(), attributeRows = new ArrayList<>();
            for (Planned p : inserts) {
                long id = (Long) written.get(observationKey(series.get(p.seriesIdentity()), p.start(), p.end()))[0];
                for (Object[] d : p.dimensionRows()) dimensionRows.add(new Object[]{id, d[0], d[1], d[2]});
                for (Map.Entry<String, String> a : p.source().attributes().entrySet()) attributeRows.add(new Object[]{id, a.getKey(), json(a.getValue())});
            }
            batch("INSERT INTO [statistics].observation_dimension(observation_id,dimension_id,classification_item_id,scalar_code) VALUES(?,?,?,?)", dimensionRows);
            batch("INSERT INTO [statistics].observation_attribute(observation_id,attribute_code,value_json) VALUES(?,?,?)", attributeRows);
            return new Receipt(newSeries.size(), inserts.size(), unchanged);
        });
    }

    /** Pure resolution; each distinct binding is looked up once and cached for the whole load. */
    private List<Planned> resolve(SemanticPlan plan, List<Observation> observations, Binding binding, LoadContext context) {
        Map<Ref, Long> metrics = new HashMap<>();
        Map<String, Long> dimensionIds = new HashMap<>();
        Map<String, Long> items = new HashMap<>();
        List<PlannedComponent> keyDimensions = plan.withRole(Component.Role.DIMENSION).stream()
                .filter(d -> !(d.representation() instanceof Representation.TimePeriod)).toList(); // time lives in the period columns
        List<Planned> out = new ArrayList<>(observations.size());
        for (Observation o : observations) {
            long metricId = metrics.computeIfAbsent(o.measureRef(), ref -> binding.metricId(ref).orElseThrow(() -> new WriteConflict("measure is not bound to a metric: " + ref)));
            Long sourceRecord = context.sourceRecordIdByRow().get(o.sourceRow());
            if (sourceRecord == null) throw new WriteConflict("no lineage record for source row " + o.sourceRow());
            Map<String, String> seriesTuple = new TreeMap<>();
            List<Object[]> dimensionRows = new ArrayList<>(keyDimensions.size());
            for (PlannedComponent d : keyDimensions) {
                String value = o.dimensions().get(d.code());
                seriesTuple.put(d.code(), value);
                long dimensionId = dimensionIds.computeIfAbsent(d.code(), code -> binding.dimensionId(code).orElseThrow(() -> new WriteConflict("dimension is not bound: " + code)));
                Long item = null;
                if (d.representation() instanceof Representation.Coded coded)
                    item = items.computeIfAbsent(coded.codelistRef().wire() + "|" + value, k -> binding.classificationItemId(coded.codelistRef(), value)
                            .orElseThrow(() -> new WriteConflict("code is not bound: " + d.code() + "=" + value)));
                dimensionRows.add(new Object[]{dimensionId, item, value});
            }
            String seriesKey = CanonicalJson.digest(SERIES_DOMAIN, Map.of("structure", plan.structureRef().wire(), "measure", o.measureRef().wire(), "dimensions", seriesTuple));
            out.add(new Planned(o, metricId, seriesKey, o.unitRef() == null ? null : o.unitRef().code(),
                    o.period() == null ? null : o.period().start(), o.period() == null ? null : o.period().endInclusive(),
                    o.status() != null ? o.status() : STATUS_NORMAL, sourceRecord, dimensionRows));
        }
        return out;
    }

    private Map<String, Long> seriesIds(long snapshot) {
        Map<String, Long> ids = new HashMap<>();
        dataPlane.query("SELECT series_id, metric_id, series_key_hash FROM [statistics].series WHERE dataset_snapshot_id=?",
                rs -> { ids.put(rs.getLong(2) + "|" + rs.getString(3), rs.getLong(1)); }, snapshot);
        return ids;
    }

    /** key -> {observation_id, numeric_value, observation_status} for the current observations of one snapshot. */
    private Map<String, Object[]> currentObservations(long snapshot) {
        Map<String, Object[]> rows = new HashMap<>();
        dataPlane.query("""
                SELECT o.observation_id, o.series_id, o.period_start, o.period_end, o.numeric_value, o.observation_status
                FROM [statistics].observation o JOIN [statistics].series s ON s.series_id=o.series_id
                WHERE s.dataset_snapshot_id=? AND o.is_current=1""",
                rs -> { rows.put(observationKey(rs.getLong(2), rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class)), new Object[]{rs.getLong(1), rs.getBigDecimal(5), rs.getString(6)}); }, snapshot);
        return rows;
    }

    /** Calendar dates end to end: a java.sql.Date passes through the JVM time zone, which shifts historical dates by a day. */
    private static String observationKey(Long seriesId, LocalDate start, LocalDate end) { return seriesId + "|" + start + "|" + end; }


    private void batch(String sql, List<Object[]> rows) {
        for (int from = 0; from < rows.size(); from += BATCH)
            dataPlane.batchUpdate(sql, rows.subList(from, Math.min(rows.size(), from + BATCH)));
    }

    private String json(String value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
