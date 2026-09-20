package org.base.api.service.platform.statistical.chart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.chart.ChartSpec.Channel;
import org.base.api.service.platform.statistical.chart.ChartSpec.Encoding;
import org.base.api.service.platform.statistical.chart.ChartSpec.Mark;
import org.base.api.service.platform.statistical.compiler.ContractIssue;
import org.base.api.service.platform.statistical.compiler.ContractIssue.Code;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Aggregation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses a chart declaration in a closed grammar and validates it against the approved contract of the data
 * it draws. The same compiler shape as the contract compiler: unknown field rejected, every reference
 * resolved, every rule stated once.
 *
 * What it refuses is what a picture must never do: name a field the contract does not declare, put a category
 * where a quantity belongs, or combine a measure in a way the contract forbids — summing percentages, indices
 * or averages (register Q24). A chart is presentation; it is not allowed to invent arithmetic.
 */
public final class ChartCompiler {
    private static final Set<String> ROOT = Set.of("code", "mark", "encoding", "titles");
    private static final Set<String> ENCODING_FIELDS = Set.of("component", "aggregate", "descending");
    /** Channels that carry a number; everything else carries an identity. */
    static final Set<Channel> QUANTITATIVE = Set.of(Channel.THETA, Channel.SIZE);

    public record Result(Optional<ChartSpec> spec, List<ContractIssue> issues) {
        public boolean accepted() { return spec.isPresent(); }
    }

    private final ObjectMapper mapper;

    public ChartCompiler(ObjectMapper mapper) { this.mapper = mapper; }

    public Result compile(String document, SemanticPlan plan) {
        List<ContractIssue> issues = new ArrayList<>();
        JsonNode root;
        try { root = mapper.readTree(document); }
        catch (Exception e) { return new Result(Optional.empty(), List.of(ContractIssue.of(Code.MALFORMED_DOCUMENT, "", "document is not valid JSON"))); }
        if (root == null || !root.isObject()) return new Result(Optional.empty(), List.of(ContractIssue.of(Code.MALFORMED_DOCUMENT, "", "document must be a JSON object")));

        closed(root, ROOT, "", issues);
        String code = text(root, "code", "", issues);
        Mark mark = enumOf(Mark.class, text(root, "mark", "", issues), "/mark", issues);
        Map<Channel, Encoding> encodings = new EnumMap<>(Channel.class);
        JsonNode encoding = root.get("encoding");
        if (encoding == null || !encoding.isObject()) issues.add(ContractIssue.of(Code.MISSING_FIELD, "/encoding", "a chart without an encoding draws nothing"));
        else {
            Iterator<Map.Entry<String, JsonNode>> channels = encoding.fields();
            while (channels.hasNext()) {
                Map.Entry<String, JsonNode> entry = channels.next();
                String path = "/encoding/" + entry.getKey();
                Channel channel = enumOf(Channel.class, entry.getKey(), path, issues);
                if (channel == null) continue;
                JsonNode node = entry.getValue();
                if (!node.isObject()) { issues.add(ContractIssue.of(Code.INVALID_VALUE, path, "a channel binds a component")); continue; }
                closed(node, ENCODING_FIELDS, path, issues);
                String component = text(node, "component", path, issues);
                Aggregation aggregate = node.hasNonNull("aggregate") ? enumOf(Aggregation.class, node.get("aggregate").asText(), path + "/aggregate", issues) : Aggregation.NONE;
                if (component != null && aggregate != null) encodings.put(channel, new Encoding(component, aggregate, node.path("descending").asBoolean(false)));
            }
        }
        Map<String, String> titles = new LinkedHashMap<>();
        if (root.has("titles")) root.get("titles").fields().forEachRemaining(e -> titles.put(e.getKey(), e.getValue().asText()));

        if (!issues.isEmpty()) return new Result(Optional.empty(), List.copyOf(issues));
        ChartSpec spec = new ChartSpec(code, mark, encodings, titles);
        validate(spec, plan, issues);
        return issues.isEmpty() ? new Result(Optional.of(spec), List.of()) : new Result(Optional.empty(), List.copyOf(issues));
    }

    /** Every rule a picture of governed data must obey, stated once. */
    public void validate(ChartSpec spec, SemanticPlan plan, List<ContractIssue> issues) {
        Map<String, PlannedComponent> declared = new LinkedHashMap<>();
        for (PlannedComponent c : plan.components()) declared.put(c.code(), c);

        for (Map.Entry<Channel, Encoding> entry : spec.encodings().entrySet()) {
            Channel channel = entry.getKey();
            Encoding encoding = entry.getValue();
            String path = "/encoding/" + channel;
            PlannedComponent component = declared.get(encoding.component());
            if (component == null) {
                issues.add(ContractIssue.of(Code.INVALID_REFERENCE, path, "the contract declares no component named " + encoding.component()));
                continue;
            }
            boolean measure = component.role() == Component.Role.MEASURE;
            if (QUANTITATIVE.contains(channel) && !measure)
                issues.add(ContractIssue.of(Code.INVALID_VALUE, path, channel + " shows a quantity; " + component.code() + " is not a measure"));
            if (!measure && encoding.aggregation() != Aggregation.NONE)
                issues.add(ContractIssue.of(Code.INVALID_VALUE, path, "only a measure is aggregated"));
            if (measure && encoding.aggregation() != Aggregation.NONE && encoding.aggregation() != component.aggregation())
                issues.add(ContractIssue.of(Code.INVALID_VALUE, path, component.code() + " may not be combined with " + encoding.aggregation()
                        + "; its contract allows " + component.aggregation()));
            if (component.role() == Component.Role.ATTRIBUTE && QUANTITATIVE.contains(channel))
                issues.add(ContractIssue.of(Code.INVALID_VALUE, path, "an attribute explains a value; it is not one"));
        }

        switch (spec.mark()) {
            case ARC -> {
                require(spec, Channel.THETA, "an arc needs the quantity it divides", issues);
                require(spec, Channel.COLOR, "an arc needs the category it divides by", issues);
                if (spec.encoding(Channel.THETA) != null && spec.encoding(Channel.THETA).aggregation() == Aggregation.NONE
                        && plan.withRole(Component.Role.DIMENSION).size() > 1)
                    issues.add(ContractIssue.of(Code.INVALID_VALUE, "/encoding/THETA",
                            "a share of a total needs an aggregation the contract allows, or one observation per slice"));
            }
            case BAR, LINE, AREA, POINT -> {
                require(spec, Channel.X, "this mark needs an x channel", issues);
                require(spec, Channel.Y, "this mark needs a y channel", issues);
                if (spec.mark() == Mark.LINE || spec.mark() == Mark.AREA) {
                    Encoding x = spec.encoding(Channel.X);
                    PlannedComponent component = x == null ? null : declared.get(x.component());
                    if (component != null && !(component.representation() instanceof Representation.TimePeriod))
                        issues.add(ContractIssue.of(Code.INVALID_VALUE, "/encoding/X", "a line joins points along an ordered dimension; " + component.code() + " is not one"));
                }
            }
        }
    }

    private static void require(ChartSpec spec, Channel channel, String why, List<ContractIssue> issues) {
        if (spec.encoding(channel) == null) issues.add(ContractIssue.of(Code.MISSING_FIELD, "/encoding/" + channel, why));
    }

    private static void closed(JsonNode node, Set<String> allowed, String path, List<ContractIssue> issues) {
        node.fieldNames().forEachRemaining(name -> {
            if (!allowed.contains(name)) issues.add(ContractIssue.of(Code.UNKNOWN_FIELD, path + "/" + name, "field is not part of the chart grammar"));
        });
    }

    private static String text(JsonNode parent, String field, String path, List<ContractIssue> issues) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            issues.add(ContractIssue.of(Code.MISSING_FIELD, path + "/" + field, "value is required"));
            return null;
        }
        return node.asText();
    }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String value, String path, List<ContractIssue> issues) {
        if (value == null) return null;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException e) {
            issues.add(ContractIssue.of(Code.INVALID_VALUE, path, "not one of " + List.of(type.getEnumConstants())));
            return null;
        }
    }
}
