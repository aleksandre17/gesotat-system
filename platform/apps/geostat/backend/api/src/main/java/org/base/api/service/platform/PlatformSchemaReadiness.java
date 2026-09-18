package org.base.api.service.platform;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Shared runtime gate for every scheduled worker that depends on platform schema. */
@Component
public class PlatformSchemaReadiness {
    private volatile boolean ready;

    @EventListener(PlatformSchemaReadyEvent.class)
    public void onMigrationsCompleted(PlatformSchemaReadyEvent event) {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }
}
