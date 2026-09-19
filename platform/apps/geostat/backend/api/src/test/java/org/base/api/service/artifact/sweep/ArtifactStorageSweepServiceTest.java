package org.base.api.service.artifact.sweep;

import org.base.api.service.artifact.ArtifactMetrics;
import org.base.api.service.artifact.ArtifactObjectInventory;
import org.base.api.service.artifact.ArtifactObjectInventory.ListedObject;
import org.base.api.service.artifact.ArtifactObjectStore;
import org.base.api.service.artifact.ArtifactProperties;
import org.base.api.service.artifact.ArtifactStorageException;
import org.base.api.service.artifact.sweep.ArtifactStorageSweepRepository.Orphan;
import org.base.api.service.artifact.sweep.ArtifactStorageSweepRepository.PageResult;
import org.base.api.service.artifact.sweep.ArtifactStorageSweepRepository.Scope;
import org.base.api.service.platform.PlatformJobLeaseService;
import org.base.api.service.platform.PlatformSchemaReadiness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtifactStorageSweepServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    private static final Instant OLD = NOW.minusSeconds(3 * 86_400);
    private static final Scope SCOPE = new Scope(1, "bucket-a", "pool/", null);

    private final ArtifactStorageSweepRepository sweeps = mock(ArtifactStorageSweepRepository.class);
    private final ArtifactObjectInventory inventory = mock(ArtifactObjectInventory.class);
    private final ArtifactObjectStore store = mock(ArtifactObjectStore.class);
    private final PlatformJobLeaseService lease = mock(PlatformJobLeaseService.class);
    private final PlatformSchemaReadiness readiness = mock(PlatformSchemaReadiness.class);
    private final ArtifactStorageSweepProperties properties = new ArtifactStorageSweepProperties();
    private ArtifactStorageSweepService service;

    @BeforeEach
    void setUp() {
        properties.setBatchSize(3);
        ArtifactProperties artifacts = new ArtifactProperties();
        artifacts.setUploadPrefix("pool/");
        when(store.ingestBucket()).thenReturn("bucket-a");
        when(readiness.isReady()).thenReturn(true);
        when(lease.acquire(anyString(), anyInt())).thenReturn(true);
        when(sweeps.nextScope()).thenReturn(Optional.of(SCOPE));
        service = new ArtifactStorageSweepService(sweeps, provider(inventory), provider(store), properties, artifacts,
                new ArtifactMetrics(provider(null)), lease, readiness, provider(Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void unregisteredSettledObjectIsAnOrphanWhileRegisteredAndInFlightObjectsAreNot() {
        when(inventory.page("bucket-a", "pool/", null, 3)).thenReturn(List.of(
                new ListedObject("pool/a", 10, OLD), new ListedObject("pool/b", 20, OLD), new ListedObject("pool/c", 30, NOW.minusSeconds(60))));
        when(sweeps.registeredKeys(eq("bucket-a"), any())).thenReturn(Set.of("pool/a"));

        service.sweepNextPage();

        PageResult page = recorded();
        assertEquals(List.of(new Orphan("pool/b", 20)), page.orphans());
        assertEquals("pool/c", page.lastKey());
        assertEquals(3, page.objectsListed());
        assertFalse(page.finished(), "a full page means the listing may continue");
        verify(sweeps).syncScopes("bucket-a", "pool/");
        verify(lease).release("ARTIFACT_STORAGE_SWEEP");
    }

    @Test
    void shortOrEmptyPageCompletesTheCycle() {
        when(inventory.page("bucket-a", "pool/", null, 3)).thenReturn(List.of(new ListedObject("pool/a", 10, OLD)));
        when(sweeps.registeredKeys(eq("bucket-a"), any())).thenReturn(Set.of("pool/a"));
        service.sweepNextPage();
        assertTrue(recorded().finished());

        when(inventory.page("bucket-a", "pool/", null, 3)).thenReturn(List.of());
        PageResult empty = service.classify(SCOPE, List.of());
        assertTrue(empty.finished());
        assertNull(empty.lastKey());
    }

    @Test
    void storageFailureLeavesTheCursorUntouchedAndReleasesTheLease() {
        when(inventory.page(anyString(), anyString(), any(), anyInt())).thenThrow(new ArtifactStorageException("down", null));

        service.sweepNextPage();

        verify(sweeps, never()).recordPage(any(), any());
        verify(lease).release("ARTIFACT_STORAGE_SWEEP");
    }

    @Test
    void doesNothingWhenDisabledNotReadyOrNotLeaseHolder() {
        when(lease.acquire(anyString(), anyInt())).thenReturn(false);
        service.sweepNextPage();
        when(readiness.isReady()).thenReturn(false);
        service.sweepNextPage();
        properties.setEnabled(false);
        service.sweepNextPage();

        verifyNoInteractions(sweeps, inventory);
        verify(lease, never()).release(anyString());
    }

    @Test
    void pageSizeMustFitOneRegistryStatement() {
        properties.setBatchSize(ArtifactStorageSweepProperties.MAX_BATCH_SIZE + 1);
        assertThrows(IllegalStateException.class, properties::validate);
    }

    private PageResult recorded() {
        ArgumentCaptor<PageResult> page = ArgumentCaptor.forClass(PageResult.class);
        verify(sweeps).recordPage(eq(SCOPE), page.capture());
        return page.getValue();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        when(provider.getIfAvailable(any())).thenAnswer(call -> value != null ? value : ((java.util.function.Supplier<T>) call.getArgument(0)).get());
        return provider;
    }
}
