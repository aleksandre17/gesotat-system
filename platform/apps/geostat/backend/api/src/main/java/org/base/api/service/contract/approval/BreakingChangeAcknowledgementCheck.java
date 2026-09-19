package org.base.api.service.contract.approval;

import org.base.api.service.publication.gate.GateEvaluation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A revision that breaks consumers of the currently approved one is approvable only when the
 * revision itself declares the break and the approver acknowledges it with a reason.
 */
@Component
public class BreakingChangeAcknowledgementCheck implements ContractApprovalCheck {
    static final String DECLARED_BREAKING_MODE = "BREAKING_NEW_REVISION";

    @Override
    public String code() {
        return "BREAKING_CHANGE_ACKNOWLEDGEMENT";
    }

    @Override
    public GateEvaluation evaluate(RevisionFacts facts) {
        if (facts.currentApprovedRevision() == null) return GateEvaluation.notApplicable(code(), "No approved revision to compare with");
        if (!facts.breaking()) return GateEvaluation.of(code(), List.of());
        List<String> failures = new ArrayList<>();
        if (!DECLARED_BREAKING_MODE.equals(facts.compatibilityMode()))
            failures.add("Revision declares " + facts.compatibilityMode() + " but breaks revision " + facts.currentApprovedRevision());
        if (facts.breakingAcknowledgement() == null || facts.breakingAcknowledgement().isBlank())
            failures.add("Breaking changes require an acknowledgement reason");
        if (!failures.isEmpty()) failures.addAll(facts.breakingChanges());
        return GateEvaluation.of(code(), failures);
    }
}
