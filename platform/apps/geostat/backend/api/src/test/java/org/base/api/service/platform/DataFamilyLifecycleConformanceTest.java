package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Conformance evidence for the provider-neutral lifecycle contract.
 * The same state sequence must govern every canonical data family; family
 * adapters may differ in materialization, but not in governance semantics.
 */
class DataFamilyLifecycleConformanceTest {
    private static final List<String> FAMILIES = List.of("ENTITY", "STATISTICAL", "REFERENCE", "RAW", "GEO", "RELATION");

    @Test
    void everyCanonicalFamilyUsesTheSameGovernedLifecycle() {
        for (String family : FAMILIES) {
            var lifecycle = new DataFamilyLifecycle("conformance:" + family);
            lifecycle.advance(DataFamilyLifecycle.State.STAGED);
            lifecycle.advance(DataFamilyLifecycle.State.VALIDATED);
            lifecycle.advance(DataFamilyLifecycle.State.MATERIALIZED);
            lifecycle.advance(DataFamilyLifecycle.State.RECONCILED);
            lifecycle.advance(DataFamilyLifecycle.State.PUBLISHED);
            assertEquals(DataFamilyLifecycle.State.PUBLISHED, lifecycle.state(), family);
        }
    }

    @Test
    void everyCanonicalFamilySupportsTheSameQuarantineBoundary() {
        for (String family : FAMILIES) {
            var lifecycle = new DataFamilyLifecycle("quarantine:" + family);
            lifecycle.advance(DataFamilyLifecycle.State.STAGED);
            lifecycle.advance(DataFamilyLifecycle.State.QUARANTINED);
            assertEquals(DataFamilyLifecycle.State.QUARANTINED, lifecycle.state(), family);
        }
    }
}
