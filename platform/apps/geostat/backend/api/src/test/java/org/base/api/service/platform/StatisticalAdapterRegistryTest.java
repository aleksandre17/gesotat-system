package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.jdbc.core.JdbcTemplate;

class StatisticalAdapterRegistryTest {
    @Test void registersAndResolvesByProviderWithoutStorageKnowledge() {
        var registry = new StatisticalAdapterRegistry();
        StatisticalQueryAdapter adapter = (data, product, metric) -> List.of();
        registry.register("PROVIDER_A", adapter);
        assertSame(adapter, registry.require("PROVIDER_A"));
        assertEquals(1, registry.size());
        assertThrows(IllegalStateException.class, () -> registry.register("PROVIDER_A", adapter));
        assertThrows(IllegalArgumentException.class, () -> registry.require("UNKNOWN"));
    }

    @Test void alternateRelationalAdapterUsesOnlyDeclaredPhysicalIdentifiers() {
        var registry = new StatisticalAdapterRegistry();
        var adapter = new RelationalStatisticalQueryAdapter("alt.snapshots", "alt.members", "alt.series", "alt.obs");
        registry.register("alternate", adapter);
        assertSame(adapter, registry.require("alternate"));
        assertThrows(IllegalArgumentException.class, () ->
                new RelationalStatisticalQueryAdapter("alt;drop", "m", "s", "o"));
    }
}
