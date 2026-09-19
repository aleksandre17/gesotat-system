package org.base.api.service.platform.statistical.compiler;

import org.base.api.service.platform.statistical.compiler.ContractIssue.Code;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.ContractDraft;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.model.StructureDefinition;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Codelist;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Lifecycle;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.MeasureDefinition;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Resolve stage: turns every reference into one visible, exactly versioned registry entry and records the
 * transitive dependency closure that approval pins. An invisible entry reports as unresolved, not forbidden.
 */
final class ReferenceResolver {

    /** A draft whose references all resolved. */
    record Resolved(ContractDraft draft, StructureDefinition structure, Map<String, MeasureDefinition> measures,
                    Map<String, Codelist> codelists, List<Ref> dependencies) { }

    private final StatisticalRegistry registry;

    ReferenceResolver(StatisticalRegistry registry) { this.registry = registry; }

    Optional<Resolved> resolve(ContractDraft draft, Scope scope, StatisticalContractCompiler.Mode mode, List<ContractIssue> issues) {
        int before = issues.size();
        TreeSet<Ref> closure = new TreeSet<>();
        require(draft.profileRef(), "/profileRef", scope, mode, closure, issues);
        draft.policyRefs().forEach(p -> require(p, "/policyRefs", scope, mode, closure, issues));

        StructureDefinition structure = draft.inlineStructure();
        if (draft.existingStructureRef() != null) {
            require(draft.existingStructureRef(), "/structure/existingStructureRef", scope, mode, closure, issues);
            structure = registry.structure(draft.existingStructureRef(), scope).orElse(null);
        }
        Map<String, MeasureDefinition> measures = new HashMap<>();
        Map<String, Codelist> codelists = new HashMap<>();
        if (structure != null) for (Component c : structure.components()) {
            String path = "/structure/components/" + c.code();
            if (c.conceptRef() != null) require(c.conceptRef(), path + "/conceptRef", scope, mode, closure, issues);
            if (c.representation() instanceof Representation.Coded coded && require(coded.codelistRef(), path + "/codelistRef", scope, mode, closure, issues))
                registry.codelist(coded.codelistRef(), scope).ifPresentOrElse(list -> codelists.put(c.code(), list),
                        () -> issues.add(ContractIssue.of(Code.UNRESOLVED_REFERENCE, path + "/codelistRef", "reference is not a codelist")));
            if (c.measureRef() != null && require(c.measureRef(), path + "/measureRef", scope, mode, closure, issues)) {
                Optional<MeasureDefinition> measure = registry.measure(c.measureRef(), scope);
                if (measure.isEmpty()) issues.add(ContractIssue.of(Code.UNRESOLVED_REFERENCE, path + "/measureRef", "reference is not a measure"));
                else {
                    measures.put(c.code(), measure.get());
                    require(measure.get().conceptRef(), path + "/measureRef/conceptRef", scope, mode, closure, issues);
                    if (measure.get().unitRef() != null) require(measure.get().unitRef(), path + "/measureRef/unitRef", scope, mode, closure, issues);
                }
            }
        }
        if (issues.size() > before || structure == null) return Optional.empty();
        return Optional.of(new Resolved(draft, structure, Map.copyOf(measures), Map.copyOf(codelists), List.copyOf(closure)));
    }

    private boolean require(Ref ref, String path, Scope scope, StatisticalContractCompiler.Mode mode, TreeSet<Ref> closure, List<ContractIssue> issues) {
        Optional<Lifecycle> lifecycle = registry.lifecycle(ref, scope);
        if (lifecycle.isEmpty()) {
            issues.add(ContractIssue.of(Code.UNRESOLVED_REFERENCE, path, "reference does not resolve in this scope: " + ref));
            return false;
        }
        if (lifecycle.get() != Lifecycle.APPROVED && mode == StatisticalContractCompiler.Mode.APPROVAL) {
            issues.add(ContractIssue.of(Code.REFERENCE_NOT_APPROVED, path, "approval requires an approved entry: " + ref));
            return false;
        }
        closure.add(ref);
        return true;
    }
}
