package org.base.api.service.platform.statistical.compiler;

import org.base.api.service.platform.statistical.canonical.CanonicalJson;
import org.base.api.service.platform.statistical.compiler.ContractIssue.Code;
import org.base.api.service.platform.statistical.compiler.ReferenceResolver.Resolved;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.ContractDraft;
import org.base.api.service.platform.statistical.model.ContractDraft.ConstantBinding;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.MeasureDefinition;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Scope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Parse -> resolve -> validate -> normalise -> negotiate capabilities -> plan -> digest.
 * One compiler serves UI and API, preview and approval; the only difference is {@link Mode}.
 * Pure: it reads the registry port and writes nothing, so a retry is always safe.
 */
public final class StatisticalContractCompiler {
    public static final String SEMANTIC_DOMAIN = "geostat.stat-contract.semantic.v1";
    public static final String REVISION_DOMAIN = "geostat.stat-contract.revision.v1";

    /** DRAFT admits proposed registry entries for preview; APPROVAL requires every dependency approved. */
    public enum Mode { DRAFT, APPROVAL }

    /** What the canonical store holds exactly; a contract beyond it is rejected at compile time. */
    public record NumericEnvelope(int maxPrecision, int maxScale) { }

    public record Settings(Set<String> supportedProfileCodes, NumericEnvelope numericEnvelope, Set<String> requiredCaptionLanguages) { }

    public record Result(Optional<SemanticPlan> plan, List<ContractIssue> issues) {
        public boolean accepted() { return plan.isPresent(); }
    }

    private final ContractDraftParser parser;
    private final ReferenceResolver resolver;
    private final SemanticValidator validator;
    private final PhysicalPlanner planner = new PhysicalPlanner();
    private final ProviderCapabilities.Catalog providers;
    private final Settings settings;

    public StatisticalContractCompiler(ContractDraftParser parser, StatisticalRegistry registry,
                                       ProviderCapabilities.Catalog providers, Settings settings) {
        this.parser = parser;
        this.resolver = new ReferenceResolver(registry);
        this.validator = new SemanticValidator(settings.numericEnvelope(), settings.requiredCaptionLanguages());
        this.providers = providers;
        this.settings = settings;
    }

    public Result compile(String document, Scope scope, Mode mode) {
        ContractDraftParser.Parsed parsed = parser.parse(document);
        return parsed.draft().map(draft -> compile(draft, scope, mode)).orElseGet(() -> new Result(Optional.empty(), parsed.issues()));
    }

    public Result compile(ContractDraft draft, Scope scope, Mode mode) {
        List<ContractIssue> issues = new ArrayList<>();
        if (!settings.supportedProfileCodes().contains(draft.profileRef().code()))
            return rejected(List.of(ContractIssue.of(Code.PROFILE_UNSUPPORTED, "/profileRef", "profile is not supported by this compiler: " + draft.profileRef())));

        Optional<Resolved> resolved = resolver.resolve(draft, scope, mode, issues);
        if (resolved.isEmpty()) return rejected(issues);
        validator.validate(resolved.get(), issues);
        if (!issues.isEmpty()) return rejected(issues);

        List<PlannedComponent> components = normalise(resolved.get());
        Optional<ProviderCapabilities> caps = providers.find(draft.sourceProfile());
        if (caps.isEmpty()) return rejected(List.of(ContractIssue.of(Code.PROVIDER_UNSUPPORTED, "/sourceProfile", "no registered provider for this source profile")));
        Optional<PhysicalPlan> physical = planner.plan(resolved.get().structure().ref(), components, caps.get(), issues);
        if (physical.isEmpty()) return rejected(issues);

        List<Ref> policies = draft.policyRefs().stream().sorted().distinct().toList();
        SemanticPlan undigested = new SemanticPlan(draft.profileRef(), draft.datasetNamespace(), draft.datasetCode(),
                resolved.get().structure().ref(), components, policies, draft.errorMode(), resolved.get().dependencies(),
                physical.get(), draft.captions(), null, null);
        Map<String, Object> semantic = semanticForm(undigested);
        Map<String, Object> revision = new LinkedHashMap<>(semantic);
        revision.put("captions", sorted(draft.captions()));
        revision.put("physical", physicalForm(physical.get()));
        return new Result(Optional.of(new SemanticPlan(undigested.profileRef(), undigested.datasetNamespace(), undigested.datasetCode(),
                undigested.structureRef(), components, policies, undigested.errorMode(), undigested.dependencies(), physical.get(),
                draft.captions(), CanonicalJson.digest(SEMANTIC_DOMAIN, semantic), CanonicalJson.digest(REVISION_DOMAIN, revision))), List.of());
    }

    /** Component order is semantic (it orders the key); everything derivable is made explicit here, once. */
    private static List<PlannedComponent> normalise(Resolved resolved) {
        Map<String, ConstantBinding> constants = resolved.draft().constantBindings().stream()
                .collect(Collectors.toMap(ConstantBinding::component, b -> b));
        List<PlannedComponent> out = new ArrayList<>();
        int position = 0;
        for (Component c : resolved.structure().components()) {
            MeasureDefinition measure = resolved.measures().get(c.code());
            ConstantBinding constant = constants.get(c.code());
            out.add(new PlannedComponent(c.code(), c.role(), ++position,
                    measure != null ? measure.conceptRef() : c.conceptRef(),
                    measure != null ? measure.representation() : c.representation(),
                    c.measureRef(), measure != null ? measure.unitRef() : null, c.attachment(), c.required(),
                    constant == null ? null : constant.value(), constant != null && constant.overridable()));
        }
        return out;
    }

    private static Map<String, Object> semanticForm(SemanticPlan plan) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("profile", plan.profileRef().wire());
        form.put("dataset", plan.datasetNamespace() + ":" + plan.datasetCode());
        form.put("structure", plan.structureRef().wire());
        form.put("errorMode", plan.errorMode());
        form.put("policies", plan.policyRefs().stream().map(Ref::wire).toList());
        form.put("dependencies", plan.dependencies().stream().map(Ref::wire).toList());
        form.put("components", plan.components().stream().map(StatisticalContractCompiler::componentForm).toList());
        return form;
    }

    private static Map<String, Object> componentForm(PlannedComponent c) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("code", c.code());
        form.put("role", c.role());
        form.put("position", c.position());
        form.put("required", c.required());
        form.put("concept", c.conceptRef().wire());
        form.put("representation", representationForm(c.representation()));
        form.put("measure", c.measureRef() == null ? null : c.measureRef().wire());
        form.put("unit", c.unitRef() == null ? null : c.unitRef().wire());
        if (c.attachment() != null) form.put("attachment", Map.of("level", c.attachment().level(),
                "dimensions", c.attachment().dimensions().stream().sorted().toList(),
                "measure", c.attachment().measure() == null ? "" : c.attachment().measure()));
        form.put("constant", c.constantValue());
        form.put("overridable", c.overridable());
        return form;
    }

    private static Map<String, Object> representationForm(Representation r) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("type", r.logicalType());
        if (r instanceof Representation.Coded coded) form.put("codelist", coded.codelistRef().wire());
        else if (r instanceof Representation.TimePeriod time) form.put("formats", time.formats().stream().map(Enum::name).sorted().toList());
        else if (r instanceof Representation.Numeric n) { form.put("precision", n.precision()); form.put("scale", n.scale()); }
        else if (r instanceof Representation.IntegerRange range) { form.put("min", range.min()); form.put("max", range.max()); }
        else if (r instanceof Representation.BoundedText text) form.put("maxLength", text.maxLength());
        return form;
    }

    private static Object physicalForm(PhysicalPlan physical) {
        return Map.of("provider", physical.providerCode(), "tables", physical.tables().stream().map(t -> Map.of(
                "name", t.name(), "rowRef", t.generatedRowRef(),
                "columns", t.columns().stream().map(col -> Map.of("name", col.name(), "component", col.componentCode() == null ? "" : col.componentCode())).toList())).toList());
    }

    private static Map<String, Object> sorted(Map<String, Map<String, String>> captions) {
        Map<String, Object> out = new TreeMap<>();
        captions.forEach((code, byLanguage) -> out.put(code, new TreeMap<>(byLanguage)));
        return out;
    }

    private static Result rejected(List<ContractIssue> issues) { return new Result(Optional.empty(), List.copyOf(issues)); }
}
