package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProviderLifecycleTest {
    @Test void followsRegistrationBindingHealthPath() {
        var lifecycle = new ProviderLifecycle("ALT_DB");
        lifecycle.advance(ProviderLifecycle.State.VALIDATED);
        lifecycle.advance(ProviderLifecycle.State.BOUND);
        assertEquals(ProviderLifecycle.State.HEALTHY, lifecycle.advance(ProviderLifecycle.State.HEALTHY));
        lifecycle.advance(ProviderLifecycle.State.FAILED);
        lifecycle.advance(ProviderLifecycle.State.BOUND);
        assertEquals(ProviderLifecycle.State.HEALTHY, lifecycle.advance(ProviderLifecycle.State.HEALTHY));
    }
    @Test void rejectsSkippingValidationAndMutationAfterRetirement() {
        var lifecycle = new ProviderLifecycle("ALT_DB");
        assertThrows(IllegalStateException.class, () -> lifecycle.advance(ProviderLifecycle.State.HEALTHY));
        lifecycle.advance(ProviderLifecycle.State.RETIRED);
        assertThrows(IllegalStateException.class, () -> lifecycle.advance(ProviderLifecycle.State.REGISTERED));
    }
}
