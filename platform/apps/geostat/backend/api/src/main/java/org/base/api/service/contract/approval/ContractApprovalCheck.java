package org.base.api.service.contract.approval;

import org.base.api.service.publication.gate.GateEvaluation;

/**
 * One precondition of contract revision approval. Implementations are pure functions of measured
 * facts and are discovered as Spring beans; adding a check changes neither the approval service nor
 * the API.
 */
public interface ContractApprovalCheck {

    String code();

    GateEvaluation evaluate(RevisionFacts facts);
}
