package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunRepository;
import org.base.api.service.artifact.run.PackageRunStage;
import org.base.api.service.platform.MaterializeRequest;
import org.base.api.service.platform.SemanticMaterializationService;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Canonical materialization; the contract source is resolved from the snapshot, never supplied. */
@Component
public class MaterializeStage implements PackageRunStage {
    private static final long RESOLVE_FROM_SNAPSHOT = 0;

    private final SemanticMaterializationService materialization;
    private final PackageRunRepository runs;

    public MaterializeStage(SemanticMaterializationService materialization, PackageRunRepository runs) {
        this.materialization = materialization;
        this.runs = runs;
    }

    @Override
    public String code() {
        return "MATERIALIZE";
    }

    @Override
    public int order() {
        return 400;
    }

    @Override
    public Result execute(PackageRun run) {
        long snapshotId = run.state().requireLong(PackageRunKeys.DATASET_SNAPSHOT_ID);
        try {
            var receipt = materialization.materialize(new MaterializeRequest(snapshotId, RESOLVE_FROM_SNAPSHOT));
            return Result.completed(run.state().with(PackageRunKeys.SNAPSHOT_STATUS, receipt.status()),
                    Map.of("family", receipt.family(), "writtenRows", receipt.writtenRows(), "status", receipt.status()));
        } catch (IllegalStateException refused) {
            // Materialization owns the rule for which snapshots it accepts; the run only reports the state it met.
            // Identical dataset bytes resolve to their existing snapshot (contract §13): a published one is never rewritten.
            String status = runs.datasetSnapshotStatus(snapshotId);
            return Result.blocked(run.state().with(PackageRunKeys.SNAPSHOT_STATUS, status), "SNAPSHOT_NOT_MATERIALIZABLE",
                    Map.of("datasetSnapshotId", snapshotId, "snapshotStatus", status, "reason", String.valueOf(refused.getMessage())));
        }
    }
}
