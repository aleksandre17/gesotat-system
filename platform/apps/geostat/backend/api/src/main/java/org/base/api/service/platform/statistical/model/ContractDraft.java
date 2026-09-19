package org.base.api.service.platform.statistical.model;

import java.util.List;
import java.util.Map;

/**
 * Canonical authoring model written by both UI and API. Exactly one of {@code existingStructureRef} and
 * {@code inlineStructure} is present; the parser enforces it (register Q31).
 */
public record ContractDraft(Ref profileRef, String datasetNamespace, String datasetCode,
                            Ref existingStructureRef, StructureDefinition inlineStructure,
                            List<ConstantBinding> constantBindings, List<Ref> policyRefs,
                            String sourceProfile, ErrorMode errorMode,
                            Map<String, Map<String, String>> captions) {

    public enum ErrorMode { ATOMIC_REJECT, QUARANTINE_ROWS }

    /** A dimension fixed by the contract instead of repeated per row (register Q38). */
    public record ConstantBinding(String component, String value, boolean overridable) { }

    public ContractDraft {
        constantBindings = constantBindings == null ? List.of() : List.copyOf(constantBindings);
        policyRefs = policyRefs == null ? List.of() : List.copyOf(policyRefs);
        captions = captions == null ? Map.of() : Map.copyOf(captions);
    }
}
