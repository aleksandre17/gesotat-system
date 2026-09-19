package org.base.api.service.contract.approval;

import org.base.api.service.publication.gate.GateEvaluation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** An executable revision declares datasets, and each resolves to a dataset version of its own product. */
@Component
public class StructuralBindingCheck implements ContractApprovalCheck {

    @Override
    public String code() {
        return "STRUCTURAL_BINDING";
    }

    @Override
    public GateEvaluation evaluate(RevisionFacts facts) {
        List<String> failures = new ArrayList<>();
        if (facts.datasetCount() == 0) failures.add("Revision declares no dataset");
        if (facts.foreignProductDatasetCount() > 0)
            failures.add(facts.foreignProductDatasetCount() + " dataset(s) are bound to a dataset version of another product");
        return GateEvaluation.of(code(), failures);
    }
}
