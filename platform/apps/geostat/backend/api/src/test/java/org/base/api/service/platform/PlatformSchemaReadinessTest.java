package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformSchemaReadinessTest {
    @Test
    void scheduledWorkersRemainClosedUntilMigrationCompletionEvent() {
        PlatformSchemaReadiness readiness = new PlatformSchemaReadiness();

        assertFalse(readiness.isReady());
        readiness.onMigrationsCompleted(new PlatformSchemaReadyEvent());
        assertTrue(readiness.isReady());
    }
}
