package org.base.api.service.artifact;

import org.base.api.service.platform.PlatformJobLeaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtifactObjectIntegrityAuditServiceTest {
    private static final String SHA = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final ArtifactRegistry.AuditCandidate CANDIDATE =
            new ArtifactRegistry.AuditCandidate(7, SHA, 3, "geostat-ingest", "artifacts/sha256/ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad.bin");

    @Test
    void successfulPassPersistsVerifiedStateAndRunEvidence() {
        Fixture f = new Fixture(true);
        when(f.stores.getIfAvailable()).thenReturn(f.storage);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(true);
        when(f.registry.startAuditRun()).thenReturn(21L);
        when(f.registry.auditCandidates(100, 1440, 10)).thenReturn(List.of(CANDIDATE));
        when(f.storage.stat(CANDIDATE_LOCATION)).thenReturn(Optional.of(new ArtifactObjectStore.ObjectStat(3, "application/octet-stream")));
        when(f.storage.sha256(CANDIDATE_LOCATION)).thenReturn(SHA);

        f.service.onApplicationReady();
        f.service.auditDueObjects();

        verify(f.registry).recordAuditResult(21L, CANDIDATE, VerificationStatus.VERIFIED, 3L, SHA);
        verify(f.registry).completeAuditRun(21L, "COMPLETED", null);
        verify(f.metrics).integrityAudit("COMPLETED");
        verify(f.lease).release("ARTIFACT_OBJECT_INTEGRITY_AUDIT");
    }

    @Test
    void missingObjectBecomesAnExplicitFailClosedIssue() {
        Fixture f = new Fixture(true);
        when(f.stores.getIfAvailable()).thenReturn(f.storage);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(true);
        when(f.registry.startAuditRun()).thenReturn(22L);
        when(f.registry.auditCandidates(100, 1440, 10)).thenReturn(List.of(CANDIDATE));
        when(f.storage.stat(CANDIDATE_LOCATION)).thenReturn(Optional.empty());

        f.service.onApplicationReady();
        f.service.auditDueObjects();

        verify(f.registry).recordAuditResult(22L, CANDIDATE, VerificationStatus.MISSING, null, null);
        verify(f.storage, never()).sha256(CANDIDATE_LOCATION);
        verify(f.registry).completeAuditRun(22L, "COMPLETED", null);
    }

    @Test
    void sameSizedObjectWithChangedChecksumIsMarkedMismatched() {
        Fixture f = new Fixture(true);
        when(f.stores.getIfAvailable()).thenReturn(f.storage);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(true);
        when(f.registry.startAuditRun()).thenReturn(24L);
        when(f.registry.auditCandidates(100, 1440, 10)).thenReturn(List.of(CANDIDATE));
        when(f.storage.stat(CANDIDATE_LOCATION)).thenReturn(Optional.of(new ArtifactObjectStore.ObjectStat(3, "application/octet-stream")));
        when(f.storage.sha256(CANDIDATE_LOCATION)).thenReturn("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        f.service.onApplicationReady();
        f.service.auditDueObjects();

        verify(f.registry).recordAuditResult(24L, CANDIDATE, VerificationStatus.CHECKSUM_MISMATCH, 3L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        verify(f.registry).completeAuditRun(24L, "COMPLETED", null);
    }

    @Test
    void providerFailureLeavesObjectDueAndRunRetryable() {
        Fixture f = new Fixture(true);
        when(f.stores.getIfAvailable()).thenReturn(f.storage);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(true);
        when(f.registry.startAuditRun()).thenReturn(23L);
        when(f.registry.auditCandidates(100, 1440, 10)).thenReturn(List.of(CANDIDATE));
        when(f.storage.stat(CANDIDATE_LOCATION)).thenThrow(new ArtifactStorageException("offline", null));

        f.service.onApplicationReady();
        f.service.auditDueObjects();

        verify(f.registry, never()).recordAuditResult(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(CANDIDATE),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(f.registry).completeAuditRun(23L, "RETRYABLE", "OBJECT_STORAGE_UNAVAILABLE");
        verify(f.metrics).integrityAudit("RETRYABLE");
    }

    @Test
    void scheduledWorkWaitsForSchemaReadinessAndHonorsDisableFlag() {
        Fixture waiting = new Fixture(true);
        waiting.service.auditDueObjects();
        verifyNoInteractions(waiting.registry, waiting.stores, waiting.lease);

        Fixture disabled = new Fixture(false);
        disabled.service.onApplicationReady();
        disabled.service.auditDueObjects();
        verifyNoInteractions(disabled.registry, disabled.stores, disabled.lease);
    }

    @Test
    void anotherReplicaHoldingTheLeasePreventsDuplicateStorageReads() {
        Fixture f = new Fixture(true);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(false);

        f.service.onApplicationReady();
        f.service.auditDueObjects();

        verifyNoInteractions(f.registry, f.stores);
        verify(f.lease, never()).release("ARTIFACT_OBJECT_INTEGRITY_AUDIT");
    }

    @Test
    void lostLeaseDuringStorageReadCannotCommitAStaleResult() {
        Fixture f = new Fixture(true);
        when(f.stores.getIfAvailable()).thenReturn(f.storage);
        when(f.lease.acquire(anyString(), anyInt())).thenReturn(true, true, false);
        when(f.registry.startAuditRun()).thenReturn(25L);
        when(f.registry.auditCandidates(100, 1440, 10)).thenReturn(List.of(CANDIDATE));
        when(f.storage.stat(CANDIDATE_LOCATION)).thenReturn(Optional.of(new ArtifactObjectStore.ObjectStat(3, "application/octet-stream")));
        when(f.storage.sha256(CANDIDATE_LOCATION)).thenReturn(SHA);

        f.service.onApplicationReady();
        f.service.auditDueObjects();

        verify(f.registry, never()).recordAuditResult(25L, CANDIDATE, VerificationStatus.VERIFIED, 3L, SHA);
        verify(f.registry).completeAuditRun(25L, "RETRYABLE", "AUDIT_LEASE_LOST");
    }

    private static final ArtifactObjectStore.ObjectLocation CANDIDATE_LOCATION =
            new ArtifactObjectStore.ObjectLocation("geostat-ingest", CANDIDATE.objectKey());

    private static final class Fixture {
        final ArtifactRegistry registry = mock(ArtifactRegistry.class);
        @SuppressWarnings("unchecked")
        final ObjectProvider<ArtifactObjectStore> stores = mock(ObjectProvider.class);
        final ArtifactObjectStore storage = mock(ArtifactObjectStore.class);
        final ArtifactMetrics metrics = mock(ArtifactMetrics.class);
        final PlatformJobLeaseService lease = mock(PlatformJobLeaseService.class);
        final ArtifactObjectIntegrityAuditService service;

        Fixture(boolean migrationEnabled) {
            service = new ArtifactObjectIntegrityAuditService(registry, stores, metrics, new ArtifactProperties(), lease, migrationEnabled);
        }
    }
}
