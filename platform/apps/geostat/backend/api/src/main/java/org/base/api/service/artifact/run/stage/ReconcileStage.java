package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.ArtifactReconciliationService;
import org.base.api.service.artifact.GateResult;
import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunStage;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Records the ARTIFACT_RECONCILIATION gate over the bound snapshot. */
@Component
public class ReconcileStage implements PackageRunStage {
    private final ArtifactReconciliationService reconciliation;

    public ReconcileStage(ArtifactReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @Override
    public String code() {
        return "RECONCILE";
    }

    @Override
    public int order() {
        return 600;
    }

    @Override
    public Result execute(PackageRun run) {
        var report = reconciliation.reconcile(run.state().requireLong(PackageRunKeys.DATASET_SNAPSHOT_ID));
        Map<String, Object> detail = Map.of("result", report.result().name(), "entities", report.entities(), "attachments", report.attachments());
        if (report.result() == GateResult.FAIL) return Result.blocked(run.state(), "RECONCILIATION_FAILED", detail);
        return Result.completed(report.attachmentChecksum() == null ? run.state()
                : run.state().with(PackageRunKeys.ATTACHMENT_CHECKSUM, report.attachmentChecksum()), detail);
    }
}
