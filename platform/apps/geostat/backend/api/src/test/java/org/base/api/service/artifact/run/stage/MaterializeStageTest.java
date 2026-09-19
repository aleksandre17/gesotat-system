package org.base.api.service.artifact.run.stage;

import org.base.api.service.artifact.run.PackageRun;
import org.base.api.service.artifact.run.PackageRunKeys;
import org.base.api.service.artifact.run.PackageRunRepository;
import org.base.api.service.artifact.run.PackageRunState;
import org.base.api.service.platform.MaterializationReceipt;
import org.base.api.service.platform.MaterializeRequest;
import org.base.api.service.platform.SemanticMaterializationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterializeStageTest {
    private final SemanticMaterializationService materialization = mock(SemanticMaterializationService.class);
    private final PackageRunRepository runs = mock(PackageRunRepository.class);
    private final MaterializeStage stage = new MaterializeStage(materialization, runs);
    private final PackageRun run = new PackageRun(1, 6, 73, PackageRun.Status.RUNNING, "MATERIALIZE", 1,
            PackageRunState.empty().with(PackageRunKeys.DATASET_SNAPSHOT_ID, 52), null);

    @Test
    void materializesTheSnapshotResolvingItsContractSourceFromTheSnapshot() {
        when(materialization.materialize(new MaterializeRequest(52, 0))).thenReturn(new MaterializationReceipt(52, 9, "ENTITY", 225, "SEMANTIC_REVIEW"));

        var result = stage.execute(run);

        assertFalse(result.blocked());
        assertEquals("SEMANTIC_REVIEW", result.state().require(PackageRunKeys.SNAPSHOT_STATUS));
        assertEquals(225L, result.detail().get("writtenRows"));
    }

    @Test
    void identicalContentThatIsAlreadyPublishedBlocksWithTheObservedSnapshotState() {
        when(materialization.materialize(new MaterializeRequest(52, 0))).thenThrow(new IllegalStateException("Snapshot is not ready or does not match contract version"));
        when(runs.datasetSnapshotStatus(52)).thenReturn("PUBLISHED");

        var result = stage.execute(run);

        assertTrue(result.blocked());
        assertEquals("SNAPSHOT_NOT_MATERIALIZABLE", result.blockedIssueCode());
        assertEquals("PUBLISHED", result.detail().get("snapshotStatus"));
        assertEquals("PUBLISHED", result.state().require(PackageRunKeys.SNAPSHOT_STATUS));
    }
}
