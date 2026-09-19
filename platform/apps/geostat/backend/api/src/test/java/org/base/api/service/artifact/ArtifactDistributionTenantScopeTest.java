package org.base.api.service.artifact;

import org.base.api.security.tenancy.Caller;
import org.base.api.security.tenancy.ProductTenancy;
import org.base.api.security.tenancy.TenantAccessGuards;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Object-level authorization at the distribution boundary (OWASP API1). A published entity of another
 * tenant must be indistinguishable from an entity that does not exist.
 */
class ArtifactDistributionTenantScopeTest {
    private static final String RECORD_TYPE = "RESOURCE";
    private static final String KEY = "resource|128";
    private static final ProductTenancy OWNED = new ProductTenancy(1, "PRODUCT_A", "tenant-a");

    private final ArtifactAttachmentRepository attachments = mock(ArtifactAttachmentRepository.class);
    private final ArtifactContractResolver contracts = mock(ArtifactContractResolver.class);

    @SuppressWarnings("unchecked")
    private ArtifactDistributionService service(String callerTenant) {
        ObjectProvider<Clock> clockProvider = mock(ObjectProvider.class);
        when(clockProvider.getIfAvailable(any())).thenReturn(Clock.systemUTC());
        return new ArtifactDistributionService(attachments, contracts, mock(ObjectProvider.class),
                new ArtifactMetrics(mock(ObjectProvider.class)),
                TenantAccessGuards.enforcing(new Caller("sub", callerTenant, Set.of("READ_RESOURCE"), Caller.Kind.OIDC), OWNED),
                clockProvider);
    }

    @Test
    void anEntityOfAnotherTenantAnswersExactlyLikeAnAbsentEntity() {
        when(attachments.newestPublished(RECORD_TYPE, KEY))
                .thenReturn(Optional.of(new ArtifactAttachmentRepository.PublishedEntity(12, 34, 56)));
        ArtifactNotFoundException foreign = assertThrows(ArtifactNotFoundException.class, () -> service("tenant-b").list(RECORD_TYPE, KEY));

        when(attachments.newestPublished(RECORD_TYPE, KEY)).thenReturn(Optional.empty());
        ArtifactNotFoundException absent = assertThrows(ArtifactNotFoundException.class, () -> service("tenant-a").list(RECORD_TYPE, KEY));

        assertEquals(absent.getClass(), foreign.getClass());
        assertEquals(absent.getMessage(), foreign.getMessage());
    }

    @Test
    void aDeniedListNeverReadsTheContractOrTheAttachedObjects() {
        when(attachments.newestPublished(RECORD_TYPE, KEY))
                .thenReturn(Optional.of(new ArtifactAttachmentRepository.PublishedEntity(12, 34, 56)));
        assertThrows(ArtifactNotFoundException.class, () -> service("tenant-b").list(RECORD_TYPE, KEY));
        verify(contracts, never()).approved(56);
        verify(attachments, never()).attachedObjects(any());
    }

    @Test
    void aDeniedDownloadNeverReachesThePolicyOrTheObjectStore() {
        when(attachments.newestPublished(RECORD_TYPE, KEY))
                .thenReturn(Optional.of(new ArtifactAttachmentRepository.PublishedEntity(12, 34, 56)));
        ArtifactNotFoundException denial = assertThrows(ArtifactNotFoundException.class,
                () -> service("tenant-b").download(RECORD_TYPE, KEY, "PRIMARY_FILE", "ka", 1, Set.of("READ_RESOURCE")));
        assertNotNull(denial.getMessage());
        verify(contracts, never()).approved(56, "PRIMARY_FILE");
    }
}
