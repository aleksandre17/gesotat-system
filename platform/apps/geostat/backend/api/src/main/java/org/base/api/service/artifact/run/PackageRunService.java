package org.base.api.service.artifact.run;

import org.base.api.service.artifact.ArtifactNotFoundException;
import org.base.api.service.artifact.ArtifactStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.base.api.security.tenancy.TenantAccessGuard;

/**
 * Governed orchestration facade over the complete package pipeline (artifact contract §26). The
 * pipeline is the ordered set of {@link PackageRunStage} beans; this service only sequences them and
 * persists progress after every stage, so a run survives restarts and is safe to retry.
 */
@Service
public class PackageRunService {
    private static final Logger log = LoggerFactory.getLogger(PackageRunService.class);

    private final List<PackageRunStage> stages;
    private final PackageRunRepository runs;
    private final TransactionTemplate transaction;
    private final TenantAccessGuard tenants;

    /** Progress surface: the run plus its append-only stage history. */
    public record RunView(PackageRun run, List<String> pipeline, List<PackageRunRepository.StageRecord> history) {}

    public PackageRunService(List<PackageRunStage> stages, PackageRunRepository runs,
                             @Qualifier("dataPlaneTransactionManager") PlatformTransactionManager transactionManager,
                             TenantAccessGuard tenants) {
        this.stages = stages.stream().sorted(Comparator.comparingInt(PackageRunStage::order)).toList();
        if (this.stages.isEmpty()) throw new IllegalStateException("A package run needs at least one stage");
        if (this.stages.stream().map(PackageRunStage::code).distinct().count() != this.stages.size()
                || this.stages.stream().mapToInt(PackageRunStage::order).distinct().count() != this.stages.size())
            throw new IllegalStateException("Package run stages must have unique codes and orders");
        this.runs = runs;
        this.tenants = tenants;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Idempotent per manifest: the same package checksum always maps to the same run. */
    public RunView start(long manifestId, String requestedBy) {
        if (requestedBy == null || requestedBy.isBlank()) throw new IllegalArgumentException("Requester identity is required");
        // The manifest arrives in the body; this is the tenancy enforcement point of the start route.
        tenants.requireManifest(manifestId);
        long runId = transaction.execute(status -> runs.lockByManifest(manifestId).map(PackageRun::runId).orElseGet(() -> {
            var manifest = runs.manifest(manifestId).orElseThrow(() -> new ArtifactNotFoundException("Manifest " + manifestId + " not found"));
            if (manifest.datasetVersionId() == null)
                throw new IllegalArgumentException("Manifest " + manifestId + " is not bound to a contract dataset and cannot be run");
            return runs.insert(manifestId, manifest.datasetVersionId(), stages.get(0).code(), requestedBy);
        }));
        return view(runId);
    }

    public RunView view(long runId) {
        PackageRun run = runs.find(runId).orElseThrow(() -> new ArtifactNotFoundException("Package run " + runId + " not found"));
        return new RunView(run, stages.stream().map(PackageRunStage::code).toList(), runs.history(runId));
    }

    /** Makes a stopped run runnable again at the stage where it stopped. */
    public RunView retry(long runId) {
        PackageRun run = runs.find(runId).orElseThrow(() -> new ArtifactNotFoundException("Package run " + runId + " not found"));
        if (!runs.release(runId)) throw new IllegalStateException("Package run " + runId + " is " + run.status() + " and cannot be retried");
        return view(runId);
    }

    /** Executes the run from its next stage until it completes or stops. Called by the worker. */
    public void advance(long runId) {
        PackageRun claimed = runs.find(runId).filter(runs::claim).flatMap(run -> runs.find(runId)).orElse(null);
        if (claimed == null) return;
        PackageRun run = claimed;
        int first = indexOf(run.nextStageCode());
        if (first < 0) {
            runs.stop(runId, PackageRun.Status.BLOCKED, "STAGE_NOT_DEPLOYED", run.state());
            return;
        }
        for (int index = first; index < stages.size(); index++) {
            PackageRunStage stage = stages.get(index);
            PackageRunStage.Result result;
            try {
                result = stage.execute(run);
            } catch (ArtifactStorageException | DataAccessException | IOException infrastructure) {
                stop(run, stage, PackageRun.Status.RETRYABLE, "INFRASTRUCTURE_UNAVAILABLE", Map.of());
                log.warn("artifact.package_run retryable run={} stage={} exception={}", runId, stage.code(), infrastructure.getClass().getSimpleName());
                return;
            } catch (Exception rejected) {
                // A deterministic refusal by the pipeline; repeating it unchanged would fail the same way.
                stop(run, stage, PackageRun.Status.BLOCKED, "STAGE_REJECTED", Map.of("reason", String.valueOf(rejected.getMessage())));
                return;
            }
            if (result.blocked()) {
                run = with(run, result.state());
                stop(run, stage, PackageRun.Status.BLOCKED, result.blockedIssueCode(), result.detail());
                return;
            }
            String next = index + 1 < stages.size() ? stages.get(index + 1).code() : null;
            runs.recordStage(runId, run.attempt(), stage.code(), "COMPLETED", null, result.detail());
            if (next == null) runs.complete(runId, result.state());
            else runs.progress(runId, next, result.state());
            run = with(run, result.state());
        }
        log.info("artifact.package_run completed run={} manifest={}", runId, run.manifestId());
    }

    private void stop(PackageRun run, PackageRunStage stage, PackageRun.Status status, String issueCode, Map<String, Object> detail) {
        runs.recordStage(run.runId(), run.attempt(), stage.code(), status.name(), issueCode, detail);
        runs.stop(run.runId(), status, issueCode, run.state());
    }

    private int indexOf(String stageCode) {
        for (int index = 0; index < stages.size(); index++) if (stages.get(index).code().equals(stageCode)) return index;
        return -1;
    }

    private static PackageRun with(PackageRun run, PackageRunState state) {
        return new PackageRun(run.runId(), run.manifestId(), run.datasetVersionId(), run.status(), run.nextStageCode(), run.attempt(), state, run.lastIssueCode());
    }
}
