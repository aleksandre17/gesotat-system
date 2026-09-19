package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunStage;
import org.base.api.service.platform.PlatformSnapshotPreparationService;
import org.base.api.service.platform.PrepareSnapshotRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Creates (or, on replay, returns) the immutable snapshot candidate of the validated load. */
@Component
public class PrepareSnapshotStage implements PackageRunStage {
    private final PlatformSnapshotPreparationService preparation;

    public PrepareSnapshotStage(PlatformSnapshotPreparationService preparation) {
        this.preparation = preparation;
    }

    @Override
    public String code() {
        return "PREPARE_SNAPSHOT";
    }

    @Override
    public int order() {
        return 300;
    }

    @Override
    public Result execute(PackageRun run) {
        long snapshotId = preparation.prepare(new PrepareSnapshotRequest(run.state().requireLong(PackageRunKeys.DATASET_LOAD_ID),
                run.state().requireLong(PackageRunKeys.INGEST_ARTIFACT_ID), run.state().require(PackageRunKeys.DATASET_CHECKSUM)));
        return Result.completed(run.state().with(PackageRunKeys.DATASET_SNAPSHOT_ID, snapshotId), Map.of("datasetSnapshotId", snapshotId));
    }
}
