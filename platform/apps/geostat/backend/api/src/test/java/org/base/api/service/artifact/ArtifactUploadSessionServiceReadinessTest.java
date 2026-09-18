package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ArtifactUploadSessionServiceReadinessTest {
    @Test
    void cleanupWaitsUntilApplicationRunnersHaveCompletedMigrations() {
        JdbcTemplate data = mock(JdbcTemplate.class);
        ArtifactUploadSessionService service = service(data, true);

        service.expireAndClean();

        verifyNoInteractions(data);
    }

    @Test
    void cleanupStaysDisabledWhenSchemaMigrationsAreDisabled() {
        JdbcTemplate data = mock(JdbcTemplate.class);
        ArtifactUploadSessionService service = service(data, false);

        service.onApplicationReady();
        service.expireAndClean();

        verifyNoInteractions(data);
    }

    private static ArtifactUploadSessionService service(JdbcTemplate data, boolean migrationsEnabled) {
        return new ArtifactUploadSessionService(data, mock(PlatformTransactionManager.class),
                mock(ObjectProvider.class), mock(ArtifactPackageContractResolver.class),
                mock(ArtifactPackageService.class), new ArtifactProperties(), migrationsEnabled);
    }
}
