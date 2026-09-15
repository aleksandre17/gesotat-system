package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.DefaultApplicationArguments;

class ProviderCapabilityDiscoveryServiceTest {
    @Test void loadsOnlyActiveRowsAndGroupsProviderCapabilities() {
        JdbcTemplate db = Mockito.mock(JdbcTemplate.class);
        Mockito.when(db.queryForList(Mockito.anyString())).thenReturn(List.of(
                Map.of("provider_code", "sqlserver", "family_code", "ENTITY", "operation_code", "QUERY", "feature_code", "KEYSET", "max_page_size", 1000, "transactional_supported", true),
                Map.of("provider_code", "sqlserver", "family_code", "STATISTICAL", "operation_code", "QUERY", "feature_code", "AGGREGATION", "max_page_size", 500, "transactional_supported", true)));
        var registry = new ProviderCapabilityRegistry();
        assertEquals(1, new ProviderCapabilityDiscoveryService(db).loadActive(registry));
        var capability = registry.require("sqlserver", "STATISTICAL", "QUERY");
        assertEquals(500, capability.maxPageSize());
        assertTrue(capability.features().contains("KEYSET"));
    }

    @Test void startupRunnerFailsClosedWhenDiscoveryReturnsNoActiveProvider() {
        var discovery = Mockito.mock(ProviderCapabilityDiscoveryService.class);
        var registry = new ProviderCapabilityRegistry();
        Mockito.when(discovery.loadActive(registry)).thenReturn(0);
        var runner = new ProviderCapabilityDiscoveryRunner(discovery, registry);
        assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
    }

    @Test void reloadReplacesActiveSetAndRetiresStaleProvider() {
        JdbcTemplate db = Mockito.mock(JdbcTemplate.class);
        Mockito.when(db.queryForList(Mockito.anyString())).thenReturn(
                List.of(Map.of("provider_code", "old", "family_code", "ENTITY", "operation_code", "QUERY", "feature_code", "KEYSET", "max_page_size", 100, "transactional_supported", true)),
                List.of(Map.of("provider_code", "new", "family_code", "ENTITY", "operation_code", "QUERY", "feature_code", "KEYSET", "max_page_size", 100, "transactional_supported", true)));
        var registry = new ProviderCapabilityRegistry();
        var discovery = new ProviderCapabilityDiscoveryService(db);
        discovery.loadActive(registry);
        assertTrue(registry.find("old").isPresent());
        discovery.loadActive(registry);
        assertTrue(registry.find("new").isPresent());
        assertTrue(registry.find("old").isEmpty());
    }
}
