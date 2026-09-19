package org.base.api.service.platform.statistical.ingest;

import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Binds an approved plan to the identifiers of the existing governed model, so a statistical contract loads
 * through the same dataset / dataset-version / ingestion-contract / metric / dimension rows as every other data
 * family instead of a parallel one. Idempotent: binding the same approved revision twice changes nothing; a new
 * approved revision of the dataset gets the next dataset version. Control Plane only; fixed parameterised SQL.
 */
public final class StatisticalBindingService {
    public static final String FAMILY = "STATISTICAL";
    public static final String SOURCE_SYSTEM = "STATISTICAL_AUTHORING";
    public static final String FORMAT_PROFILE = "STAT_AUTHORING_V1";

    /** Identifiers a data-plane load needs; all of them already exist when this record is returned. */
    public record Bound(long productId, long datasetId, long datasetVersionId, long ingestionContractId) { }

    private final JdbcTemplate control;
    private final TransactionTemplate transaction;

    public StatisticalBindingService(JdbcTemplate control, TransactionTemplate transaction) {
        this.control = control;
        this.transaction = transaction;
    }

    public Bound bind(SemanticPlan plan, String productCode) {
        return transaction.execute(status -> {
            long productId = id("SELECT product_id FROM platform.data_product WHERE product_code=?", productCode)
                    .orElseThrow(() -> new IllegalStateException("unknown product"));
            String grain = plan.withRole(Component.Role.DIMENSION).stream().map(PlannedComponent::code).collect(Collectors.joining(" x "));
            long datasetId = ensure("SELECT dataset_id FROM platform.dataset WHERE product_id=? AND dataset_code=?", List.of(productId, plan.datasetCode()),
                    "INSERT INTO platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status) VALUES(?,?,?,?,'APPROVED')",
                    List.of(productId, plan.datasetCode(), FAMILY, grain));
            long versionId = ensure("SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=? AND contract_checksum=?", List.of(datasetId, plan.revisionDigest()),
                    "INSERT INTO platform.dataset_version(dataset_id,version,status,contract_checksum) SELECT ?, COALESCE(MAX(version),0)+1, 'APPROVED', ? FROM platform.dataset_version WHERE dataset_id=?",
                    List.of(datasetId, plan.revisionDigest(), datasetId));
            long sourceId = ensure("SELECT source_system_id FROM platform.source_system WHERE source_code=?", List.of(SOURCE_SYSTEM),
                    "INSERT INTO platform.source_system(source_code,source_type,title,trust_level,enabled) VALUES(?,'FILE','Statistical authoring file','UNTRUSTED',1)", List.of(SOURCE_SYSTEM));
            long contractId = ensure("SELECT contract_id FROM platform.ingestion_contract WHERE source_system_id=? AND dataset_id=?", List.of(sourceId, datasetId),
                    "INSERT INTO platform.ingestion_contract(source_system_id,dataset_id,format_profile,ingestion_method,auto_publish,status,contract_code) VALUES(?,?,?,'UPLOAD',0,'APPROVED',?)",
                    List.of(sourceId, datasetId, FORMAT_PROFILE, productCode + "." + plan.datasetCode()));

            for (PlannedComponent d : plan.withRole(Component.Role.DIMENSION)) {
                if (d.representation() instanceof Representation.TimePeriod) continue; // time lives in the period columns
                ensure("SELECT dimension_id FROM platform.dimension WHERE dimension_code=?", List.of(dimensionCode(d)),
                        "INSERT INTO platform.dimension(dimension_code,value_type,title_ka) VALUES(?,?,?)",
                        List.of(dimensionCode(d), d.representation().logicalType(), caption(plan, d)));
            }
            for (PlannedComponent m : plan.withRole(Component.Role.MEASURE)) {
                long measureId = measureId(m.measureRef()).orElseThrow(() -> new IllegalStateException("measure reference has no registry row: " + m.measureRef()));
                ensure("SELECT metric_id FROM platform.metric WHERE source_dataset_id=? AND measure_id=?", List.of(datasetId, measureId),
                        "INSERT INTO platform.metric(metric_code,source_dataset_id,measure_id,aggregation,status) VALUES(?,?,?,'NONE','APPROVED')",
                        List.of(metricCode(productCode, plan, m), datasetId, measureId));
            }
            return new Bound(productId, datasetId, versionId, contractId);
        });
    }

    /** Lookup port of the writer, scoped to one bound dataset. */
    public CanonicalObservationWriter.Binding binding(SemanticPlan plan, Bound bound) {
        Map<String, PlannedComponent> byCode = plan.components().stream().collect(Collectors.toMap(PlannedComponent::code, c -> c));
        return new CanonicalObservationWriter.Binding() {
            @Override public OptionalLong metricId(Ref measureRef) {
                OptionalLong measure = measureId(measureRef);
                return measure.isEmpty() ? OptionalLong.empty()
                        : id("SELECT metric_id FROM platform.metric WHERE source_dataset_id=? AND measure_id=?", bound.datasetId(), measure.getAsLong());
            }
            @Override public OptionalLong dimensionId(String componentCode) {
                PlannedComponent c = byCode.get(componentCode);
                return c == null ? OptionalLong.empty() : id("SELECT dimension_id FROM platform.dimension WHERE dimension_code=?", dimensionCode(c));
            }
            @Override public OptionalLong classificationItemId(Ref codelistRef, String code) {
                return id("""
                        SELECT i.classification_item_id FROM platform.classification_item i
                        JOIN platform.statistical_reference r ON r.target_type='CLASSIFICATION_VERSION' AND r.target_id=i.classification_version_id
                        JOIN platform.contract_namespace n ON n.namespace_id=r.namespace_id
                        WHERE r.kind='CODELIST' AND n.namespace_code=? AND r.code=? AND r.version_major=? AND r.version_minor=? AND r.version_patch=? AND i.code=?""",
                        codelistRef.namespace(), codelistRef.code(), codelistRef.version().major(), codelistRef.version().minor(), codelistRef.version().patch(), code);
            }
        };
    }

    private OptionalLong measureId(Ref ref) {
        return id("""
                SELECT r.target_id FROM platform.statistical_reference r JOIN platform.contract_namespace n ON n.namespace_id=r.namespace_id
                WHERE r.kind='MEASURE' AND n.namespace_code=? AND r.code=? AND r.version_major=? AND r.version_minor=? AND r.version_patch=?""",
                ref.namespace(), ref.code(), ref.version().major(), ref.version().minor(), ref.version().patch());
    }

    /** A dimension is the concept, shared by every dataset that uses it; the version is part of its identity. */
    private static String dimensionCode(PlannedComponent c) { return bounded(c.conceptRef().namespace() + "." + c.conceptRef().code() + "." + c.conceptRef().version()); }

    private static String metricCode(String productCode, SemanticPlan plan, PlannedComponent m) { return bounded(productCode + "." + plan.datasetCode() + "." + m.code()); }

    private static String bounded(String code) {
        if (code.length() > 120) throw new IllegalStateException("binding code exceeds 120 characters: " + code);
        return code;
    }

    private static String caption(SemanticPlan plan, PlannedComponent c) {
        String caption = plan.captions().getOrDefault(c.code(), Map.of()).get("ka");
        return caption == null || caption.isBlank() ? c.code() : caption;
    }

    private long ensure(String select, List<Object> selectArgs, String insert, List<Object> insertArgs) {
        OptionalLong existing = id(select, selectArgs.toArray());
        if (existing.isPresent()) return existing.getAsLong();
        control.update(insert, insertArgs.toArray());
        return id(select, selectArgs.toArray()).orElseThrow(() -> new IllegalStateException("binding row was not created"));
    }

    private OptionalLong id(String sql, Object... args) {
        List<Long> rows = control.queryForList(sql, Long.class, args);
        return rows.size() == 1 && rows.get(0) != null ? OptionalLong.of(rows.get(0)) : OptionalLong.empty();
    }
}
