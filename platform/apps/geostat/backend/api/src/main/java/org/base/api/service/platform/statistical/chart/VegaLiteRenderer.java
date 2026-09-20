package org.base.api.service.platform.statistical.chart;

import org.base.api.service.platform.statistical.chart.ChartSpec.Channel;
import org.base.api.service.platform.statistical.chart.ChartSpec.Encoding;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Aggregation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Renders a validated chart declaration as a Vega-Lite specification — the published, declarative form of the
 * grammar of graphics — so the client only draws what the contract already decided.
 *
 * Nothing here is chosen by the renderer: the measurement type comes from the component's representation, the
 * axis title from the contract caption, the number format from the declared decimals, and the unit from the
 * measure. A new mark or channel is a change in the grammar, not in this file.
 */
public final class VegaLiteRenderer {
    public static final String SCHEMA = "https://vega.github.io/schema/vega-lite/v5.json";

    private VegaLiteRenderer() { }

    public static Map<String, Object> render(ChartSpec spec, SemanticPlan plan, String languageTag, String dataUrl) {
        Map<String, PlannedComponent> declared = new LinkedHashMap<>();
        for (PlannedComponent c : plan.components()) declared.put(c.code(), c);

        Map<String, Object> encoding = new LinkedHashMap<>();
        for (Map.Entry<Channel, Encoding> entry : spec.encodings().entrySet()) {
            PlannedComponent component = declared.get(entry.getValue().component());
            encoding.put(entry.getKey().name().toLowerCase(Locale.ROOT), channel(entry.getValue(), component, plan, languageTag));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("$schema", SCHEMA);
        out.put("data", Map.of("url", dataUrl, "format", Map.of("type", "json", "property", "data")));
        out.put("mark", Map.of("type", spec.mark().name().toLowerCase(Locale.ROOT), "tooltip", true));
        out.put("encoding", encoding);
        String title = spec.titles().get(languageTag);
        if (title != null) out.put("title", title);
        // The data is a published snapshot of a governed dataset; the picture names what it draws.
        out.put("description", plan.datasetNamespace() + ":" + plan.datasetCode() + " (" + plan.structureRef().wire() + ")");
        return out;
    }

    private static Map<String, Object> channel(Encoding encoding, PlannedComponent component, SemanticPlan plan, String languageTag) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("field", component.code());
        field.put("type", measurementType(component));
        if (encoding.aggregation() != Aggregation.NONE) field.put("aggregate", encoding.aggregation().name().toLowerCase(Locale.ROOT));
        if (encoding.descending()) field.put("sort", "descending");

        String caption = plan.captions().getOrDefault(component.code(), Map.of()).get(languageTag);
        String unit = component.unitRef() == null ? null : component.unitRef().code();
        if (caption != null) field.put("title", unit == null ? caption : caption + ", " + unit);
        if (component.representation() instanceof Representation.Numeric numeric && !numeric.approximate())
            field.put("format", "." + numeric.scale() + "f"); // exactly the decimals the contract declares
        return field;
    }

    /** Vega-Lite's measurement types, decided by the contract's representation and role, never by the caller. */
    private static String measurementType(PlannedComponent component) {
        if (component.representation() instanceof Representation.TimePeriod) return "temporal";
        if (component.role() == Component.Role.MEASURE) return "quantitative";
        if (component.representation() instanceof Representation.IntegerRange) return "ordinal";
        return "nominal";
    }
}
