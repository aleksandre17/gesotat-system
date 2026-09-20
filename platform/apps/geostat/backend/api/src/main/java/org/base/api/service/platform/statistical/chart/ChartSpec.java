package org.base.api.service.platform.statistical.chart;

import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Aggregation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One chart, written in the grammar of graphics rather than as a chart type: a mark and the channels its
 * fields are encoded on. A pie is not a kind of chart here — it is an {@code ARC} mark with a {@code THETA}
 * channel — so a new picture is a new declaration, never new code.
 *
 * Every field names a component of the approved contract, so meaning, unit, decimals, labels and the
 * aggregation a measure permits all come from the contract that already governs the data.
 */
public record ChartSpec(String code, Mark mark, Map<Channel, Encoding> encodings, Map<String, String> titles) {

    /** The shapes this profile can draw; the renderer maps them to the published visual grammar. */
    public enum Mark { BAR, LINE, AREA, POINT, ARC }

    /** Where a field is shown. A channel carries meaning, not decoration. */
    public enum Channel { X, Y, COLOR, THETA, SIZE, COLUMN, ROW, TOOLTIP, ORDER }

    /**
     * A field on a channel. {@code aggregation} is only ever what the measure's contract allows; NONE means the
     * observation is shown as it was collected.
     */
    public record Encoding(String component, Aggregation aggregation, boolean descending) {
        public Encoding { aggregation = aggregation == null ? Aggregation.NONE : aggregation; }
        public static Encoding of(String component) { return new Encoding(component, Aggregation.NONE, false); }
    }

    public ChartSpec {
        encodings = Map.copyOf(new LinkedHashMap<>(encodings));
        titles = titles == null ? Map.of() : Map.copyOf(titles);
    }

    public Encoding encoding(Channel channel) { return encodings.get(channel); }
}
