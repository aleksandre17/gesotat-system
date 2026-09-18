package org.base.api.service.publication.gate;

import java.util.Set;

/**
 * One publication gate. Implementations are pure functions of measured facts and are discovered as
 * Spring beans; adding a gate changes neither the evaluation service nor publication.
 */
public interface ReleaseGate {

    /** Control Plane facts a gate may need beyond the snapshot itself. */
    record Context(Set<Long> activeClassificationItemIds) {
        public Context {
            activeClassificationItemIds = Set.copyOf(activeClassificationItemIds);
        }
    }

    String code();

    GateEvaluation evaluate(SnapshotFacts facts, Context context);
}
