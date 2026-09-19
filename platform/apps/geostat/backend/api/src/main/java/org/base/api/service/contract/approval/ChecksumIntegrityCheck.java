package org.base.api.service.contract.approval;

import org.base.api.service.publication.gate.GateEvaluation;
import org.springframework.stereotype.Component;

import java.util.List;

/** The stored checksum must still describe the stored document; what is approved is what was reviewed. */
@Component
public class ChecksumIntegrityCheck implements ContractApprovalCheck {

    @Override
    public String code() {
        return "CHECKSUM_INTEGRITY";
    }

    @Override
    public GateEvaluation evaluate(RevisionFacts facts) {
        return GateEvaluation.of(code(), facts.checksumMatchesDocument()
                ? List.of() : List.of("Contract document does not match its recorded checksum"));
    }
}
