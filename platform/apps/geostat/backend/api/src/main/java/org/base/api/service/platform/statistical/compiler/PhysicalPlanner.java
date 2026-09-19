package org.base.api.service.platform.statistical.compiler;

import org.base.api.service.platform.statistical.compiler.ContractIssue.Code;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.PhysicalNameStrategy;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalColumn;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalTable;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Capability and plan stages: maps authoring columns to provider tables. A structure wider than the
 * provider allows is split vertically with the full key repeated in every part; what cannot be held
 * losslessly is rejected here, before approval, never at generation time (register Q35, Q37).
 */
final class PhysicalPlanner {
    static final String ROW_REF = "row_ref";

    Optional<PhysicalPlan> plan(Ref structureRef, List<PlannedComponent> components, ProviderCapabilities caps, List<ContractIssue> issues) {
        int before = issues.size();
        for (PlannedComponent c : components)
            if (c.representation() instanceof Representation.Numeric n && !n.approximate()
                    && (!caps.exactDecimal() || n.precision() > caps.maxDecimalPrecision()))
                issues.add(ContractIssue.of(Code.CAPABILITY_EXCEEDED, "/structure/components/" + c.code(),
                        caps.providerCode() + " cannot hold an exact decimal of precision " + n.precision()));

        List<PlannedComponent> keys = columns(components, Component.Role.DIMENSION);
        boolean rowRef = keys.size() > caps.maxUniqueIndexFields();
        int fixed = keys.size() + (rowRef ? 1 : 0);
        int room = caps.maxColumnsPerTable() - fixed;

        // A measure travels with the attributes attached to it, so a split never separates a value from its status.
        List<List<PlannedComponent>> bundles = new ArrayList<>();
        List<PlannedComponent> observationLevel = new ArrayList<>();
        for (PlannedComponent attribute : columns(components, Component.Role.ATTRIBUTE))
            if (attribute.attachment().measure() == null) observationLevel.add(attribute);
        for (PlannedComponent measure : columns(components, Component.Role.MEASURE)) {
            List<PlannedComponent> bundle = new ArrayList<>(List.of(measure));
            for (PlannedComponent attribute : columns(components, Component.Role.ATTRIBUTE))
                if (measure.code().equals(attribute.attachment().measure())) bundle.add(attribute);
            bundles.add(bundle);
        }
        if (!observationLevel.isEmpty()) bundles.add(0, observationLevel);

        for (List<PlannedComponent> bundle : bundles)
            if (bundle.size() > room) issues.add(ContractIssue.of(Code.CAPABILITY_EXCEEDED, "/structure",
                    caps.providerCode() + " cannot hold the key and one measure bundle within " + caps.maxColumnsPerTable() + " columns"));
        if (issues.size() > before) return Optional.empty();

        List<List<PlannedComponent>> parts = new ArrayList<>();
        List<PlannedComponent> current = new ArrayList<>();
        for (List<PlannedComponent> bundle : bundles) {
            if (!current.isEmpty() && current.size() + bundle.size() > room) { parts.add(current); current = new ArrayList<>(); }
            current.addAll(bundle);
        }
        parts.add(current);

        Set<String> tableNames = new HashSet<>();
        List<PhysicalTable> tables = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            String preferred = "stat_" + structureRef.code() + (parts.size() == 1 ? "" : "_p" + (i + 1));
            String table = PhysicalNameStrategy.allocate(preferred, structureRef.wire() + "#" + i, tableNames, caps);
            Set<String> columnNames = new HashSet<>();
            List<PhysicalColumn> columns = new ArrayList<>();
            if (rowRef) columns.add(new PhysicalColumn(PhysicalNameStrategy.allocate(ROW_REF, ROW_REF, columnNames, caps), null));
            for (PlannedComponent key : keys) columns.add(column(key, structureRef, columnNames, caps));
            for (PlannedComponent value : parts.get(i)) columns.add(column(value, structureRef, columnNames, caps));
            tables.add(new PhysicalTable(table, columns, rowRef));
        }
        return Optional.of(new PhysicalPlan(caps.providerCode(), tables));
    }

    private static PhysicalColumn column(PlannedComponent c, Ref structureRef, Set<String> taken, ProviderCapabilities caps) {
        return new PhysicalColumn(PhysicalNameStrategy.allocate(c.code(), structureRef.wire() + "/" + c.code(), taken, caps), c.code());
    }

    private static List<PlannedComponent> columns(List<PlannedComponent> components, Component.Role role) {
        return components.stream().filter(c -> c.role() == role && c.isAuthoringColumn()).toList();
    }
}
