package org.base.api.service.artifact;

import org.base.api.service.artifact.ArtifactUploadIdentityResolver.Identity;
import org.base.api.service.artifact.upload.UploadSession;
import org.base.api.service.artifact.upload.UploadSessionRepository;
import org.base.api.service.artifact.upload.UploadSessionStatus;
import org.base.api.service.platform.PlatformSchemaReadiness;
import org.base.api.service.platform.PlatformSchemaReadyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtifactUploadSessionServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    private static final Identity OWNER = new Identity("t".repeat(64), "o".repeat(64));
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String KEY = "idempotency-key-0001";
    private static final byte[] PART_ONE = "0123456789".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PART_TWO = "abcde".getBytes(StandardCharsets.UTF_8);
    private static final String SHA_ONE = Sha256.ofUtf8("0123456789");
    private static final String SHA_TWO = Sha256.ofUtf8("abcde");

    private final UploadSessionRepository sessions = mock(UploadSessionRepository.class);
    private final ArtifactUploadStagingStore staging = mock(ArtifactUploadStagingStore.class);
    private final ArtifactPackageContractResolver contracts = mock(ArtifactPackageContractResolver.class);
    private final ArtifactPackageService packages = mock(ArtifactPackageService.class);
    private final PlatformSchemaReadiness readiness = new PlatformSchemaReadiness();
    private final ArtifactProperties properties = new ArtifactProperties();
    private ArtifactUploadSessionService service;

    @BeforeEach
    void setUp() {
        properties.setUploadPartBytes(10);
        properties.setMaxUploadBytes(1_000);
        properties.setMaxTenantReservedUploadBytes(100);
        properties.setMaxTenantActiveUploadSessions(2);
        when(contracts.resolve("SITE", 1, "ITEM")).thenReturn(new ArtifactPackageContractResolver.DatasetContract("SITE", 1, "c".repeat(64), 73, "ITEM", "item_table", List.of("id")));
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new ArtifactUploadSessionService(sessions, transactions, provider(staging), contracts, packages, properties, readiness,
                provider(Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void startReservesQuotaOnceAndReplaysTheSameIdempotencyKey() {
        when(sessions.lockByIdempotencyKey(OWNER, KEY)).thenReturn(Optional.empty());
        when(sessions.lockQuota(OWNER.tenantKeyHash())).thenReturn(Optional.empty());

        var receipt = service.start(request(15), OWNER, KEY);

        assertEquals(2, receipt.expectedParts());
        assertEquals("OPEN", receipt.status());
        verify(sessions).reserveQuota(OWNER.tenantKeyHash(), 15, true);

        var created = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sessions).insert(any(), eq(OWNER), eq(KEY), created.capture(), eq("PKG"), any(), eq(15L), eq(10L), eq(2), any());
        when(sessions.lockByIdempotencyKey(OWNER, KEY)).thenReturn(Optional.of(session(UploadSessionStatus.OPEN, 0, created.getValue())));
        assertEquals(ID, service.start(request(15), OWNER, KEY).uploadSessionId());
        assertThrows(ArtifactUploadConflictException.class, () -> service.start(request(16), OWNER, KEY));
        verify(sessions).reserveQuota(anyString(), anyLong(), anyBoolean());
    }

    @Test
    void startFailsClosedOnQuotaSizeAndKey() {
        when(sessions.lockByIdempotencyKey(OWNER, KEY)).thenReturn(Optional.empty());
        when(sessions.lockQuota(OWNER.tenantKeyHash())).thenReturn(Optional.of(new UploadSession.Quota(90, 1)));
        assertThrows(ArtifactUploadQuotaExceededException.class, () -> service.start(request(15), OWNER, KEY));
        when(sessions.lockQuota(OWNER.tenantKeyHash())).thenReturn(Optional.of(new UploadSession.Quota(0, 2)));
        assertThrows(ArtifactUploadQuotaExceededException.class, () -> service.start(request(15), OWNER, KEY));
        assertThrows(ArtifactUploadTooLargeException.class, () -> service.start(request(1_001), OWNER, KEY));
        assertThrows(IllegalArgumentException.class, () -> service.start(request(15), OWNER, "short"));
        verify(sessions, never()).insert(any(), any(), any(), any(), any(), any(), anyLong(), anyLong(), anyInt(), any());
    }

    @Test
    void partMustMatchItsDeclaredRangeAndIsCheckpointedOnce() {
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.OPEN, 0, "f")));
        when(sessions.part(ID, 2, true)).thenReturn(Optional.empty());
        when(sessions.markPartReceived(ID, 2, SHA_TWO)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.uploadPart(ID, 2, 10, 19, 15, SHA_TWO, stream(PART_TWO), 5, OWNER));
        assertThrows(IllegalArgumentException.class, () -> service.uploadPart(ID, 3, 20, 24, 15, SHA_TWO, stream(PART_TWO), 5, OWNER));
        verifyNoInteractions(staging);

        var receipt = service.uploadPart(ID, 2, 10, 14, 15, SHA_TWO, stream(PART_TWO), 5, OWNER);

        assertFalse(receipt.replayed());
        verify(sessions).reservePart(ID, 2, SHA_TWO, 5);
        verify(staging).putPart(eq(ID), eq(2), eq(SHA_TWO), any(), eq(5L));
        verify(sessions).recordProgress(any());
    }

    @Test
    void receivedPartIsAReplayAndDifferentBytesForTheSamePartConflict() {
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.OPEN, 10, "f")));
        when(sessions.part(ID, 1, true)).thenReturn(Optional.of(new UploadSession.Part(1, SHA_ONE, 10, true)));

        assertTrue(service.uploadPart(ID, 1, 0, 9, 15, SHA_ONE, stream(PART_ONE), 10, OWNER).replayed());
        assertThrows(ArtifactUploadConflictException.class, () -> service.uploadPart(ID, 1, 0, 9, 15, "e".repeat(64), stream(PART_ONE), 10, OWNER));
        verifyNoInteractions(staging);
    }

    @Test
    void rejectedDigestReleasesTheReservationAndALostSessionRemovesTheStoredPart() {
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.OPEN, 0, "f")));
        when(sessions.part(ID, 1, true)).thenReturn(Optional.empty());
        doThrow(new IllegalArgumentException("digest")).when(staging).putPart(eq(ID), eq(1), eq(SHA_ONE), any(), eq(10L));
        assertThrows(IllegalArgumentException.class, () -> service.uploadPart(ID, 1, 0, 9, 15, SHA_ONE, stream(PART_ONE), 10, OWNER));
        verify(sessions).discardReservedPart(ID, 1, SHA_ONE);

        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.OPEN, 0, "f")),
                Optional.of(session(UploadSessionStatus.CANCELLED, 0, "f")));
        assertThrows(ArtifactUploadConflictException.class, () -> service.uploadPart(ID, 2, 10, 14, 15, SHA_TWO, stream(PART_TWO), 5, OWNER));
        verify(staging).deletePart(ID, 2, SHA_TWO);
    }

    @Test
    void completeAssemblesVerifiedPartsCommitsReleasesQuotaAndCleansCheckpoints() throws Exception {
        UploadSession open = session(UploadSessionStatus.OPEN, 15, "f");
        List<UploadSession.Part> parts = List.of(new UploadSession.Part(1, SHA_ONE, 10, true), new UploadSession.Part(2, SHA_TWO, 5, true));
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(open));
        when(sessions.parts(eq(ID), anyBoolean())).thenReturn(parts);
        when(staging.openPart(ID, 1, SHA_ONE)).thenReturn(stream(PART_ONE));
        when(staging.openPart(ID, 2, SHA_TWO)).thenReturn(stream(PART_TWO));
        when(sessions.transition(any(), any(), any(), any(), any())).thenReturn(true);
        when(sessions.releaseQuota(any())).thenReturn(true);
        when(packages.uploadPackage(eq("PKG"), eq("SITE"), eq(1), eq("ITEM"), any())).thenAnswer(call -> {
            assertEquals("0123456789abcde", new String(((InputStream) call.getArgument(4)).readAllBytes(), StandardCharsets.UTF_8));
            return new ArtifactPackageService.ManifestReceipt(9, true, "p".repeat(64), 2, 2, 2, 0, 0);
        });

        var receipt = service.complete(ID, OWNER);

        assertEquals("COMMITTED", receipt.status());
        assertEquals(9L, receipt.manifestId());
        verify(sessions).markProcessing(ID);
        verify(sessions).transition(open.withStatus(UploadSessionStatus.PROCESSING), UploadSessionStatus.COMMITTED, null, 9L, "p".repeat(64));
        verify(sessions).releaseQuota(any());
        verify(staging).deletePart(ID, 1, SHA_ONE);
        verify(sessions).deleteParts(ID);
    }

    @Test
    void dependencyFailureKeepsPartsForRetryWhileAnInvalidPackageIsRejected() {
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.RETRYABLE, 15, "f")));
        when(sessions.parts(eq(ID), anyBoolean())).thenReturn(List.of(new UploadSession.Part(1, SHA_ONE, 10, true), new UploadSession.Part(2, SHA_TWO, 5, true)));
        when(sessions.transition(any(), any(), any(), any(), any())).thenReturn(true);
        when(sessions.releaseQuota(any())).thenReturn(true);

        when(packages.uploadPackage(any(), any(), anyInt(), any(), any())).thenThrow(new ArtifactStorageException("down", null));
        assertThrows(ArtifactStorageException.class, () -> service.complete(ID, OWNER));
        verify(sessions).transition(any(), eq(UploadSessionStatus.RETRYABLE), eq("DEPENDENCY_UNAVAILABLE"), isNull(), isNull());
        verify(sessions, never()).deleteParts(ID);
        verify(sessions, never()).releaseQuota(any());

        doThrow(new IllegalArgumentException("not a zip")).when(packages).uploadPackage(any(), any(), anyInt(), any(), any());
        assertThrows(IllegalArgumentException.class, () -> service.complete(ID, OWNER));
        verify(sessions).transition(any(), eq(UploadSessionStatus.REJECTED), eq("PACKAGE_INVALID"), isNull(), isNull());
        verify(sessions).releaseQuota(any());
        verify(sessions).deleteParts(ID);
    }

    @Test
    void completionRequiresEveryPartAndIsIdempotentOnceCommitted() {
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.OPEN, 10, "f")));
        when(sessions.parts(ID, true)).thenReturn(List.of(new UploadSession.Part(1, SHA_ONE, 10, true)));
        assertThrows(ArtifactUploadConflictException.class, () -> service.complete(ID, OWNER));

        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.PROCESSING, 15, "f")));
        assertThrows(ArtifactUploadConflictException.class, () -> service.complete(ID, OWNER));

        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.COMMITTED, 15, "f")));
        assertEquals("COMMITTED", service.complete(ID, OWNER).status());
        verifyNoInteractions(packages);
    }

    @Test
    void anotherOwnerSeesNothingAndExpiredSessionsStopAcceptingWork() {
        when(sessions.owned(any(), any(), anyBoolean())).thenReturn(Optional.empty());
        assertThrows(ArtifactNotFoundException.class, () -> service.status(ID, new Identity("x".repeat(64), "y".repeat(64))));

        UploadSession stale = new UploadSession(ID, OWNER.tenantKeyHash(), OWNER.ownerKeyHash(), "f", "PKG", "SITE", 1, "ITEM", 15, 10, 2, 0,
                UploadSessionStatus.OPEN, false, null, null, NOW.minusSeconds(1));
        when(sessions.owned(ID, OWNER, false)).thenReturn(Optional.of(stale));
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(stale));
        when(sessions.transition(any(), any(), any(), any(), any())).thenReturn(true);
        when(sessions.releaseQuota(any())).thenReturn(true);

        assertEquals("EXPIRED", service.status(ID, OWNER).status());
        assertThrows(ArtifactUploadConflictException.class, () -> service.uploadPart(ID, 1, 0, 9, 15, SHA_ONE, stream(PART_ONE), 10, OWNER));
        assertThrows(ArtifactUploadConflictException.class, () -> service.complete(ID, OWNER));
    }

    @Test
    void inconsistentQuotaAccountingIsNeverIgnored() {
        when(sessions.owned(ID, OWNER, true)).thenReturn(Optional.of(session(UploadSessionStatus.OPEN, 0, "f")));
        when(sessions.transition(any(), any(), any(), any(), any())).thenReturn(true);
        when(sessions.releaseQuota(any())).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> service.cancel(ID, OWNER));
    }

    @Test
    void cleanupWaitsForTheSchemaReadyEvent() {
        service.expireAndClean();
        verifyNoInteractions(sessions);

        readiness.onMigrationsCompleted(new PlatformSchemaReadyEvent());
        when(sessions.cleanupCandidates(anyInt())).thenReturn(List.of());
        service.expireAndClean();
        verify(sessions).requeueStaleProcessing(properties.getUploadProcessingLeaseSeconds());
    }

    private static ArtifactUploadSessionService.StartRequest request(long bytes) {
        return new ArtifactUploadSessionService.StartRequest("PKG", "SITE", 1, "ITEM", bytes);
    }

    private static UploadSession session(UploadSessionStatus status, long receivedBytes, String fingerprint) {
        return new UploadSession(ID, OWNER.tenantKeyHash(), OWNER.ownerKeyHash(), fingerprint, "PKG", "SITE", 1, "ITEM", 15, 10, 2, receivedBytes,
                status, false, null, null, NOW.plusSeconds(3_600));
    }

    private static InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        when(provider.getIfAvailable(any())).thenAnswer(call -> value != null ? value : ((Supplier<T>) call.getArgument(0)).get());
        return provider;
    }
}
