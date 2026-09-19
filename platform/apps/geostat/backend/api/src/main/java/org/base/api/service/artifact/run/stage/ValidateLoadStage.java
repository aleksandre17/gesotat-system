package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunRepository;
import org.base.api.service.artifact.run.PackageRunStage;
import org.base.api.service.platform.PlatformValidationService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/** Technical validation of the staged rows; rejected rows block the run (contract §12, no partial publication). */
@Component
public class ValidateLoadStage implements PackageRunStage {
    /** Load states that already passed validation; validating again would move the load backwards. */
    private static final Set<String> SETTLED = Set.of("VALIDATED", "PREPARED");

    private final PlatformValidationService validation;
    private final PackageRunRepository runs;

    public ValidateLoadStage(PlatformValidationService validation, PackageRunRepository runs) {
        this.validation = validation;
        this.runs = runs;
    }

    @Override
    public String code() {
        return "VALIDATE_LOAD";
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public Result execute(PackageRun run) {
        long loadId = run.state().requireLong(PackageRunKeys.DATASET_LOAD_ID);
        if (SETTLED.contains(runs.datasetLoadStatus(loadId))) return Result.completed(run.state(), Map.of("replayed", true));
        var receipt = validation.validate(loadId);
        Map<String, Object> detail = Map.of("acceptedRows", receipt.acceptedRows(), "rejectedRows", receipt.rejectedRows());
        return receipt.rejectedRows() > 0 ? Result.blocked(run.state(), "ROWS_REJECTED", detail) : Result.completed(run.state(), detail);
    }
}
