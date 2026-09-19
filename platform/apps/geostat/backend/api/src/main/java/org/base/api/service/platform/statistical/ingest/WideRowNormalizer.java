package org.base.api.service.platform.statistical.ingest;

import org.base.api.service.platform.statistical.canonical.CanonicalJson;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.ContractDraft.ErrorMode;
import org.base.api.service.platform.statistical.model.PeriodValue;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Ingestion core: one wide authoring row (a dimension tuple with N measures) becomes N canonical
 * observations. Deterministic and order-independent; nothing is rounded, defaulted or dropped silently.
 * Input rows are keyed by component code — the provider adapter has already mapped physical names.
 */
public final class WideRowNormalizer {
    private static final Pattern DECIMAL = Pattern.compile("[+-]?\\d+(\\.\\d+)?");
    public static final String KEY_DOMAIN = "geostat.stat-observation-key.v1";

    /** Membership port over the codelists pinned by the plan. */
    public interface CodeMembership { boolean contains(Ref codelistRef, String code); }

    public enum IssueCode {
        UNDECLARED_COLUMN, MISSING_DIMENSION, INVALID_CODE, INVALID_PERIOD, INVALID_VALUE, INEXACT_INPUT,
        NUMERIC_OVERFLOW, SCALE_EXCEEDED, VALUE_WITHOUT_STATUS_MISSING, CONSTANT_NOT_OVERRIDABLE, DUPLICATE_OBSERVATION
    }

    public record RowIssue(long row, String component, IssueCode code, String message) { }

    public record Observation(long sourceRow, String observationKey, Map<String, String> dimensions, PeriodValue period,
                              String measureCode, Ref measureRef, Ref unitRef, BigDecimal value, String status,
                              Map<String, String> attributes, Set<String> overriddenConstants) { }

    public record Result(boolean accepted, List<Observation> observations, List<RowIssue> issues, Set<Long> quarantinedRows) { }

    private final SemanticPlan plan;
    private final CodeMembership codes;
    private final String statusConceptCode;

    /** @param statusConceptCode concept code that marks an attribute as the observation status (SDMX OBS_STATUS) */
    public WideRowNormalizer(SemanticPlan plan, CodeMembership codes, String statusConceptCode) {
        this.plan = plan;
        this.codes = codes;
        this.statusConceptCode = statusConceptCode;
    }

    public Result normalize(List<Map<String, Object>> rows) {
        List<RowIssue> issues = new ArrayList<>();
        Map<Long, List<Observation>> byRow = new LinkedHashMap<>();
        Map<String, Long> firstSeen = new HashMap<>();
        Set<Long> bad = new TreeSet<>();
        long rowNumber = 0;
        for (Map<String, Object> row : rows) {
            rowNumber++;
            int before = issues.size();
            List<Observation> produced = row(rowNumber, row, issues);
            if (issues.size() == before && !produced.isEmpty()) {
                Long earlier = firstSeen.putIfAbsent(produced.get(0).observationKey(), rowNumber);
                if (earlier != null) issues.add(new RowIssue(rowNumber, null, IssueCode.DUPLICATE_OBSERVATION, "same dimension tuple as row " + earlier));
            }
            if (issues.size() > before) bad.add(rowNumber); else byRow.put(rowNumber, produced);
        }
        boolean atomic = plan.errorMode() == ErrorMode.ATOMIC_REJECT;
        if (atomic && !issues.isEmpty()) return new Result(false, List.of(), List.copyOf(issues), Set.of());
        return new Result(true, byRow.values().stream().flatMap(List::stream).toList(), List.copyOf(issues), Set.copyOf(bad));
    }

    private List<Observation> row(long n, Map<String, Object> row, List<RowIssue> issues) {
        for (String column : row.keySet())
            if (plan.components().stream().noneMatch(c -> c.code().equals(column)))
                issues.add(new RowIssue(n, column, IssueCode.UNDECLARED_COLUMN, "column is not declared by the contract"));

        Map<String, String> tuple = new TreeMap<>();
        Set<String> overridden = new TreeSet<>();
        PeriodValue period = null;
        for (PlannedComponent d : plan.withRole(Component.Role.DIMENSION)) {
            String given = text(row.get(d.code()));
            String value = given;
            if (d.constantValue() != null) {
                if (given == null || given.equals(d.constantValue())) value = d.constantValue();
                else if (d.overridable()) overridden.add(d.code());
                else { issues.add(new RowIssue(n, d.code(), IssueCode.CONSTANT_NOT_OVERRIDABLE, "value is fixed by the contract")); continue; }
            }
            if (value == null) { issues.add(new RowIssue(n, d.code(), IssueCode.MISSING_DIMENSION, "every dimension of the key is required")); continue; }
            if (d.representation() instanceof Representation.TimePeriod time) {
                period = PeriodValue.parse(value, time.formats()).orElse(null);
                if (period == null) { issues.add(new RowIssue(n, d.code(), IssueCode.INVALID_PERIOD, "not a period in an admitted format")); continue; }
                value = period.lexical();
            } else if (!valid(d, value, n, issues)) continue;
            tuple.put(d.code(), value);
        }

        Map<String, String> attributes = new TreeMap<>();
        for (PlannedComponent a : plan.withRole(Component.Role.ATTRIBUTE)) {
            if (!a.isAuthoringColumn()) continue;
            String value = text(row.get(a.code()));
            if (value != null && valid(a, value, n, issues)) attributes.put(a.code(), value);
        }

        String key = CanonicalJson.digest(KEY_DOMAIN, Map.of("dataset", plan.datasetNamespace() + ":" + plan.datasetCode(),
                "structure", plan.structureRef().wire(), "dimensions", tuple));
        List<Observation> out = new ArrayList<>();
        for (PlannedComponent m : plan.withRole(Component.Role.MEASURE)) {
            BigDecimal value = decimal(m, row.get(m.code()), n, issues);
            Map<String, String> own = new TreeMap<>();
            String status = null;
            for (PlannedComponent a : plan.withRole(Component.Role.ATTRIBUTE)) {
                String attributeValue = attributes.get(a.code());
                if (attributeValue == null) continue;
                String target = a.attachment().measure();
                if (target != null && !target.equals(m.code())) continue;
                if (a.conceptRef().code().equals(statusConceptCode)) { if (status == null || target != null) status = attributeValue; }
                else own.put(a.code(), attributeValue);
            }
            if (value == null && status == null && text(row.get(m.code())) == null)
                issues.add(new RowIssue(n, m.code(), IssueCode.VALUE_WITHOUT_STATUS_MISSING, "an empty value needs an explicit status; zero, missing and suppressed are different facts"));
            out.add(new Observation(n, key, Map.copyOf(tuple), period, m.code(), m.measureRef(), m.unitRef(), value, status, Map.copyOf(own), Set.copyOf(overridden)));
        }
        return out;
    }

    private boolean valid(PlannedComponent c, String value, long n, List<RowIssue> issues) {
        Representation r = c.representation();
        boolean ok = true;
        if (r instanceof Representation.Coded coded) ok = codes.contains(coded.codelistRef(), value);
        else if (r instanceof Representation.IntegerRange range) {
            try { long v = Long.parseLong(value); ok = v >= range.min() && v <= range.max(); } catch (NumberFormatException e) { ok = false; }
        } else if (r instanceof Representation.BoundedText text) ok = value.length() <= text.maxLength();
        if (!ok) issues.add(new RowIssue(n, c.code(), r instanceof Representation.Coded ? IssueCode.INVALID_CODE : IssueCode.INVALID_VALUE, "value is outside the declared representation"));
        return ok;
    }

    private BigDecimal decimal(PlannedComponent m, Object raw, long n, List<RowIssue> issues) {
        if (text(raw) == null) return null;
        Representation.Numeric numeric = (Representation.Numeric) m.representation();
        BigDecimal value;
        if (raw instanceof BigDecimal b) value = b;
        else if (raw instanceof Integer || raw instanceof Long || raw instanceof Short) value = BigDecimal.valueOf(((Number) raw).longValue());
        else if (raw instanceof java.math.BigInteger i) value = new BigDecimal(i);
        else if (raw instanceof String s && DECIMAL.matcher(s.trim()).matches()) value = new BigDecimal(s.trim());
        else if ((raw instanceof Double || raw instanceof Float) && numeric.approximate()) value = new BigDecimal(raw.toString());
        else {
            boolean binary = raw instanceof Double || raw instanceof Float;
            issues.add(new RowIssue(n, m.code(), binary ? IssueCode.INEXACT_INPUT : IssueCode.INVALID_VALUE,
                    binary ? "an exact measure cannot be read through binary floating point" : "not a decimal number"));
            return null;
        }
        if (numeric.approximate()) return value;
        if (Math.max(value.stripTrailingZeros().scale(), 0) > numeric.scale()) {
            issues.add(new RowIssue(n, m.code(), IssueCode.SCALE_EXCEEDED, "more fraction digits than the declared scale; values are never rounded on load"));
            return null;
        }
        if (value.precision() - value.scale() > numeric.precision() - numeric.scale()) {
            issues.add(new RowIssue(n, m.code(), IssueCode.NUMERIC_OVERFLOW, "more integer digits than the declared precision allows"));
            return null;
        }
        return value;
    }

    private static String text(Object raw) {
        if (raw == null) return null;
        String s = raw.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
