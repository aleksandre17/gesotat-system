package org.base.api.service.platform;

import java.util.Objects;

/** Restartsafe, idempotent contract revision state coordinator. */
public final class ContractLifecycleOrchestrator {
    private final ContractLifecycleStateStore store;
    public ContractLifecycleOrchestrator(ContractLifecycleStateStore store) { this.store = Objects.requireNonNull(store, "store"); }

    public ContractLifecycle.State advance(String revisionKey, ContractLifecycle.State next) {
        ContractLifecycle.State persisted = store.load(revisionKey);
        ContractLifecycle lifecycle = new ContractLifecycle(revisionKey, persisted == null ? ContractLifecycle.State.DRAFT : persisted);
        ContractLifecycle.State state = lifecycle.advance(next);
        store.save(revisionKey, state);
        return state;
    }
    public ContractLifecycle.State state(String revisionKey) {
        ContractLifecycle.State state = store.load(revisionKey);
        return state == null ? ContractLifecycle.State.DRAFT : state;
    }
}
