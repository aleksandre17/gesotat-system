package org.base.api.service.platform.statistical.registry;

import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.model.StructureDefinition;

import java.util.Optional;
import java.util.Set;

/**
 * Port to the versioned Control registry. Every lookup is scope-bound: an entry the caller may not see is
 * indistinguishable from a missing one, so resolution never leaks another tenant's definitions (register Q44).
 */
public interface StatisticalRegistry {

    /** The caller's resolution scope; opaque to the compiler. */
    record Scope(String productCode) {
        public Scope { if (productCode == null || productCode.isBlank()) throw new IllegalArgumentException("scope is required"); }
    }

    enum Lifecycle { PROPOSED, APPROVED, SUPERSEDED }

    /** Single semantic authority for a measure: concept, exact representation and unit (register Q32). */
    record MeasureDefinition(Ref ref, Ref conceptRef, Representation.Numeric representation, Ref unitRef) { }

    record Codelist(Ref ref, Set<String> codes) {
        public Codelist { codes = Set.copyOf(codes); }
    }

    Optional<StructureDefinition> structure(Ref ref, Scope scope);

    Optional<MeasureDefinition> measure(Ref ref, Scope scope);

    Optional<Codelist> codelist(Ref ref, Scope scope);

    /** Lifecycle of any visible entry (concept, unit, policy, profile included); empty when not visible. */
    Optional<Lifecycle> lifecycle(Ref ref, Scope scope);
}
