package org.base.api.service.artifact.run;

import org.base.api.service.artifact.ArtifactNotFoundException;
import org.base.api.service.artifact.ArtifactStorageException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.base.api.security.tenancy.TenantAccessGuards;

class PackageRunServiceTest {
    private final PackageRunRepository runs = mock(PackageRunRepository.class);
    private final List<String> executed = new ArrayList<>();

    @Test
    void stagesRunInDeclaredOrderAndProgressIsPersistedAfterEachOne() {
        PackageRunService service = service(stage("B", 20, run -> completed(run, "b", 2)), stage("A", 10, run -> completed(run, "a", 1)));
        claimable(run("A", PackageRunState.empty()));

        service.advance(1);

        assertEquals(List.of("A", "B"), executed);
        InOrder order = inOrder(runs);
        order.verify(runs).recordStage(1, 1, "A", "COMPLETED", null, Map.of());
        order.verify(runs).progress(1, "B", PackageRunState.empty().with("a", 1));
        order.verify(runs).recordStage(1, 1, "B", "COMPLETED", null, Map.of());
        order.verify(runs).complete(1, PackageRunState.empty().with("a", 1).with("b", 2));
    }

    @Test
    void resumedRunContinuesAtItsNextStageWithItsSavedState() {
        PackageRunService service = service(stage("A", 10, run -> completed(run, "a", 1)),
                stage("B", 20, run -> completed(run, "sawA", run.state().require("a"))));
        claimable(run("B", PackageRunState.empty().with("a", 7)));

        service.advance(1);

        assertEquals(List.of("B"), executed);
        verify(runs).complete(1, PackageRunState.empty().with("a", 7).with("sawA", "7"));
    }

    @Test
    void blockedStageStopsThePipelineAndKeepsTheStageForRetry() {
        PackageRunService service = service(stage("A", 10, run -> PackageRunStage.Result.blocked(run.state(), "ROWS_REJECTED", Map.of("rejectedRows", 3))),
                stage("B", 20, run -> completed(run, "b", 2)));
        claimable(run("A", PackageRunState.empty()));

        service.advance(1);

        assertEquals(List.of("A"), executed);
        verify(runs).recordStage(1, 1, "A", "BLOCKED", "ROWS_REJECTED", Map.of("rejectedRows", 3));
        verify(runs).stop(1, PackageRun.Status.BLOCKED, "ROWS_REJECTED", PackageRunState.empty());
        verify(runs, never()).progress(anyLong(), anyString(), any());
        verify(runs, never()).complete(anyLong(), any());
    }

    @Test
    void infrastructureFailureIsRetryableWhileAPipelineRefusalBlocks() {
        PackageRunService storageDown = service(stage("A", 10, run -> { throw new ArtifactStorageException("down", null); }));
        claimable(run("A", PackageRunState.empty()));
        storageDown.advance(1);
        verify(runs).stop(1, PackageRun.Status.RETRYABLE, "INFRASTRUCTURE_UNAVAILABLE", PackageRunState.empty());

        PackageRunService refused = service(stage("A", 10, run -> { throw new IllegalStateException("Snapshot is PUBLISHED"); }));
        refused.advance(1);
        verify(runs).recordStage(1, 1, "A", "BLOCKED", "STAGE_REJECTED", Map.of("reason", "Snapshot is PUBLISHED"));
    }

    @Test
    void unclaimedRunIsNotExecutedAndUndeployedStageBlocks() {
        PackageRunService service = service(stage("A", 10, run -> completed(run, "a", 1)));
        when(runs.find(1)).thenReturn(Optional.of(run("A", PackageRunState.empty())));
        when(runs.claim(any())).thenReturn(false);
        service.advance(1);
        assertEquals(List.of(), executed);

        claimable(run("REMOVED_STAGE", PackageRunState.empty()));
        service.advance(1);
        verify(runs).stop(1, PackageRun.Status.BLOCKED, "STAGE_NOT_DEPLOYED", PackageRunState.empty());
    }

    @Test
    void startIsIdempotentPerManifestAndRequiresAContractBoundManifest() {
        PackageRunService service = service(stage("A", 10, run -> completed(run, "a", 1)));
        when(runs.find(1)).thenReturn(Optional.of(run("A", PackageRunState.empty())));
        when(runs.lockByManifest(5)).thenReturn(Optional.empty(), Optional.of(run("A", PackageRunState.empty())));
        when(runs.manifest(5)).thenReturn(Optional.of(new PackageRunRepository.ManifestBinding(5, 73L)));
        when(runs.insert(5, 73, "A", "operator")).thenReturn(1L);

        assertEquals(1, service.start(5, "operator").run().runId());
        assertEquals(1, service.start(5, "operator").run().runId());
        verify(runs).insert(5, 73, "A", "operator");

        when(runs.lockByManifest(6)).thenReturn(Optional.empty());
        when(runs.manifest(6)).thenReturn(Optional.of(new PackageRunRepository.ManifestBinding(6, null)));
        assertThrows(IllegalArgumentException.class, () -> service.start(6, "operator"));
        when(runs.manifest(7)).thenReturn(Optional.empty());
        assertThrows(ArtifactNotFoundException.class, () -> service.start(7, "operator"));
        assertThrows(IllegalArgumentException.class, () -> service.start(5, " "));
    }

    @Test
    void retryIsOnlyForStoppedRunsAndAmbiguousPipelinesAreRejectedAtStartup() {
        PackageRunService service = service(stage("A", 10, run -> completed(run, "a", 1)));
        when(runs.find(1)).thenReturn(Optional.of(run("A", PackageRunState.empty())));
        when(runs.release(1)).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.retry(1));

        assertThrows(IllegalStateException.class, () -> service(stage("A", 10, run -> null), stage("A", 20, run -> null)));
        assertThrows(IllegalStateException.class, () -> service(stage("A", 10, run -> null), stage("B", 10, run -> null)));
        assertThrows(IllegalStateException.class, this::service);
    }

    private void claimable(PackageRun run) {
        when(runs.find(1)).thenReturn(Optional.of(run));
        when(runs.claim(any())).thenReturn(true);
    }

    private static PackageRun run(String nextStage, PackageRunState state) {
        return new PackageRun(1, 5, 73, PackageRun.Status.RUNNING, nextStage, 1, state, null);
    }

    private static PackageRunStage.Result completed(PackageRun run, String key, Object value) {
        return PackageRunStage.Result.completed(run.state().with(key, value), Map.of());
    }

    private PackageRunService service(PackageRunStage... stages) {
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new PackageRunService(List.of(stages), runs, transactions, TenantAccessGuards.permitAll());
    }

    private interface StageBody {
        PackageRunStage.Result apply(PackageRun run) throws Exception;
    }

    private PackageRunStage stage(String code, int order, StageBody body) {
        return new PackageRunStage() {
            @Override public String code() { return code; }
            @Override public int order() { return order; }
            @Override public Result execute(PackageRun run) throws Exception {
                executed.add(code);
                return body.apply(run);
            }
        };
    }
}
