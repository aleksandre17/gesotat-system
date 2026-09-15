package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;

class PlatformServingCacheServiceTest {
    @Test
    void disabledSchedulerDoesNotTouchDataPlane() {
        JdbcTemplate data = mock(JdbcTemplate.class);
        new PlatformServingCacheService(data, false).rebuildPublished();
        verifyNoInteractions(data);
    }
}
