package org.base.api.service.platform;

import java.util.*;

/** Provider/family-neutral lifecycle state machine for contract-governed data products. */
public final class DataFamilyLifecycle {
    public enum State { RECEIVED, STAGED, VALIDATED, MATERIALIZED, RECONCILED, PUBLISHED, ROLLED_BACK, QUARANTINED }
    private static final Map<State, Set<State>> TRANSITIONS = Map.of(
            State.RECEIVED, Set.of(State.STAGED, State.QUARANTINED),
            State.STAGED, Set.of(State.VALIDATED, State.QUARANTINED),
            State.VALIDATED, Set.of(State.MATERIALIZED, State.QUARANTINED),
            State.MATERIALIZED, Set.of(State.RECONCILED, State.QUARANTINED),
            State.RECONCILED, Set.of(State.PUBLISHED, State.QUARANTINED),
            State.PUBLISHED, Set.of(State.ROLLED_BACK),
            State.ROLLED_BACK, Set.of(), State.QUARANTINED, Set.of());
    private final String id;
    private State state;
    public DataFamilyLifecycle(String id) { if (id == null || id.isBlank() || id.length() > 256) throw new IllegalArgumentException("lifecycle id is invalid"); this.id=id; this.state=State.RECEIVED; }
    DataFamilyLifecycle(String id, State initial) { this(id); this.state=Objects.requireNonNull(initial, "initial state"); }
    public String id() { return id; }
    public State state() { return state; }
    /** Idempotent repeat of the current state is accepted; all other invalid moves fail closed. */
    public synchronized State advance(State next) {
        Objects.requireNonNull(next, "next state");
        if (next == state) return state;
        if (!TRANSITIONS.getOrDefault(state, Set.of()).contains(next)) throw new IllegalStateException("Invalid lifecycle transition: " + state + " -> " + next);
        state = next; return state;
    }
}
