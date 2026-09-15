package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyFallbackPolicyTest {
    @Test void approvedContractAlwaysWins() {
        var decision = LegacyFallbackPolicy.evaluate(true, true);
        assertFalse(decision.allowed());
        assertEquals("APPROVED_CONTRACT", decision.reason());
    }

    @Test void fallbackRequiresExplicitFlag() {
        assertTrue(LegacyFallbackPolicy.evaluate(true, false).allowed());
        assertFalse(LegacyFallbackPolicy.evaluate(false, false).allowed());
        assertEquals("LEGACY_FALLBACK_DISABLED", LegacyFallbackPolicy.evaluate(false, false).reason());
    }
}
