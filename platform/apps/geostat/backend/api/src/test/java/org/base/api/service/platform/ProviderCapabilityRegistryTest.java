package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ProviderCapabilityRegistryTest {
    private static ProviderCapabilityRegistry.Capability sql() {
        return new ProviderCapabilityRegistry.Capability("sqlserver", Set.of("ENTITY", "STATISTICAL"), Set.of("QUERY", "PUBLISH"), Set.of("KEYSET", "TRANSACTION"), 1000, true);
    }

    @Test void registersAndNegotiatesApprovedCapability() {
        var registry = new ProviderCapabilityRegistry(); registry.register(sql());
        assertEquals("sqlserver", registry.require("sqlserver", "ENTITY", "QUERY").providerCode());
        assertThrows(IllegalArgumentException.class, () -> registry.require("sqlserver", "GEO", "QUERY"));
    }

    @Test void duplicateAndRetiredProvidersFailClosed() {
        var registry = new ProviderCapabilityRegistry(); registry.register(sql());
        assertThrows(IllegalStateException.class, () -> registry.register(sql()));
        registry.retire("sqlserver");
        assertTrue(registry.find("sqlserver").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> registry.require("sqlserver", "ENTITY", "QUERY"));
    }

    @Test void rejectsUnsafeMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new ProviderCapabilityRegistry.Capability("sql;drop", Set.of("ENTITY"), Set.of("QUERY"), Set.of("KEYSET"), 10, false));
        assertThrows(IllegalArgumentException.class, () -> new ProviderCapabilityRegistry.Capability("sqlserver", Set.of("ENTITY"), Set.of("QUERY"), Set.of("KEYSET"), 0, false));
    }
}
