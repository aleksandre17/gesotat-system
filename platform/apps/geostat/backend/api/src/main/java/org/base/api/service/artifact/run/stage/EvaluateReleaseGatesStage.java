package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunStage;
import org.base.api.service.publication.gate.ReleaseGateService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evaluates the release gates so the snapshot reaches steward review with evidence. The run ends here:
 * publication is a separate, human-approved operation.
 */
@Component
public class EvaluateReleaseGatesStage implements PackageRunStage {
    private final ReleaseGateService gates;

    public EvaluateReleaseGatesStage(ReleaseGateService gates) {
        this.gates = gates;
    }

    @Override
    public String code() {
        return "EVALUATE_RELEASE_GATES";
    }

    @Override
    public int order() {
        return 700;
    }

    @Override
    public Result execute(PackageRun run) {
        var report = gates.evaluate(run.state().requireLong(PackageRunKeys.DATASET_SNAPSHOT_ID));
        var state = run.state().with(PackageRunKeys.GATE_FACTS_DIGEST, report.factsDigest()).with(PackageRunKeys.SNAPSHOT_STATUS, report.status());
        Map<String, Object> detail = Map.of("releasable", report.releasable(), "status", report.status(), "gates", report.gates());
        return report.releasable() ? Result.completed(state, detail) : Result.blocked(state, "RELEASE_GATES_FAILED", detail);
    }
}
