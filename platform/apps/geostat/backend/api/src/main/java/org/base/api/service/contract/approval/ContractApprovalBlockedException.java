package org.base.api.service.contract.approval;

import org.base.api.service.publication.gate.GateEvaluation;

import java.util.List;

/** Approval was refused; nothing was written. Carries every check so the caller sees exact reasons. */
public class ContractApprovalBlockedException extends RuntimeException {
    private final transient List<GateEvaluation> checks;

    public ContractApprovalBlockedException(List<GateEvaluation> checks) {
        super("Contract revision approval is blocked");
        this.checks = List.copyOf(checks);
    }

    public List<GateEvaluation> checks() {
        return checks;
    }
}
