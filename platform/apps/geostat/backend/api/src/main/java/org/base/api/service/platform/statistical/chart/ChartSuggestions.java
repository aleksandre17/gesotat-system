package org.base.api.service.platform.statistical.chart;

import org.base.api.service.platform.statistical.chart.ChartSpec.Channel;
import org.base.api.service.platform.statistical.chart.ChartSpec.Encoding;
import org.base.api.service.platform.statistical.chart.ChartSpec.Mark;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Aggregation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the contract already says a dataset can honestly show, offered so that nobody writes a chart by hand:
 * a time dimension becomes a line over time, a coded dimension becomes bars, and a single coded dimension with
 * a measure the contract allows to be summed becomes a share of a total.
 *
 * These are proposals, not decisions. Each one is validated by the same compiler a hand-written chart goes
 * through, so a suggestion can never be something a person would be refused.
 */
public final class ChartSuggestions {

    private ChartSuggestions() { }

    public static List<ChartSpec> propose(SemanticPlan plan) {
        List<PlannedComponent> measures = plan.withRole(Component.Role.MEASURE);
        List<PlannedComponent> dimensions = plan.withRole(Component.Role.DIMENSION);
        Optional<PlannedComponent> time = dimensions.stream().filter(d -> d.representation() instanceof Representation.TimePeriod).findFirst();
        List<PlannedComponent> coded = dimensions.stream().filter(d -> d.representation() instanceof Representation.Coded).toList();

        List<ChartSpec> out = new ArrayList<>();
        for (PlannedComponent measure : measures) {
            time.ifPresent(t -> {
                Map<Channel, Encoding> encoding = new EnumMap<>(Channel.class);
                encoding.put(Channel.X, Encoding.of(t.code()));
                encoding.put(Channel.Y, Encoding.of(measure.code()));
                coded.stream().findFirst().ifPresent(c -> encoding.put(Channel.COLOR, Encoding.of(c.code())));
                out.add(new ChartSpec(measure.code() + "_OVER_TIME", Mark.LINE, encoding, Map.of()));
            });
            if (!coded.isEmpty()) {
                Map<Channel, Encoding> bars = new EnumMap<>(Channel.class);
                bars.put(Channel.X, Encoding.of(coded.get(0).code()));
                bars.put(Channel.Y, Encoding.of(measure.code()));
                out.add(new ChartSpec(measure.code() + "_BY_" + coded.get(0).code(), Mark.BAR, bars, Map.of()));
            }
            // A share of a total is only honest when the contract says this measure may be added up.
            if (coded.size() == 1 && dimensions.size() == 1 && measure.aggregation() == Aggregation.SUM) {
                Map<Channel, Encoding> arc = new EnumMap<>(Channel.class);
                arc.put(Channel.THETA, new Encoding(measure.code(), Aggregation.SUM, false));
                arc.put(Channel.COLOR, Encoding.of(coded.get(0).code()));
                out.add(new ChartSpec(measure.code() + "_SHARE", Mark.ARC, arc, Map.of()));
            }
        }
        return List.copyOf(out);
    }
}
