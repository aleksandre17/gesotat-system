package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

class ContractLifecycleOrchestratorTest {
    @Test void persistsAndRestoresApproveSupersedeAndRollbackTransitions() {
        Map<String, ContractLifecycle.State> state = new ConcurrentHashMap<>();
        ContractLifecycleStateStore store = new ContractLifecycleStateStore() {
            public ContractLifecycle.State load(String key) { return state.get(key); }
            public void save(String key, ContractLifecycle.State value) { state.put(key, value); }
        };
        var first = new ContractLifecycleOrchestrator(store);
        first.advance("SITE:1", ContractLifecycle.State.REVIEW_REQUIRED);
        first.advance("SITE:1", ContractLifecycle.State.APPROVED);
        assertEquals(ContractLifecycle.State.APPROVED, new ContractLifecycleOrchestrator(store).state("SITE:1"));
        first.advance("SITE:1", ContractLifecycle.State.SUPERSEDED);
        assertEquals(ContractLifecycle.State.REVIEW_REQUIRED, first.advance("SITE:2", ContractLifecycle.State.REVIEW_REQUIRED));
        assertThrows(IllegalStateException.class, () -> first.advance("SITE:1", ContractLifecycle.State.ROLLED_BACK));
    }

    @Test void jdbcStorePersistsRevisionStateIdempotently() {
        var ds = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:contract_lifecycle_store;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(ds);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS platform");
        jdbc.execute("CREATE TABLE platform.contract_revision_lifecycle (revision_key VARCHAR(256) PRIMARY KEY, state_code VARCHAR(32) NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        var store = new JdbcContractLifecycleStateStore(jdbc);
        store.save("SITE:8", ContractLifecycle.State.REVIEW_REQUIRED);
        store.save("SITE:8", ContractLifecycle.State.APPROVED);
        assertEquals(ContractLifecycle.State.APPROVED, store.load("SITE:8"));
    }
}
