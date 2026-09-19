package org.base.api.service.contract.approval;

import org.base.api.service.platform.ContractLifecycle;
import org.base.api.service.publication.gate.GateEvaluation;
import org.springframework.stereotype.Component;

import java.util.List;

/** Approval is reachable only through the canonical lifecycle ({@link ContractLifecycle}). */
@Component
public class LifecycleTransitionCheck implements ContractApprovalCheck {

    @Override
    public String code() {
        return "LIFECYCLE_TRANSITION";
    }

    @Override
    public GateEvaluation evaluate(RevisionFacts facts) {
        return GateEvaluation.of(code(), ContractLifecycle.permits(facts.status(), ContractLifecycle.State.APPROVED)
                ? List.of() : List.of("Revision in status " + facts.status() + " cannot be approved"));
    }
}
