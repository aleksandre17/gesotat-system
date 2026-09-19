package org.base.api.service.platform;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Contract-revision lifecycle independent of product, provider and storage.
 * Invalid transitions fail closed; repeating the current state is idempotent.
 */
public final class ContractLifecycle {
    public enum State { DRAFT, REVIEW_REQUIRED, APPROVED, SUPERSEDED, ROLLED_BACK }

    private static final Map<State, Set<State>> TRANSITIONS = Map.of(
            State.DRAFT, Set.of(State.REVIEW_REQUIRED),
            State.REVIEW_REQUIRED, Set.of(State.APPROVED, State.DRAFT),
            State.APPROVED, Set.of(State.SUPERSEDED, State.ROLLED_BACK),
            State.SUPERSEDED, Set.of(),
            State.ROLLED_BACK, Set.of());

    private final String revisionKey;
    private State state = State.DRAFT;

    public ContractLifecycle(String revisionKey) {
        this(revisionKey, State.DRAFT);
    }

    ContractLifecycle(String revisionKey, State initialState) {
        if (revisionKey == null || revisionKey.isBlank() || revisionKey.length() > 256) {
            throw new IllegalArgumentException("revision key is invalid");
        }
        this.revisionKey = revisionKey;
        this.state = Objects.requireNonNull(initialState, "initial state");
    }

    /** Whether a persisted status may move to {@code next}; an unknown status permits nothing. */
    public static boolean permits(String persistedStatus, State next) {
        Objects.requireNonNull(next, "next state");
        try {
            return TRANSITIONS.getOrDefault(State.valueOf(persistedStatus), Set.of()).contains(next);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            return false;
        }
    }

    public String revisionKey() { return revisionKey; }
    public synchronized State state() { return state; }

    public synchronized State advance(State next) {
        Objects.requireNonNull(next, "next state");
        if (next == state) return state;
        if (!TRANSITIONS.getOrDefault(state, Set.of()).contains(next)) {
            throw new IllegalStateException("Invalid contract transition: " + state + " -> " + next);
        }
        state = next;
        return state;
    }
}
