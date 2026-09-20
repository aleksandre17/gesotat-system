package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.chart.ChartCompiler;
import org.base.api.service.platform.statistical.chart.ChartSpec;
import org.base.api.service.platform.statistical.chart.ChartSuggestions;
import org.base.api.service.platform.statistical.chart.VegaLiteRenderer;
import org.base.api.service.platform.statistical.compiler.ContractIssue;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Aggregation;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.MeasureDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** A picture of governed data obeys the contract that governs the data. */
class ChartGrammarTest {
    private final ChartCompiler charts = new ChartCompiler(new ObjectMapper());

    /** EMPLOYED counts people and may be added up; UNEMPLOYED is left as the registry's safe default. */
    private SemanticPlan plan() {
        InMemoryStatisticalRegistry registry = registry();
        Ref employed = Ref.parse("measure:SHARED:EMPLOYED(1.0.0)");
        registry.approved(employed, new MeasureDefinition(employed, Ref.parse("concept:SHARED:EMPLOYED(1.0.0)"),
                new Representation.Numeric(18, 0, false), Ref.parse("unit:SHARED:PERSONS(1.0.0)"), Aggregation.SUM));
        return compiler(registry).compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow();
    }

    private static Set<String> messages(ChartCompiler.Result result) {
        return result.issues().stream().map(ContractIssue::message).collect(Collectors.toSet());
    }

    @Test void aChartIsAMarkAndItsChannels() {
        ChartCompiler.Result result = charts.compile("""
                {"code":"EMPLOYED_OVER_TIME","mark":"LINE",
                 "encoding":{"X":{"component":"TIME_PERIOD"},"Y":{"component":"EMPLOYED"},"COLOR":{"component":"SEX"}},
                 "titles":{"ka":"დასაქმებულები დროში"}}""", plan());
        assertTrue(result.accepted(), result.issues().toString());
        ChartSpec spec = result.spec().orElseThrow();
        assertEquals(ChartSpec.Mark.LINE, spec.mark());
        assertEquals("EMPLOYED", spec.encoding(ChartSpec.Channel.Y).component());
    }

    @Test void theGrammarIsClosedAndEveryFieldMustBeDeclaredByTheContract() {
        SemanticPlan plan = plan();
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"LINE","encoding":{"X":{"component":"TIME_PERIOD"},"Y":{"component":"EMPLOYED"}},"sql":"DROP"}""", plan))
                .stream().anyMatch(m -> m.contains("not part of the chart grammar")));
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"DONUT","encoding":{"X":{"component":"TIME_PERIOD"},"Y":{"component":"EMPLOYED"}}}""", plan))
                .stream().anyMatch(m -> m.contains("not one of")), "a chart type is not invented on the spot");
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"BAR","encoding":{"X":{"component":"REF_AREA"},"Y":{"component":"INVENTED"}}}""", plan))
                .stream().anyMatch(m -> m.contains("declares no component named INVENTED")));
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"BAR","encoding":{"WIDTH":{"component":"EMPLOYED"}}}""", plan))
                .stream().anyMatch(m -> m.contains("not one of")), "an unknown channel is refused");
    }

    @Test void aPictureMayNotInventArithmetic() {
        SemanticPlan plan = plan();
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"BAR","encoding":{"X":{"component":"REF_AREA"},"Y":{"component":"UNEMPLOYED","aggregate":"SUM"}}}""", plan))
                .stream().anyMatch(m -> m.contains("may not be combined with SUM")), "the contract allows NONE for this measure");
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"BAR","encoding":{"X":{"component":"REF_AREA","aggregate":"SUM"},"Y":{"component":"EMPLOYED"}}}""", plan))
                .stream().anyMatch(m -> m.contains("only a measure is aggregated")));
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"ARC","encoding":{"THETA":{"component":"SEX"},"COLOR":{"component":"REF_AREA"}}}""", plan))
                .stream().anyMatch(m -> m.contains("is not a measure")), "a category is not a quantity");
        assertTrue(charts.compile("""
                {"code":"C","mark":"BAR","encoding":{"X":{"component":"REF_AREA"},"Y":{"component":"EMPLOYED","aggregate":"SUM"}}}""", plan).accepted(),
                "what the contract allows is allowed");
    }

    @Test void eachMarkAsksForWhatItNeeds() {
        SemanticPlan plan = plan();
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"ARC","encoding":{"THETA":{"component":"EMPLOYED","aggregate":"SUM"}}}""", plan))
                .stream().anyMatch(m -> m.contains("needs the category it divides by")));
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"LINE","encoding":{"X":{"component":"REF_AREA"},"Y":{"component":"EMPLOYED"}}}""", plan))
                .stream().anyMatch(m -> m.contains("joins points along an ordered dimension")), "a line over regions is not a line");
        assertTrue(messages(charts.compile("""
                {"code":"C","mark":"BAR","encoding":{"X":{"component":"REF_AREA"}}}""", plan))
                .stream().anyMatch(m -> m.contains("needs a y channel")));
    }

    @Test void theSpecificationCarriesWhatTheContractKnows() {
        SemanticPlan plan = plan();
        ChartSpec spec = charts.compile("""
                {"code":"C","mark":"LINE","encoding":{"X":{"component":"TIME_PERIOD"},"Y":{"component":"EMPLOYED"},"COLOR":{"component":"SEX"}},
                 "titles":{"ka":"დასაქმებულები"}}""", plan).spec().orElseThrow();
        Map<String, Object> vega = VegaLiteRenderer.render(spec, plan, "ka", "/api/v1/data");
        assertEquals(VegaLiteRenderer.SCHEMA, vega.get("$schema"));
        assertEquals(Map.of("type", "line", "tooltip", true), vega.get("mark"));
        Map<?, ?> encoding = (Map<?, ?>) vega.get("encoding");
        assertEquals("temporal", ((Map<?, ?>) encoding.get("x")).get("type"), "the time dimension is time, because the contract says so");
        Map<?, ?> y = (Map<?, ?>) encoding.get("y");
        assertEquals("quantitative", y.get("type"));
        assertEquals("დასაქმებულები, PERSONS", y.get("title"), "caption and unit come from the contract");
        assertEquals(".0f", y.get("format"), "the decimals the contract declares");
        assertEquals("nominal", ((Map<?, ?>) encoding.get("color")).get("type"));
        assertEquals("დასაქმებულები", vega.get("title"));
    }

    @Test void whatTheContractAllowsIsOfferedWithoutAnyoneWritingIt() {
        SemanticPlan plan = plan();
        List<ChartSpec> proposals = ChartSuggestions.propose(plan);
        assertFalse(proposals.isEmpty());
        for (ChartSpec spec : proposals) {
            List<ContractIssue> issues = new java.util.ArrayList<>();
            charts.validate(spec, plan, issues);
            assertTrue(issues.isEmpty(), spec.code() + " -> " + issues);
        }
        assertTrue(proposals.stream().anyMatch(s -> s.mark() == ChartSpec.Mark.LINE && "TIME_PERIOD".equals(s.encoding(ChartSpec.Channel.X).component())));
        assertTrue(proposals.stream().anyMatch(s -> s.mark() == ChartSpec.Mark.BAR));
        assertTrue(proposals.stream().noneMatch(s -> s.mark() == ChartSpec.Mark.ARC),
                "a share of a total is not offered for a structure whose rows are not one slice each");

        SemanticPlan singleDimension = compiler(registry()).compile(landDraft(false), LAND, Mode.APPROVAL).plan().orElseThrow();
        assertTrue(ChartSuggestions.propose(singleDimension).stream().noneMatch(s -> s.mark() == ChartSpec.Mark.ARC),
                "nor when the contract does not allow that measure to be added up");
    }
}
