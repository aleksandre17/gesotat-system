package org.base.api.service.platform.statistical.plan;

import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.ContractDraft.ErrorMode;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;

import java.util.List;
import java.util.Map;

/**
 * The single intermediate representation every backend reads: Access emitter, row validator and exporters
 * share this plan, so there is one interpretation of the contract (lifecycle §5). Immutable once digested.
 */
public record SemanticPlan(Ref profileRef, String datasetNamespace, String datasetCode, Ref structureRef,
                           List<PlannedComponent> components, List<Ref> policyRefs, ErrorMode errorMode,
                           List<Ref> dependencies, PhysicalPlan physical,
                           Map<String, Map<String, String>> captions,
                           String semanticDigest, String revisionDigest) {

    /** A component with every derived value resolved. {@code constantValue} is set when bound by contract. */
    public record PlannedComponent(String code, Component.Role role, int position, Ref conceptRef,
                                   Representation representation, Ref measureRef, Ref unitRef,
                                   Component.Attachment attachment, boolean required,
                                   String constantValue, boolean overridable) {
        public boolean isAuthoringColumn() {
            if (constantValue != null && !overridable) return false;
            return role != Component.Role.ATTRIBUTE || attachment.level().variesPerRow();
        }
    }

    public record PhysicalColumn(String name, String componentCode) { }

    /** One authoring table; every part of a vertical split repeats the key columns. */
    public record PhysicalTable(String name, List<PhysicalColumn> columns, boolean generatedRowRef) {
        public PhysicalTable { columns = List.copyOf(columns); }
    }

    public record PhysicalPlan(String providerCode, List<PhysicalTable> tables) {
        public PhysicalPlan { tables = List.copyOf(tables); }
    }

    public SemanticPlan {
        components = List.copyOf(components);
        policyRefs = List.copyOf(policyRefs);
        dependencies = List.copyOf(dependencies);
    }

    public List<PlannedComponent> withRole(Component.Role role) {
        return components.stream().filter(c -> c.role() == role).toList();
    }

    public PlannedComponent component(String code) {
        return components.stream().filter(c -> c.code().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown component: " + code));
    }
}
