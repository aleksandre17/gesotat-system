package org.base.api.service.platform.statistical.export;

import org.base.api.service.platform.statistical.ingest.WideRowNormalizer.Observation;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SDMX-CSV 2.0 rendering of canonical observations, driven only by the semantic plan: one row per observation
 * key, one column per dimension, measure and row-level attribute (SDMX 3 multi-measure layout). Deterministic:
 * rows are ordered by their dimension tuple, so equal data gives equal bytes.
 *
 * What this profile cannot express losslessly is refused, never approximated: a format that has a single
 * primary measure is rejected for a multi-measure structure instead of being pivoted silently (register Q47).
 * Conformance is claimed only for what {@code SdmxCsvExporterTest} asserts; validation against the official
 * SDMX tooling is tracked in the implementation checklist.
 */
public final class SdmxCsvExporter {

    public enum Format { SDMX_CSV_2_0, SDMX_ML_2_1_GENERIC }

    public static final class UnsupportedConversion extends RuntimeException {
        public UnsupportedConversion(String message) { super(message); }
    }

    private SdmxCsvExporter() { }

    public static String export(SemanticPlan plan, List<Observation> observations, Format format) {
        List<PlannedComponent> measures = plan.withRole(Component.Role.MEASURE);
        if (format == Format.SDMX_ML_2_1_GENERIC)
            throw new UnsupportedConversion(measures.size() > 1
                    ? "SDMX 2.1 has one primary measure; a structure with " + measures.size() + " measures is not converted implicitly"
                    : "SDMX-ML 2.1 is not a supported exchange format of this release");

        List<PlannedComponent> dimensions = plan.withRole(Component.Role.DIMENSION);
        List<PlannedComponent> attributes = plan.withRole(Component.Role.ATTRIBUTE).stream().filter(a -> a.attachment().level().variesPerRow()).toList();

        List<String> header = new ArrayList<>(List.of("STRUCTURE", "STRUCTURE_ID", "ACTION"));
        dimensions.forEach(d -> header.add(d.code()));
        measures.forEach(m -> header.add(m.code()));
        attributes.forEach(a -> header.add(a.code()));

        // One output row per observation key; the key's tuple orders the rows.
        Map<String, Map<String, String>> rows = new TreeMap<>();
        for (Observation o : observations) {
            StringBuilder order = new StringBuilder();
            dimensions.forEach(d -> order.append(o.dimensions().getOrDefault(d.code(), "")).append('	'));
            Map<String, String> row = rows.computeIfAbsent(order.toString(), k -> new LinkedHashMap<>(o.dimensions()));
            row.put(o.measureCode(), render(o.value(), plan.component(o.measureCode())));
            if (o.statusAttribute() != null && o.status() != null) row.put(o.statusAttribute(), o.status()); // no declared status, nothing published
            o.attributes().forEach(row::putIfAbsent);
        }

        String structureId = plan.structureRef().namespace() + ":" + plan.structureRef().code() + "(" + plan.structureRef().version() + ")";
        StringBuilder out = new StringBuilder(String.join(",", header)).append("\r\n");
        for (Map<String, String> row : rows.values()) {
            List<String> cells = new ArrayList<>(List.of("datastructure", structureId, "I"));
            for (int i = 3; i < header.size(); i++) cells.add(row.getOrDefault(header.get(i), ""));
            out.append(String.join(",", cells.stream().map(SdmxCsvExporter::escape).toList())).append("\r\n");
        }
        return out.toString();
    }

    /**
     * A measure is published with the number of decimals its contract declares, whatever scale the storage
     * column happened to use. The value itself is never changed: ingestion already refused anything that does
     * not fit, so this only fixes how many zeros are written.
     */
    private static String render(java.math.BigDecimal value, PlannedComponent measure) {
        if (value == null) return "";
        if (!(measure.representation() instanceof org.base.api.service.platform.statistical.model.Representation.Numeric numeric) || numeric.approximate())
            return value.toPlainString();
        return value.setScale(numeric.scale(), java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    /** RFC 4180 quoting. */
    private static String escape(String cell) {
        if (cell.indexOf(',') < 0 && cell.indexOf('"') < 0 && cell.indexOf('\n') < 0 && cell.indexOf('\r') < 0) return cell;
        return '"' + cell.replace("\"", "\"\"") + '"';
    }
}
