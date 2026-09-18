package org.base.api.service.artifact;

import org.base.api.service.platform.PlatformJobLeaseService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtifactRelationIntegrityAuditServiceTest {
    @Test
    void reconcilesDueSnapshotsFromCurrentAndRetiredApprovedDefinitions() {
        Fixture f = new Fixture(true);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(true);
        when(f.contracts.reconcilableDatasetVersions()).thenReturn(List.of(10L, 20L));
        when(f.attachments.reconciliationCandidates(List.of(10L, 20L), 100, 1440)).thenReturn(List.of(51L, 52L));

        f.service.onApplicationReady();
        f.service.reconcileDueSnapshots();

        verify(f.reconciliation).reconcile(51L);
        verify(f.reconciliation).reconcile(52L);
        verify(f.metrics).relationAudit("COMPLETED");
        verify(f.lease).release("ARTIFACT_RELATION_INTEGRITY_AUDIT");
    }

    @Test
    void readinessDisableAndAnotherReplicaLeasePreventDatabaseWork() {
        Fixture waiting = new Fixture(true);
        waiting.service.reconcileDueSnapshots();
        verifyNoInteractions(waiting.contracts, waiting.attachments, waiting.reconciliation);

        Fixture disabled = new Fixture(false);
        disabled.service.onApplicationReady();
        disabled.service.reconcileDueSnapshots();
        verifyNoInteractions(disabled.contracts, disabled.attachments, disabled.reconciliation);

        Fixture leased = new Fixture(true);
        when(leased.lease.acquire(anyString(), anyInt())).thenReturn(false);
        leased.service.onApplicationReady();
        leased.service.reconcileDueSnapshots();
        verifyNoInteractions(leased.contracts, leased.attachments, leased.reconciliation);
        verify(leased.lease, never()).release("ARTIFACT_RELATION_INTEGRITY_AUDIT");
    }

    @Test
    void lostLeaseStopsTheBatchAndLeavesRemainingSnapshotsDue() {
        Fixture f = new Fixture(true);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(true, true, false);
        when(f.contracts.reconcilableDatasetVersions()).thenReturn(List.of(10L));
        when(f.attachments.reconciliationCandidates(List.of(10L), 100, 1440)).thenReturn(List.of(51L, 52L));

        f.service.onApplicationReady();
        f.service.reconcileDueSnapshots();

        verify(f.reconciliation).reconcile(51L);
        verify(f.reconciliation, never()).reconcile(52L);
        verify(f.metrics).relationAudit("RETRYABLE");
    }

    private static final class Fixture {
        final ArtifactContractResolver contracts = mock(ArtifactContractResolver.class);
        final ArtifactAttachmentRepository attachments = mock(ArtifactAttachmentRepository.class);
        final ArtifactReconciliationService reconciliation = mock(ArtifactReconciliationService.class);
        final ArtifactMetrics metrics = mock(ArtifactMetrics.class);
        final ArtifactProperties properties = new ArtifactProperties();
        final PlatformJobLeaseService lease = mock(PlatformJobLeaseService.class);
        final ArtifactRelationIntegrityAuditService service;

        Fixture(boolean migrationsEnabled) {
            service = new ArtifactRelationIntegrityAuditService(contracts, attachments, reconciliation, metrics, properties, lease, migrationsEnabled);
        }
    }
}
