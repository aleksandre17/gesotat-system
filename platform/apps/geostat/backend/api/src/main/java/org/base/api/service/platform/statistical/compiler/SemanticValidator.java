package org.base.api.service.platform.statistical.compiler;

import org.base.api.service.platform.statistical.compiler.ContractIssue.Code;
import org.base.api.service.platform.statistical.compiler.ReferenceResolver.Resolved;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Component.Attachment;
import org.base.api.service.platform.statistical.model.ContractDraft.ConstantBinding;
import org.base.api.service.platform.statistical.model.PeriodValue;
import org.base.api.service.platform.statistical.model.Representation;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Semantic stage: grain, attachments, numeric envelope and constant bindings. Provider-independent. */
final class SemanticValidator {
    private final StatisticalContractCompiler.NumericEnvelope envelope;
    private final Set<String> requiredCaptionLanguages;

    SemanticValidator(StatisticalContractCompiler.NumericEnvelope envelope, Set<String> requiredCaptionLanguages) {
        this.envelope = envelope;
        this.requiredCaptionLanguages = Set.copyOf(requiredCaptionLanguages);
    }

    void validate(Resolved resolved, List<ContractIssue> issues) {
        List<Component> components = resolved.structure().components();
        Set<String> seen = new HashSet<>();
        for (Component c : components)
            if (!seen.add(c.code().toUpperCase(Locale.ROOT)))
                issues.add(ContractIssue.of(Code.DUPLICATE_COMPONENT, path(c), "component code is not unique in the structure"));

        List<Component> dimensions = resolved.structure().withRole(Component.Role.DIMENSION);
        Set<String> dimensionCodes = dimensions.stream().map(Component::code).collect(Collectors.toSet());
        Set<String> measureCodes = resolved.structure().withRole(Component.Role.MEASURE).stream().map(Component::code).collect(Collectors.toSet());
        if (dimensions.isEmpty()) issues.add(ContractIssue.of(Code.NO_DIMENSION, "/structure", "an observation needs at least one dimension to be identifiable"));
        if (measureCodes.isEmpty()) issues.add(ContractIssue.of(Code.NO_MEASURE, "/structure", "a statistical structure needs at least one measure"));
        if (dimensions.stream().filter(d -> d.representation() instanceof Representation.TimePeriod).count() > 1)
            issues.add(ContractIssue.of(Code.INVALID_VALUE, "/structure", "at most one time dimension is admitted"));
        for (Component d : dimensions)
            if (d.representation() instanceof Representation.Numeric)
                issues.add(ContractIssue.of(Code.UNCODED_DIMENSION_NOT_ALLOWED, path(d), "a decimal value cannot identify an observation"));

        resolved.measures().forEach((code, measure) -> {
            Representation.Numeric n = measure.representation();
            if (!n.approximate() && (n.precision() < 1 || n.precision() > envelope.maxPrecision() || n.scale() < 0
                    || n.scale() > envelope.maxScale() || n.scale() > n.precision()))
                issues.add(ContractIssue.of(Code.NUMERIC_ENVELOPE_EXCEEDED, "/structure/components/" + code,
                        "exact decimal must fit precision " + envelope.maxPrecision() + " and scale " + envelope.maxScale()));
        });

        for (Component a : resolved.structure().withRole(Component.Role.ATTRIBUTE)) attachment(a, dimensionCodes, measureCodes, issues);
        constants(resolved, dimensions, issues);
        captions(resolved, seen, issues);
    }

    private void attachment(Component attribute, Set<String> dimensions, Set<String> measures, List<ContractIssue> issues) {
        Attachment at = attribute.attachment();
        if (at == null) { issues.add(ContractIssue.of(Code.INVALID_ATTACHMENT, path(attribute), "attachment is required")); return; }
        switch (at.level()) {
            case MEASURE -> {
                if (at.measure() == null || !measures.contains(at.measure()))
                    issues.add(ContractIssue.of(Code.INVALID_ATTACHMENT, path(attribute), "attachment must name a measure of this structure"));
            }
            case DIMENSION_GROUP -> {
                Set<String> group = new HashSet<>(at.dimensions());
                if (group.isEmpty() || group.size() != at.dimensions().size() || !dimensions.containsAll(group) || group.size() == dimensions.size())
                    issues.add(ContractIssue.of(Code.INVALID_ATTACHMENT, path(attribute), "group must be a non-empty proper subset of the dimensions, without repeats"));
            }
            default -> { }
        }
    }

    private void constants(Resolved resolved, List<Component> dimensions, List<ContractIssue> issues) {
        Map<String, Component> byCode = dimensions.stream().collect(Collectors.toMap(Component::code, d -> d, (a, b) -> a));
        Set<String> bound = new HashSet<>();
        for (ConstantBinding binding : resolved.draft().constantBindings()) {
            String path = "/constantBindings/" + binding.component();
            Component dimension = byCode.get(binding.component());
            if (dimension == null) { issues.add(ContractIssue.of(Code.CONSTANT_BINDING_INVALID, path, "only a dimension of this structure can be bound")); continue; }
            if (!bound.add(binding.component())) issues.add(ContractIssue.of(Code.CONSTANT_BINDING_INVALID, path, "component is bound more than once"));
            if (dimension.representation() instanceof Representation.Coded && !resolved.codelists().get(dimension.code()).codes().contains(binding.value()))
                issues.add(ContractIssue.of(Code.CONSTANT_BINDING_INVALID, path, "value is not a code of the pinned codelist"));
            if (dimension.representation() instanceof Representation.TimePeriod time && PeriodValue.parse(binding.value(), time.formats()).isEmpty())
                issues.add(ContractIssue.of(Code.CONSTANT_BINDING_INVALID, path, "value is not a period in an admitted format"));
        }
    }

    private void captions(Resolved resolved, Set<String> upperCodes, List<ContractIssue> issues) {
        Map<String, Map<String, String>> captions = resolved.draft().captions();
        for (String code : captions.keySet())
            if (!upperCodes.contains(code.toUpperCase(Locale.ROOT)))
                issues.add(ContractIssue.of(Code.INVALID_VALUE, "/presentation/captions/" + code, "caption names an unknown component"));
        for (Component c : resolved.structure().components())
            for (String language : requiredCaptionLanguages) {
                String caption = captions.getOrDefault(c.code(), Map.of()).get(language);
                if (caption == null || caption.isBlank())
                    issues.add(ContractIssue.of(Code.MISSING_CAPTION, "/presentation/captions/" + c.code() + "/" + language, "caption is required in this language"));
            }
    }

    private static String path(Component c) { return "/structure/components/" + c.code(); }
}
