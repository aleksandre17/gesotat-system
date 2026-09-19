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
import java.sql.Date;
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
    /** Stored when the contract declares no status for a present value: the value is simply a normal one. */
    private static final String STATUS_NORMAL = "VALID";

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

    public Receipt write(SemanticPlan plan, List<Observation> observations, Binding binding, LoadContext context) {
        return transaction.execute(status -> {
            int series = 0, created = 0, unchanged = 0;
            for (Observation o : observations) {
                long metricId = binding.metricId(o.measureRef()).orElseThrow(() -> new WriteConflict("measure is not bound to a metric: " + o.measureRef()));
                Long sourceRecord = context.sourceRecordIdByRow().get(o.sourceRow());
                if (sourceRecord == null) throw new WriteConflict("no lineage record for source row " + o.sourceRow());

                Map<String, String> seriesTuple = new TreeMap<>(o.dimensions());
                plan.withRole(Component.Role.DIMENSION).stream().filter(d -> d.representation() instanceof Representation.TimePeriod)
                        .forEach(d -> seriesTuple.remove(d.code()));
                String seriesKey = CanonicalJson.digest(SERIES_DOMAIN, Map.of("structure", plan.structureRef().wire(), "measure", o.measureRef().wire(), "dimensions", seriesTuple));

                List<Long> existingSeries = dataPlane.queryForList("SELECT series_id FROM [statistics].series WHERE dataset_snapshot_id=? AND metric_id=? AND series_key_hash=?",
                        Long.class, context.datasetSnapshotId(), metricId, seriesKey);
                long seriesId;
                if (existingSeries.isEmpty()) {
                    dataPlane.update("INSERT INTO [statistics].series(dataset_snapshot_id,metric_id,series_key_hash,unit_code,status,source_record_id) VALUES(?,?,?,?,'VALID',?)",
                            context.datasetSnapshotId(), metricId, seriesKey, o.unitRef() == null ? null : o.unitRef().code(), sourceRecord);
                    seriesId = dataPlane.queryForObject("SELECT series_id FROM [statistics].series WHERE dataset_snapshot_id=? AND metric_id=? AND series_key_hash=?",
                            Long.class, context.datasetSnapshotId(), metricId, seriesKey);
                    series++;
                } else seriesId = existingSeries.get(0);

                Date start = o.period() == null ? null : Date.valueOf(o.period().start()), end = o.period() == null ? null : Date.valueOf(o.period().endInclusive());
                String observationStatus = o.status() != null ? o.status() : STATUS_NORMAL;
                List<Map<String, Object>> existing = dataPlane.queryForList("""
                        SELECT observation_id, numeric_value, observation_status FROM [statistics].observation
                        WHERE series_id=? AND is_current=1 AND ((period_start IS NULL AND ? IS NULL) OR period_start=?) AND ((period_end IS NULL AND ? IS NULL) OR period_end=?)""",
                        seriesId, start, start, end, end);
                if (!existing.isEmpty()) {
                    BigDecimal stored = (BigDecimal) existing.get(0).get("numeric_value");
                    boolean sameValue = stored == null ? o.value() == null : o.value() != null && stored.compareTo(o.value()) == 0;
                    if (!sameValue || !observationStatus.equals(existing.get(0).get("observation_status")))
                        throw new WriteConflict("observation already exists with another value or status: " + o.measureCode() + " " + o.dimensions());
                    unchanged++;
                    continue;
                }
                dataPlane.update("INSERT INTO [statistics].observation(series_id,period_start,period_end,observation_status,numeric_value,source_record_id) VALUES(?,?,?,?,?,?)",
                        seriesId, start, end, observationStatus, o.value(), sourceRecord);
                long observationId = dataPlane.queryForObject("SELECT MAX(observation_id) FROM [statistics].observation WHERE series_id=?", Long.class, seriesId);
                dimensions(plan, o, binding, observationId);
                for (Map.Entry<String, String> attribute : o.attributes().entrySet())
                    dataPlane.update("INSERT INTO [statistics].observation_attribute(observation_id,attribute_code,value_json) VALUES(?,?,?)",
                            observationId, attribute.getKey(), json(attribute.getValue()));
                created++;
            }
            return new Receipt(series, created, unchanged);
        });
    }

    private void dimensions(SemanticPlan plan, Observation o, Binding binding, long observationId) {
        for (PlannedComponent d : plan.withRole(Component.Role.DIMENSION)) {
            if (d.representation() instanceof Representation.TimePeriod) continue; // carried by period_start / period_end
            String value = o.dimensions().get(d.code());
            long dimensionId = binding.dimensionId(d.code()).orElseThrow(() -> new WriteConflict("dimension is not bound: " + d.code()));
            Long item = null;
            if (d.representation() instanceof Representation.Coded coded)
                item = binding.classificationItemId(coded.codelistRef(), value).orElseThrow(() -> new WriteConflict("code is not bound: " + d.code() + "=" + value));
            dataPlane.update("INSERT INTO [statistics].observation_dimension(observation_id,dimension_id,classification_item_id,scalar_code) VALUES(?,?,?,?)",
                    observationId, dimensionId, item, value);
        }
    }

    private String json(String value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
