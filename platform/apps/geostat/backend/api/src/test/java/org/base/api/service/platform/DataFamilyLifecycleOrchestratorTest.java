package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;

class DataFamilyLifecycleOrchestratorTest {
    @Test void appliesOneGovernedPipelineToEveryConcreteFamily() {
        var orchestrator = new DataFamilyLifecycleOrchestrator();
        for (PageFamily family : new PageFamily[]{PageFamily.ENTITY, PageFamily.STATISTICAL,
                PageFamily.REFERENCE, PageFamily.RAW, PageFamily.GEO, PageFamily.RELATION}) {
            String id = "dataset-" + family.name();
            assertEquals(DataFamilyLifecycle.State.RECEIVED, orchestrator.begin(id, family));
            for (DataFamilyLifecycle.State state : new DataFamilyLifecycle.State[]{
                    DataFamilyLifecycle.State.STAGED, DataFamilyLifecycle.State.VALIDATED,
                    DataFamilyLifecycle.State.MATERIALIZED, DataFamilyLifecycle.State.RECONCILED,
                    DataFamilyLifecycle.State.PUBLISHED}) {
                assertEquals(state, orchestrator.advance(id, family, state));
            }
            assertEquals(DataFamilyLifecycle.State.PUBLISHED, orchestrator.begin(id, family));
        }
        assertEquals(6, orchestrator.activeCount());
    }

    @Test void validatesCommonRelationAndProjectionSurfaceForEveryFamily() {
        var orchestrator = new DataFamilyLifecycleOrchestrator();
        for (PageFamily family : new PageFamily[]{PageFamily.ENTITY, PageFamily.STATISTICAL,
                PageFamily.REFERENCE, PageFamily.RAW, PageFamily.GEO, PageFamily.RELATION}) {
            orchestrator.begin("surface-" + family, family);
            orchestrator.acceptSurface("surface-" + family, family,
                    Map.of("relations", List.of(Map.of("code", "r1")),
                           "projections", List.of(Map.of("field", "value"))));
        }
        assertThrows(IllegalArgumentException.class, () -> orchestrator.acceptSurface(
                "surface-ENTITY", PageFamily.ENTITY, Map.of("relations", "invalid")));
    }

    @Test void rejectsUnknownFamilyAndInvalidTransition() {
        var orchestrator = new DataFamilyLifecycleOrchestrator();
        assertThrows(IllegalArgumentException.class, () -> orchestrator.begin("x", PageFamily.UNKNOWN));
        orchestrator.begin("x", PageFamily.ENTITY);
        assertThrows(IllegalStateException.class, () -> orchestrator.advance("x", PageFamily.ENTITY,
                DataFamilyLifecycle.State.PUBLISHED));
    }

    @Test void restoresAndPersistsStateAcrossOrchestratorInstances() {
        var state = new java.util.concurrent.ConcurrentHashMap<String, DataFamilyLifecycle.State>();
        LifecycleStateStore store = new LifecycleStateStore() {
            public DataFamilyLifecycle.State load(String id) { return state.get(id); }
            public void save(String id, DataFamilyLifecycle.State value) { state.put(id, value); }
        };
        var first = new DataFamilyLifecycleOrchestrator(store);
        first.begin("persisted", PageFamily.ENTITY);
        first.advance("persisted", PageFamily.ENTITY, DataFamilyLifecycle.State.STAGED);
        first.advance("persisted", PageFamily.ENTITY, DataFamilyLifecycle.State.VALIDATED);

        var restarted = new DataFamilyLifecycleOrchestrator(store);
        assertEquals(DataFamilyLifecycle.State.VALIDATED, restarted.begin("persisted", PageFamily.ENTITY));
        assertEquals(DataFamilyLifecycle.State.VALIDATED, restarted.begin("persisted", PageFamily.ENTITY));
    }

    @Test void jdbcStorePersistsLifecycleStateIdempotently() {
        var ds = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                "jdbc:h2:mem:lifecycle_store;MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(ds);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS platform");
        jdbc.execute("CREATE TABLE platform.data_family_lifecycle (lifecycle_id VARCHAR(256) PRIMARY KEY, state_code VARCHAR(32) NOT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        var store = new JdbcLifecycleStateStore(jdbc);
        store.save("ENTITY:one", DataFamilyLifecycle.State.RECEIVED);
        store.save("ENTITY:one", DataFamilyLifecycle.State.VALIDATED);
        assertEquals(DataFamilyLifecycle.State.VALIDATED, store.load("ENTITY:one"));
    }
}
