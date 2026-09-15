package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataFamilyLifecycleTest {
    @Test void followsCanonicalLifecycleAndIsIdempotent() {
        var lifecycle = new DataFamilyLifecycle("site-x:dataset-y:batch-1");
        for (var state : DataFamilyLifecycle.State.values()) {
            if (state == DataFamilyLifecycle.State.RECEIVED || state == DataFamilyLifecycle.State.ROLLED_BACK || state == DataFamilyLifecycle.State.QUARANTINED) continue;
            if (state == DataFamilyLifecycle.State.STAGED) lifecycle.advance(state);
            else if (state == DataFamilyLifecycle.State.VALIDATED) lifecycle.advance(state);
            else if (state == DataFamilyLifecycle.State.MATERIALIZED) lifecycle.advance(state);
            else if (state == DataFamilyLifecycle.State.RECONCILED) lifecycle.advance(state);
            else if (state == DataFamilyLifecycle.State.PUBLISHED) lifecycle.advance(state);
        }
        assertEquals(DataFamilyLifecycle.State.PUBLISHED, lifecycle.state());
        assertEquals(DataFamilyLifecycle.State.PUBLISHED, lifecycle.advance(DataFamilyLifecycle.State.PUBLISHED));
    }

    @Test void invalidSkipAndPostPublishMutationFailClosed() {
        var lifecycle = new DataFamilyLifecycle("x");
        assertThrows(IllegalStateException.class, () -> lifecycle.advance(DataFamilyLifecycle.State.PUBLISHED));
        lifecycle.advance(DataFamilyLifecycle.State.STAGED);
        assertThrows(IllegalStateException.class, () -> lifecycle.advance(DataFamilyLifecycle.State.MATERIALIZED));
        lifecycle.advance(DataFamilyLifecycle.State.VALIDATED); lifecycle.advance(DataFamilyLifecycle.State.MATERIALIZED); lifecycle.advance(DataFamilyLifecycle.State.RECONCILED); lifecycle.advance(DataFamilyLifecycle.State.PUBLISHED); lifecycle.advance(DataFamilyLifecycle.State.ROLLED_BACK);
        assertThrows(IllegalStateException.class, () -> lifecycle.advance(DataFamilyLifecycle.State.STAGED));
    }
}
