package org.base.api.service.artifact;

import org.base.api.security.tenancy.TenantAccessGuards;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArtifactDistributionServiceTest {
    @SuppressWarnings("unchecked")
    @Test
    void metadataListsPolicyModeAndSignedUrlLifetimeFromApprovedContract() throws Exception {
        var attachments = mock(ArtifactAttachmentRepository.class);
        var contracts = mock(ArtifactContractResolver.class);
        var store = mock(ObjectProvider.class);
        var metricsProvider = mock(ObjectProvider.class);
        var clockProvider = mock(ObjectProvider.class);
        when(clockProvider.getIfAvailable(any())).thenReturn(Clock.fixed(java.time.Instant.parse("2026-09-18T00:00:00Z"), java.time.ZoneOffset.UTC));
        var entity = new ArtifactAttachmentRepository.PublishedEntity(12, 34, 56);
        when(attachments.newestPublished("KIDS_RESOURCE", "resource|128")).thenReturn(java.util.Optional.of(entity));
        when(contracts.approved(56)).thenReturn(List.of(ArtifactMatcherTest.definition(
                "{\"type\":\"SOURCE_PATH\",\"bindings\":[{\"language\":\"ka\",\"field\":\"path_ka\"}]}")));
        when(attachments.attachedObjects(entity)).thenReturn(List.of(new ArtifactAttachmentRepository.AttachedObject(
                "resource|128", "PRIMARY_FILE", ArtifactRole.PRIMARY, "ka", 1, "report.xlsx", ArtifactMatcherTest.XLSX,
                42, "a".repeat(64), "geostat-ingest", "artifacts/sha256/a.xlsx", VerificationStatus.VERIFIED)));

        ArtifactDistributionService service = new ArtifactDistributionService(attachments, contracts, store,
                new ArtifactMetrics(metricsProvider), TenantAccessGuards.permitAll(), clockProvider);
        var response = service.list("KIDS_RESOURCE", "resource|128");

        assertEquals("PUBLIC_WHEN_PUBLISHED", response.artifacts().get(0).download().mode());
        assertEquals(300, response.artifacts().get(0).download().expiresInSeconds());
    }

    @SuppressWarnings("unchecked")
    @Test
    void metadataFailsClosedForAttachmentWithoutApprovedRelation() throws Exception {
        var attachments = mock(ArtifactAttachmentRepository.class);
        var contracts = mock(ArtifactContractResolver.class);
        var store = mock(ObjectProvider.class);
        var clockProvider = mock(ObjectProvider.class);
        when(clockProvider.getIfAvailable(any())).thenReturn(Clock.systemUTC());
        var entity = new ArtifactAttachmentRepository.PublishedEntity(12, 34, 56);
        when(attachments.newestPublished("KIDS_RESOURCE", "resource|128")).thenReturn(java.util.Optional.of(entity));
        when(contracts.approved(56)).thenReturn(List.of());
        when(attachments.attachedObjects(entity)).thenReturn(List.of(new ArtifactAttachmentRepository.AttachedObject(
                "resource|128", "UNDECLARED", ArtifactRole.PRIMARY, "ka", 1, "report.xlsx", ArtifactMatcherTest.XLSX,
                42, "a".repeat(64), "geostat-ingest", "artifacts/sha256/a.xlsx", VerificationStatus.VERIFIED)));

        ArtifactDistributionService service = new ArtifactDistributionService(attachments, contracts, store,
                new ArtifactMetrics(mock(ObjectProvider.class)), TenantAccessGuards.permitAll(), clockProvider);

        assertThrows(IllegalStateException.class, () -> service.list("KIDS_RESOURCE", "resource|128"));
    }
}
