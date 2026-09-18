package org.base.api.service.artifact;

import org.base.api.service.platform.PlatformSchemaReadiness;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtifactUploadSessionServiceReadinessTest {
    @Test
    void cleanupWaitsForExplicitSchemaReadyEvent() {
        JdbcTemplate data = mock(JdbcTemplate.class);
        PlatformSchemaReadiness readiness = new PlatformSchemaReadiness();
        ArtifactUploadSessionService service = service(data, readiness);

        service.expireAndClean();

        verifyNoInteractions(data);
    }

    @Test
    void cleanupRunsOnlyAfterSchemaReadyEventIsPublished() {
        JdbcTemplate data = mock(JdbcTemplate.class);
        PlatformSchemaReadiness readiness = new PlatformSchemaReadiness();
        ArtifactUploadSessionService service = service(data, readiness);
        when(data.query(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(java.util.List.of());

        service.expireAndClean();
        verifyNoInteractions(data);

        readiness.onMigrationsCompleted(new org.base.api.service.platform.PlatformSchemaReadyEvent());
        service.expireAndClean();

        verify(data).update(org.mockito.ArgumentMatchers.contains("PROCESSING_LEASE_EXPIRED"), org.mockito.ArgumentMatchers.anyInt());
    }

    private static ArtifactUploadSessionService service(JdbcTemplate data) {
        return service(data, new PlatformSchemaReadiness());
    }

    private static ArtifactUploadSessionService service(JdbcTemplate data, PlatformSchemaReadiness readiness) {
        return new ArtifactUploadSessionService(data, mock(PlatformTransactionManager.class),
                mock(ObjectProvider.class), mock(ArtifactPackageContractResolver.class),
                mock(ArtifactPackageService.class), new ArtifactProperties(), readiness);
    }
}
