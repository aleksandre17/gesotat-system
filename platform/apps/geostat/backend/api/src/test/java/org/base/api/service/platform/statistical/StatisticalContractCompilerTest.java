package org.base.api.service.platform.statistical;

import org.base.api.service.platform.statistical.canonical.CanonicalJson;
import org.base.api.service.platform.statistical.compat.CompatibilityClassifier;
import org.base.api.service.platform.statistical.compiler.ContractIssue;
import org.base.api.service.platform.statistical.compiler.ContractIssue.Code;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler.Mode;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.PeriodValue;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation.TimeFormat;
import org.base.api.service.platform.statistical.plan.PhysicalNameStrategy;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class StatisticalContractCompilerTest {
    private final StatisticalContractCompiler compiler = compiler(registry());

    private static Set<Code> codes(StatisticalContractCompiler.Result result) {
        Set<Code> out = new HashSet<>();
        for (ContractIssue issue : result.issues()) out.add(issue.code());
        return out;
    }

    private SemanticPlan labour() { return compiler.compile(labourDraft(), LABOUR, Mode.APPROVAL).plan().orElseThrow(); }

    // ---- reference grammar (Q30)

    @Test void referenceIsExactAndRoundTrips() {
        Ref ref = Ref.parse("measure:SHARED:EMPLOYED(1.2.3)");
        assertEquals("measure:SHARED:EMPLOYED(1.2.3)", ref.wire());
        assertEquals(Ref.Kind.MEASURE, ref.kind());
        for (String floating : List.of("measure:SHARED:EMPLOYED(latest)", "measure:SHARED:EMPLOYED(1.*)", "measure:SHARED:EMPLOYED(1.0)",
                "measure:SHARED:EMPLOYED", "table:SHARED:X(1.0.0)", "measure:SHARED:1X(1.0.0)", "measure:SHARED:EMPLOYED(^1.0.0)"))
            assertThrows(IllegalArgumentException.class, () -> Ref.parse(floating), floating);
    }

    // ---- two unrelated subject areas, one engine (plan §12: onboarding without a core-code branch)

    @Test void compilesTwoUnrelatedDatasetsWithTheSameEngine() {
        SemanticPlan labour = labour();
        assertEquals(List.of("TIME_PERIOD", "REF_AREA", "SEX"), labour.withRole(Component.Role.DIMENSION).stream().map(SemanticPlan.PlannedComponent::code).toList());
        assertEquals(Ref.parse("unit:SHARED:PERSONS(1.0.0)"), labour.component("EMPLOYED").unitRef());
        assertEquals(Ref.parse("concept:SHARED:EMPLOYED(1.0.0)"), labour.component("EMPLOYED").conceptRef());

        SemanticPlan land = compiler.compile(landDraft(false), LAND, Mode.APPROVAL).plan().orElseThrow();
        assertTrue(land.withRole(Component.Role.DIMENSION).stream().noneMatch(d -> d.code().equals("TIME_PERIOD")), "non-time structure");
        assertEquals("GE", land.component("REF_AREA").constantValue());
        assertNotEquals(labour.semanticDigest(), land.semanticDigest());
    }

    @Test void datasetLevelAttributeIsContractMetadataNotAColumn() {
        List<String> columns = labour().physical().tables().get(0).columns().stream().map(SemanticPlan.PhysicalColumn::name).toList();
        assertEquals(List.of("TIME_PERIOD", "REF_AREA", "SEX", "EMPLOYED", "EMPLOYED_STATUS", "UNEMPLOYED", "UNEMPLOYED_STATUS"), columns);
    }

    @Test void dependencyClosureIsPinnedAndIncludesDerivedReferences() {
        List<String> closure = labour().dependencies().stream().map(Ref::wire).toList();
        assertTrue(closure.containsAll(List.of("unit:SHARED:PERSONS(1.0.0)", "concept:SHARED:EMPLOYED(1.0.0)",
                "codelist:SHARED:CL_OBS_STATUS(1.0.0)", "policy:SHARED:QUALITY_BASELINE(1.0.0)", "profile:SHARED:STAT_AGGREGATE(1.0.0)")));
        assertEquals(closure.stream().sorted().toList(), closure, "closure order is canonical");
    }

    // ---- determinism and the two digests (Q39)

    @Test void digestIsDeterministicAndIgnoresAuthoringKeyOrderAndWhitespace() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(labourDraft());
        var reordered = mapper.createObjectNode();
        List<String> names = new java.util.ArrayList<>();
        tree.fieldNames().forEachRemaining(names::add);
        java.util.Collections.reverse(names);
        names.forEach(n -> reordered.set(n, tree.get(n)));
        SemanticPlan a = labour();
        SemanticPlan b = compiler.compile(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reordered), LABOUR, Mode.APPROVAL).plan().orElseThrow();
        assertEquals(a.semanticDigest(), b.semanticDigest());
        assertEquals(a.revisionDigest(), b.revisionDigest());
        assertTrue(a.semanticDigest().matches("[0-9a-f]{64}"));
    }

    @Test void captionChangeMovesOnlyTheRevisionDigest() {
        SemanticPlan before = labour();
        SemanticPlan after = compiler.compile(labourDraft().replace("რეგიონი", "ტერიტორია"), LABOUR, Mode.APPROVAL).plan().orElseThrow();
        assertEquals(before.semanticDigest(), after.semanticDigest());
        assertNotEquals(before.revisionDigest(), after.revisionDigest());
        assertEquals(CompatibilityClassifier.Level.PRESENTATION, CompatibilityClassifier.classify(before, after).level());
    }

    @Test void canonicalJsonFollowsRfc8785ForTheAdmittedValueSpace() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("b", List.of(1, true));
        value.put("a", "x\n\"é");
        value.put("€", null);
        assertEquals("{\"a\":\"x\\n\\\"é\",\"b\":[1,true],\"€\":null}", CanonicalJson.write(value));
        assertEquals(CanonicalJson.write("é"), CanonicalJson.write("é"), "NFC before hashing");
        assertThrows(IllegalArgumentException.class, () -> CanonicalJson.write(0.1d), "no binary floating point in a digest");
        assertNotEquals(CanonicalJson.digest("domain.a", value), CanonicalJson.digest("domain.b", value), "domain separation");
    }

    // ---- closed grammar (Q31, Q32)

    @Test void closedGrammarRejectsWhatItDoesNotName() {
        assertTrue(codes(compiler.compile("not json", LABOUR, Mode.DRAFT)).contains(Code.MALFORMED_DOCUMENT));
        assertTrue(codes(compiler.compile(labourDraft().replace("\"sourceProfile\"", "\"sql\":\"DROP TABLE x\",\"sourceProfile\""), LABOUR, Mode.DRAFT)).contains(Code.UNKNOWN_FIELD));
        assertTrue(codes(compiler.compile(labourDraft().replace("{\"inline\":", "{\"existingStructureRef\":\"dsd:LABOUR:DSD_LABOUR(1.0.0)\",\"inline\":"), LABOUR, Mode.DRAFT)).contains(Code.STRUCTURE_CHOICE_AMBIGUOUS));
        assertTrue(codes(compiler.compile(labourDraft().replace("\"structure\":{\"inline\":", "\"structure\":{\"other\":"), LABOUR, Mode.DRAFT)).contains(Code.STRUCTURE_CHOICE_AMBIGUOUS));
        assertTrue(codes(compiler.compile(labourDraft().replace("measure:SHARED:EMPLOYED(1.0.0)", "measure:SHARED:EMPLOYED(latest)"), LABOUR, Mode.DRAFT)).contains(Code.INVALID_REFERENCE));
        assertTrue(codes(compiler.compile(labourDraft().replace("\"formats\":[\"YEAR\",\"QUARTER\"]", "\"formats\":[\"DECADE\"]"), LABOUR, Mode.DRAFT)).contains(Code.INVALID_VALUE));
    }

    @Test void measureSemanticsCannotBeRestatedOnTheWire() {
        String restated = labourDraft().replace("{\"code\":\"EMPLOYED\",\"measureRef\"", "{\"code\":\"EMPLOYED\",\"unitRef\":\"unit:SHARED:PERCENT(1.0.0)\",\"measureRef\"");
        assertTrue(codes(compiler.compile(restated, LABOUR, Mode.DRAFT)).contains(Code.REDUNDANT_MEASURE_SEMANTICS));
    }

    // ---- semantic validation

    @Test void rejectsInvalidAttachmentsDuplicateComponentsAndMissingCaptions() {
        assertTrue(codes(compiler.compile(labourDraft().replace("\"measure\":\"UNEMPLOYED\"}", "\"measure\":\"NO_SUCH\"}"), LABOUR, Mode.DRAFT)).contains(Code.INVALID_ATTACHMENT));
        assertTrue(codes(compiler.compile(labourDraft().replace("{\"level\":\"DATASET\"}", "{\"level\":\"DIMENSION_GROUP\",\"dimensions\":[\"TIME_PERIOD\",\"REF_AREA\",\"SEX\"]}"), LABOUR, Mode.DRAFT)).contains(Code.INVALID_ATTACHMENT));
        assertTrue(compiler.compile(labourDraft().replace("{\"level\":\"DATASET\"}", "{\"level\":\"DIMENSION_GROUP\",\"dimensions\":[\"REF_AREA\"]}"), LABOUR, Mode.DRAFT).accepted());
        assertTrue(codes(compiler.compile(labourDraft().replace("{\"code\":\"UNEMPLOYED\",\"measureRef\"", "{\"code\":\"employed\",\"measureRef\""), LABOUR, Mode.DRAFT)).contains(Code.DUPLICATE_COMPONENT));
        assertTrue(codes(compiler.compile(labourDraft().replace("\"SEX\":{\"ka\":\"სქესი\"},", ""), LABOUR, Mode.DRAFT)).contains(Code.MISSING_CAPTION));
        assertTrue(codes(compiler.compile(landDraft(false).replace("\"value\":\"GE\"", "\"value\":\"XX\""), LAND, Mode.DRAFT)).contains(Code.CONSTANT_BINDING_INVALID));
    }

    @Test void numericEnvelopeBeyondCanonicalStorageIsRejectedAtCompileTime() {
        var registry = registry();
        Ref wide = Ref.parse("measure:SHARED:EMPLOYED(1.0.0)");
        registry.approved(wide, new org.base.api.service.platform.statistical.registry.StatisticalRegistry.MeasureDefinition(wide,
                Ref.parse("concept:SHARED:EMPLOYED(1.0.0)"), new org.base.api.service.platform.statistical.model.Representation.Numeric(30, 12, false), Ref.parse("unit:SHARED:PERSONS(1.0.0)")));
        assertTrue(codes(compiler(registry).compile(labourDraft(), LABOUR, Mode.DRAFT)).contains(Code.NUMERIC_ENVELOPE_EXCEEDED));
    }

    // ---- governance: profile, proposal workflow (Q01, Q09), tenancy (Q08, Q44)

    @Test void microdataProfileIsRefusedExplicitly() {
        assertEquals(Set.of(Code.PROFILE_UNSUPPORTED), codes(compiler.compile(labourDraft().replace("STAT_AGGREGATE", "STAT_MICRODATA"), LABOUR, Mode.DRAFT)));
    }

    @Test void proposedEntryPreviewsButCannotBeApproved() {
        assertTrue(compiler.compile(landDraft(true), LAND, Mode.DRAFT).accepted());
        assertEquals(Set.of(Code.REFERENCE_NOT_APPROVED), codes(compiler.compile(landDraft(true), LAND, Mode.APPROVAL)));
    }

    @Test void anotherProductsEntriesAreIndistinguishableFromMissingOnes() {
        StatisticalContractCompiler.Result foreign = compiler.compile(landDraft(false), LABOUR, Mode.DRAFT);
        StatisticalContractCompiler.Result missing = compiler.compile(landDraft(false).replace("measure:LAND:AREA_SIZE(1.0.0)", "measure:LAND:NEVER_EXISTED(1.0.0)"), LAND, Mode.DRAFT);
        assertEquals(Set.of(Code.UNRESOLVED_REFERENCE), codes(foreign));
        assertEquals(Set.of(Code.UNRESOLVED_REFERENCE), codes(missing));
        assertTrue(foreign.issues().stream().noneMatch(i -> i.message().toLowerCase().contains("denied") || i.message().toLowerCase().contains("tenant")));
    }

    @Test void unknownProviderIsRefusedNotDefaulted() {
        assertEquals(Set.of(Code.PROVIDER_UNSUPPORTED), codes(compiler.compile(labourDraft().replace("ACCESS_ACCDB", "SOME_OTHER"), LABOUR, Mode.DRAFT)));
    }

    // ---- capability negotiation, split and naming (Q35, Q37)

    private static ProviderCapabilities.Catalog tiny(int columns, int indexFields, boolean exact) {
        return profile -> Optional.of(new ProviderCapabilities("ACCESS_ACCDB", columns, 16, indexFields, 28, exact, Set.of("SEX")));
    }

    @Test void wideStructureSplitsWithTheFullKeyRepeatedAndStatusKeptWithItsMeasure() {
        SemanticPlan plan = compiler(registry(), tiny(5, 10, true)).compile(labourDraft(), LABOUR, Mode.DRAFT).plan().orElseThrow();
        assertEquals(2, plan.physical().tables().size());
        for (SemanticPlan.PhysicalTable table : plan.physical().tables()) {
            List<String> components = table.columns().stream().map(SemanticPlan.PhysicalColumn::componentCode).toList();
            assertEquals(List.of("TIME_PERIOD", "REF_AREA", "SEX"), components.subList(0, 3));
            assertEquals(components.contains("EMPLOYED"), components.contains("EMPLOYED_STATUS"));
        }
        assertNotEquals(plan.physical().tables().get(0).name().toUpperCase(), plan.physical().tables().get(1).name().toUpperCase());
    }

    @Test void reservedAndOverlongNamesAreRewrittenDeterministically() {
        SemanticPlan plan = compiler(registry(), tiny(255, 10, true)).compile(labourDraft(), LABOUR, Mode.DRAFT).plan().orElseThrow();
        Map<String, String> physical = new LinkedHashMap<>();
        plan.physical().tables().get(0).columns().forEach(c -> physical.put(c.componentCode(), c.name()));
        assertTrue(physical.get("SEX").matches("SEX_[0-9a-f]{8}"), "reserved word");
        assertTrue(physical.get("UNEMPLOYED_STATUS").matches("UNEMPLO_[0-9a-f]{8}") && physical.get("UNEMPLOYED_STATUS").length() <= 16, "over-long");
        assertEquals("EMPLOYED", physical.get("EMPLOYED"), "accepted names stay verbatim");
        SemanticPlan again = compiler(registry(), tiny(255, 10, true)).compile(labourDraft(), LABOUR, Mode.DRAFT).plan().orElseThrow();
        assertEquals(plan.physical(), again.physical());

        Set<String> taken = new HashSet<>();
        ProviderCapabilities caps = tiny(255, 10, true).find("x").orElseThrow();
        String first = PhysicalNameStrategy.allocate("Region", "a", taken, caps);
        String second = PhysicalNameStrategy.allocate("REGION", "b", taken, caps);
        assertNotEquals(first.toUpperCase(), second.toUpperCase(), "case-insensitive collision");
    }

    @Test void keyWiderThanTheProviderIndexGetsAGeneratedRowReference() {
        SemanticPlan plan = compiler(registry(), tiny(255, 2, true)).compile(labourDraft(), LABOUR, Mode.DRAFT).plan().orElseThrow();
        assertTrue(plan.physical().tables().get(0).generatedRowRef());
        assertNull(plan.physical().tables().get(0).columns().get(0).componentCode(), "row_ref is never a component");
        assertEquals(labour().semanticDigest(), plan.semanticDigest(), "physical layout never changes semantics");
    }

    @Test void whatTheProviderCannotHoldLosslesslyIsRejectedBeforeApproval() {
        assertTrue(codes(compiler(registry(), tiny(255, 10, false)).compile(labourDraft(), LABOUR, Mode.DRAFT)).contains(Code.CAPABILITY_EXCEEDED), "no exact decimal");
        assertTrue(codes(compiler(registry(), tiny(4, 10, true)).compile(labourDraft(), LABOUR, Mode.DRAFT)).contains(Code.CAPABILITY_EXCEEDED), "key plus one bundle does not fit");
    }

    // ---- compatibility classes (Q45)

    @Test void classifiesRevisionSteps() {
        SemanticPlan base = labour();
        assertEquals(CompatibilityClassifier.Level.IDENTICAL, CompatibilityClassifier.classify(base, labour()).level());

        String withoutUnemployed = labourDraft()
                .replaceAll(",\\s*\\{\"code\":\"UNEMPLOYED\",\"measureRef\":\"[^\"]+\"}", "")
                .replaceAll("(?s)\\{\"code\":\"UNEMPLOYED_STATUS\".*?\"UNEMPLOYED\"}},\\s*", "")
                .replace("\"UNEMPLOYED\":{\"ka\":\"უმუშევრები\"},", "").replace("\"UNEMPLOYED_STATUS\":{\"ka\":\"უმუშევრების სტატუსი\"},", "");
        SemanticPlan smaller = compiler.compile(withoutUnemployed, LABOUR, Mode.APPROVAL).plan().orElseThrow();
        CompatibilityClassifier.Verdict added = CompatibilityClassifier.classify(smaller, base);
        assertEquals(CompatibilityClassifier.Level.BACKWARD_COMPATIBLE, added.level(), added.reasons().toString());
        assertEquals("BACKWARD_COMPATIBLE", added.compatibilityMode());
        CompatibilityClassifier.Verdict removed = CompatibilityClassifier.classify(base, smaller);
        assertEquals(CompatibilityClassifier.Level.BREAKING, removed.level());
        assertEquals("BREAKING_NEW_REVISION", removed.compatibilityMode());

        SemanticPlan fewerFormats = compiler.compile(labourDraft().replace("[\"YEAR\",\"QUARTER\"]", "[\"YEAR\"]"), LABOUR, Mode.APPROVAL).plan().orElseThrow();
        assertEquals(CompatibilityClassifier.Level.BREAKING, CompatibilityClassifier.classify(base, fewerFormats).level());
        assertEquals(CompatibilityClassifier.Level.BACKWARD_COMPATIBLE, CompatibilityClassifier.classify(fewerFormats, base).level());
    }

    // ---- time periods (Q19)

    @Test void periodsResolveToClosedIntervalsAndNeverGuess() {
        Set<TimeFormat> all = Set.of(TimeFormat.values());
        assertEquals(LocalDate.of(2025, 12, 31), PeriodValue.parse("2025", all).orElseThrow().endInclusive());
        assertEquals(LocalDate.of(2025, 4, 1), PeriodValue.parse("2025-Q2", all).orElseThrow().start());
        assertEquals(LocalDate.of(2024, 2, 29), PeriodValue.parse("2024-M02", all).orElseThrow().endInclusive());
        assertEquals(LocalDate.of(2025, 7, 1), PeriodValue.parse("2025-S2", all).orElseThrow().start());
        assertEquals(LocalDate.of(2024, 12, 30), PeriodValue.parse("2025-W01", all).orElseThrow().start());
        assertEquals(LocalDate.of(2025, 3, 31), PeriodValue.parse("2025-01-01/P3M", all).orElseThrow().endInclusive());
        for (String bad : List.of("2025-Q5", "2025-M13", "25", "2025-02-30", "2025-W53", "Q1 2025", "2025-01-01/P0D", ""))
            assertTrue(PeriodValue.parse(bad, all).isEmpty(), bad);
        assertTrue(PeriodValue.parse("2025-Q2", Set.of(TimeFormat.YEAR)).isEmpty(), "format not admitted by the contract");
    }
}
