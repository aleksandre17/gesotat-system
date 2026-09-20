package org.base.api.service.platform.statistical.registry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Component.Attachment;
import org.base.api.service.platform.statistical.model.Component.AttachmentLevel;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.model.Representation.TimeFormat;
import org.base.api.service.platform.statistical.model.SemVer;
import org.base.api.service.platform.statistical.model.StructureDefinition;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Control-plane adapter of {@link StatisticalRegistry}. Identity, lifecycle and visibility come from
 * {@code platform.statistical_reference}; meaning stays in the existing registry tables it points at.
 * Read-only, fixed parameterised SQL. Fail closed: an entry that is invisible, incomplete or ambiguous does
 * not resolve, and the three cases are indistinguishable to the caller.
 */
public final class JdbcStatisticalRegistry implements StatisticalRegistry {
    private static final String VISIBLE = """
            FROM platform.statistical_reference r
            JOIN platform.contract_namespace n ON n.namespace_id = r.namespace_id
            LEFT JOIN platform.data_product p ON p.product_id = r.owner_product_id
            WHERE (r.owner_product_id IS NULL OR p.product_code = ?)
            """;
    private static final String BY_IDENTITY = "SELECT r.reference_id, r.lifecycle_status, r.target_type, r.target_id " + VISIBLE
            + " AND r.kind = ? AND n.namespace_code = ? AND r.code = ? AND r.version_major = ? AND r.version_minor = ? AND r.version_patch = ?";
    private static final String REF_COLUMNS = "SELECT r.kind, n.namespace_code, r.code, r.version_major, r.version_minor, r.version_patch ";

    private record Row(long id, Lifecycle lifecycle, String targetType, Long targetId) { }

    private final JdbcTemplate control;
    private final ObjectMapper mapper;

    public JdbcStatisticalRegistry(JdbcTemplate control, ObjectMapper mapper) {
        this.control = control;
        this.mapper = mapper;
    }

    @Override public Optional<Lifecycle> lifecycle(Ref ref, Scope scope) { return row(ref, scope).map(Row::lifecycle); }

    @Override public Optional<MeasureDefinition> measure(Ref ref, Scope scope) {
        if (ref.kind() != Ref.Kind.MEASURE) return Optional.empty();
        return row(ref, scope).flatMap(row -> one(control.query(
                "SELECT numeric_precision, numeric_scale, approximate_numeric, unit_id, concept_reference_id, aggregation_default FROM platform.measure WHERE measure_id = ?",
                (rs, i) -> {
                    int precision = rs.getInt(1); boolean noPrecision = rs.wasNull();
                    int scale = rs.getInt(2); boolean noScale = rs.wasNull();
                    boolean approximate = rs.getBoolean(3);
                    long unitId = rs.getLong(4); boolean noUnit = rs.wasNull();
                    long conceptId = rs.getLong(5); boolean noConcept = rs.wasNull();
                    if (noConcept || (!approximate && (noPrecision || noScale))) return Optional.<MeasureDefinition>empty();
                    Optional<Ref> concept = byId(conceptId, scope);
                    Optional<Ref> unit = noUnit ? Optional.empty() : byTarget("STATISTICAL_UNIT", unitId, scope);
                    if (concept.isEmpty() || (!noUnit && unit.isEmpty())) return Optional.<MeasureDefinition>empty();
                    Aggregation aggregation;
                    try { aggregation = Aggregation.valueOf(String.valueOf(rs.getString(6))); }
                    catch (IllegalArgumentException unknownToThisProfile) { aggregation = Aggregation.NONE; } // an unknown rule is never a licence to combine
                    return Optional.of(new MeasureDefinition(ref, concept.get(), new Representation.Numeric(precision, scale, approximate), unit.orElse(null), aggregation));
                }, row.targetId())).flatMap(o -> o));
    }

    @Override public Optional<Codelist> codelist(Ref ref, Scope scope) {
        if (ref.kind() != Ref.Kind.CODELIST) return Optional.empty();
        return row(ref, scope).map(row -> new Codelist(ref, new HashSet<>(control.queryForList(
                "SELECT code FROM platform.classification_item WHERE classification_version_id = ? AND status = 'ACTIVE'", String.class, row.targetId()))));
    }

    @Override public Optional<StructureDefinition> structure(Ref ref, Scope scope) {
        if (ref.kind() != Ref.Kind.DSD) return Optional.empty();
        Optional<Row> row = row(ref, scope);
        if (row.isEmpty()) return Optional.empty();
        List<Map<String, Object>> rows = control.queryForList("""
                SELECT component_code, component_role, required, measure_id, classification_version_id, attachment_level,
                       constraint_json, concept_reference_id, representation_type
                FROM platform.statistical_component WHERE dsd_id = ? ORDER BY component_order""", row.get().targetId());
        List<Component> components = new ArrayList<>();
        for (Map<String, Object> c : rows) {
            Optional<Component> component = component(c, scope);
            if (component.isEmpty()) return Optional.empty(); // one unreadable component makes the structure unusable
            components.add(component.get());
        }
        return components.isEmpty() ? Optional.empty() : Optional.of(new StructureDefinition(ref, components));
    }

    private Optional<Component> component(Map<String, Object> c, Scope scope) {
        String code = (String) c.get("component_code");
        boolean required = truthy(c.get("required"));
        Component.Role role;
        try { role = Component.Role.valueOf(String.valueOf(c.get("component_role"))); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
        if (role == Component.Role.MEASURE)
            return c.get("measure_id") == null ? Optional.empty()
                    : byTarget("MEASURE", ((Number) c.get("measure_id")).longValue(), scope).map(m -> Component.measure(code, m, required));

        if (c.get("concept_reference_id") == null) return Optional.empty();
        Optional<Ref> concept = byId(((Number) c.get("concept_reference_id")).longValue(), scope);
        JsonNode facets = facets((String) c.get("constraint_json"));
        Optional<Representation> representation = representation(c, facets, scope);
        if (concept.isEmpty() || representation.isEmpty()) return Optional.empty();
        if (role == Component.Role.DIMENSION) return Optional.of(Component.dimension(code, concept.get(), representation.get()));

        AttachmentLevel level;
        try { level = AttachmentLevel.valueOf(String.valueOf(c.get("attachment_level"))); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
        List<String> dimensions = new ArrayList<>();
        facets.path("attachment").path("dimensions").forEach(d -> dimensions.add(d.asText()));
        String measure = facets.path("attachment").path("measure").isTextual() ? facets.path("attachment").path("measure").asText() : null;
        return Optional.of(Component.attribute(code, concept.get(), representation.get(), new Attachment(level, dimensions, measure), required));
    }

    private Optional<Representation> representation(Map<String, Object> c, JsonNode facets, Scope scope) {
        try {
            switch (String.valueOf(c.get("representation_type"))) {
                case "CODED" -> {
                    if (c.get("classification_version_id") == null) return Optional.empty();
                    return byTarget("CLASSIFICATION_VERSION", ((Number) c.get("classification_version_id")).longValue(), scope).map(Representation.Coded::new);
                }
                case "TIME_PERIOD" -> {
                    Set<TimeFormat> formats = EnumSet.noneOf(TimeFormat.class);
                    facets.path("formats").forEach(f -> formats.add(TimeFormat.valueOf(f.asText())));
                    return Optional.of(new Representation.TimePeriod(formats));
                }
                case "INTEGER" -> {
                    if (!facets.path("min").isIntegralNumber() || !facets.path("max").isIntegralNumber()) return Optional.empty();
                    return Optional.of(new Representation.IntegerRange(facets.get("min").asLong(), facets.get("max").asLong()));
                }
                case "TEXT" -> {
                    int max = facets.path("maxLength").asInt(0);
                    return max < 1 || max > 255 ? Optional.empty() : Optional.of(new Representation.BoundedText(max));
                }
                default -> { return Optional.empty(); }
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<Row> row(Ref ref, Scope scope) {
        return one(control.query(BY_IDENTITY, (rs, i) -> new Row(rs.getLong(1), Lifecycle.valueOf(rs.getString(2)), rs.getString(3),
                        rs.getObject(4) == null ? null : rs.getLong(4)),
                scope.productCode(), ref.kind().name(), ref.namespace(), ref.code(), ref.version().major(), ref.version().minor(), ref.version().patch()));
    }

    private Optional<Ref> byId(long referenceId, Scope scope) {
        return one(control.query(REF_COLUMNS + VISIBLE + " AND r.reference_id = ?", JdbcStatisticalRegistry::ref, scope.productCode(), referenceId));
    }

    /** The pinned version of a target row: exactly one visible, non-superseded reference, or nothing. */
    private Optional<Ref> byTarget(String targetType, long targetId, Scope scope) {
        return one(control.query(REF_COLUMNS + VISIBLE + " AND r.target_type = ? AND r.target_id = ? AND r.lifecycle_status <> 'SUPERSEDED'",
                JdbcStatisticalRegistry::ref, scope.productCode(), targetType, targetId));
    }

    private static Ref ref(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new Ref(Ref.Kind.valueOf(rs.getString(1)), rs.getString(2), rs.getString(3), new SemVer(rs.getInt(4), rs.getInt(5), rs.getInt(6)));
    }

    private JsonNode facets(String json) {
        try { return json == null ? mapper.createObjectNode() : mapper.readTree(json); }
        catch (Exception e) { return mapper.createObjectNode(); }
    }

    private static boolean truthy(Object value) { return value instanceof Boolean b ? b : value instanceof Number n && n.intValue() != 0; }

    private static <T> Optional<T> one(List<T> rows) { return rows.size() == 1 ? Optional.ofNullable(rows.get(0)) : Optional.empty(); }
}
