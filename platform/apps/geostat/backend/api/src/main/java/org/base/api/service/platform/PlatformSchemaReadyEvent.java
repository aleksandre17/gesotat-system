package org.base.api.service.platform;

import java.time.Instant;

/** Published only after every configured platform migration has been applied and recorded. */
public record PlatformSchemaReadyEvent(Instant completedAt) {
    public PlatformSchemaReadyEvent() {
        this(Instant.now());
    }
}
