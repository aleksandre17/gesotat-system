package org.base.api.service.publication.gate;

import org.base.api.service.artifact.GateResult;

import java.util.List;

/** Outcome of one gate for one snapshot, with the reasons behind a failure or non-applicability. */
public record GateEvaluation(String gateCode, GateResult result, List<String> findings) {
    public GateEvaluation {
        findings = List.copyOf(findings);
    }

    public static GateEvaluation of(String gateCode, List<String> failures) {
        return new GateEvaluation(gateCode, failures.isEmpty() ? GateResult.PASS : GateResult.FAIL, failures);
    }

    public static GateEvaluation notApplicable(String gateCode, String reason) {
        return new GateEvaluation(gateCode, GateResult.NOT_APPLICABLE, List.of(reason));
    }
}
