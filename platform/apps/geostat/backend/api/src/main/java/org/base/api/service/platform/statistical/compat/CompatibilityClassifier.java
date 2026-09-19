package org.base.api.service.platform.statistical.compat;

import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Classifies the step between two compiled revisions of one dataset contract (register Q45). The result is
 * the minimum version increment the new revision must carry; a human may only choose a stricter one.
 */
public final class CompatibilityClassifier {

    public enum Level { IDENTICAL, PRESENTATION, BACKWARD_COMPATIBLE, BREAKING }

    public record Verdict(Level level, List<String> reasons) {
        /** Value of the existing {@code compatibility_mode} column for this verdict. */
        public String compatibilityMode() { return level == Level.BREAKING ? "BREAKING_NEW_REVISION" : "BACKWARD_COMPATIBLE"; }
    }

    private CompatibilityClassifier() { }

    public static Verdict classify(SemanticPlan previous, SemanticPlan next) {
        if (previous.semanticDigest().equals(next.semanticDigest()))
            return new Verdict(previous.revisionDigest().equals(next.revisionDigest()) ? Level.IDENTICAL : Level.PRESENTATION, List.of());

        List<String> breaking = new ArrayList<>();
        List<String> compatible = new ArrayList<>();
        if (!previous.datasetCode().equals(next.datasetCode()) || !previous.datasetNamespace().equals(next.datasetNamespace()))
            breaking.add("dataset identity changed");

        List<String> oldKey = previous.withRole(Component.Role.DIMENSION).stream().map(PlannedComponent::code).toList();
        List<String> newKey = next.withRole(Component.Role.DIMENSION).stream().map(PlannedComponent::code).toList();
        if (!oldKey.equals(newKey)) breaking.add("observation key changed: " + oldKey + " -> " + newKey);

        for (PlannedComponent before : previous.components()) {
            PlannedComponent after = next.components().stream().filter(c -> c.code().equals(before.code())).findFirst().orElse(null);
            if (after == null) { breaking.add("component removed: " + before.code()); continue; }
            if (before.role() != after.role()) { breaking.add("role changed: " + before.code()); continue; }
            if (!Objects.equals(before.constantValue(), after.constantValue())) breaking.add("constant binding changed: " + before.code());
            if (!Objects.equals(before.attachment(), after.attachment())) breaking.add("attachment changed: " + before.code());
            if (!Objects.equals(before.unitRef(), after.unitRef())) breaking.add("unit changed: " + before.code());
            if (!before.required() && after.required()) breaking.add("component became required: " + before.code());
            step(before.code() + " concept", before.conceptRef(), after.conceptRef(), breaking, compatible);
            step(before.code() + " measure", before.measureRef(), after.measureRef(), breaking, compatible);
            representation(before, after, breaking, compatible);
        }
        for (PlannedComponent added : next.components()) {
            if (previous.components().stream().anyMatch(c -> c.code().equals(added.code()))) continue;
            if (added.role() == Component.Role.DIMENSION) continue; // already reported as a key change
            if (added.role() == Component.Role.ATTRIBUTE && added.required()) breaking.add("required attribute added: " + added.code());
            else compatible.add("component added: " + added.code());
        }
        if (!previous.policyRefs().equals(next.policyRefs())) compatible.add("policy set changed");
        if (previous.errorMode() != next.errorMode()) compatible.add("error mode changed");

        if (!breaking.isEmpty()) return new Verdict(Level.BREAKING, List.copyOf(breaking));
        return new Verdict(Level.BACKWARD_COMPATIBLE, List.copyOf(compatible));
    }

    private static void representation(PlannedComponent before, PlannedComponent after, List<String> breaking, List<String> compatible) {
        Representation a = before.representation(), b = after.representation();
        if (a instanceof Representation.Coded x && b instanceof Representation.Coded y) step(before.code() + " codelist", x.codelistRef(), y.codelistRef(), breaking, compatible);
        else if (a instanceof Representation.Numeric x && b instanceof Representation.Numeric y && x.approximate() == y.approximate()) {
            if (y.scale() < x.scale() || y.precision() - y.scale() < x.precision() - x.scale()) breaking.add("numeric envelope narrowed: " + before.code());
            else if (!x.equals(y)) compatible.add("numeric envelope widened: " + before.code());
        } else if (a instanceof Representation.TimePeriod x && b instanceof Representation.TimePeriod y) {
            if (!y.formats().containsAll(x.formats())) breaking.add("time format withdrawn: " + before.code());
            else if (!x.equals(y)) compatible.add("time format added: " + before.code());
        } else if (!Objects.equals(a, b)) breaking.add("representation changed: " + before.code());
    }

    /** Same identity and major, not older: compatible. Anything else breaks readers. */
    private static void step(String what, Ref before, Ref after, List<String> breaking, List<String> compatible) {
        if (Objects.equals(before, after)) return;
        if (before == null || after == null || !before.namespace().equals(after.namespace()) || !before.code().equals(after.code())
                || before.version().major() != after.version().major() || after.version().compareTo(before.version()) < 0)
            breaking.add(what + " changed incompatibly: " + before + " -> " + after);
        else compatible.add(what + " advanced: " + before + " -> " + after);
    }
}
