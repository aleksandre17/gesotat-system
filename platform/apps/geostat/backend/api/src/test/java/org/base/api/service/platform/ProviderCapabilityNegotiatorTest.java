package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ProviderCapabilityNegotiatorTest {
    private ProviderCapabilityRegistry registry() {
        var r = new ProviderCapabilityRegistry();
        r.register(new ProviderCapabilityRegistry.Capability("ALT_DB", Set.of("ENTITY"), Set.of("QUERY"), Set.of("KEYSET", "RELATIONS"), 500, true));
        return r;
    }
    @Test void acceptsDeclaredFamilyOperationFeaturesAndLimit() {
        assertEquals("ALT_DB", ProviderCapabilityNegotiator.require(registry(), "ALT_DB", "ENTITY", "QUERY", Set.of("KEYSET"), 100).providerCode());
    }
    @Test void rejectsMissingFeatureAndExcessivePage() {
        assertThrows(IllegalArgumentException.class, () -> ProviderCapabilityNegotiator.require(registry(), "ALT_DB", "ENTITY", "QUERY", Set.of("EXPORT"), 100));
        assertThrows(IllegalArgumentException.class, () -> ProviderCapabilityNegotiator.require(registry(), "ALT_DB", "ENTITY", "QUERY", Set.of(), 501));
    }
}
