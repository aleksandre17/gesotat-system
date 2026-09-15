package org.base.api.service.platform;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Provider registration/binding health lifecycle independent of any vendor. */
public final class ProviderLifecycle {
    public enum State { REGISTERED, VALIDATED, BOUND, HEALTHY, FAILED, RETIRED }
    private static final Map<State, Set<State>> TRANSITIONS = Map.of(
            State.REGISTERED, Set.of(State.VALIDATED, State.RETIRED),
            State.VALIDATED, Set.of(State.BOUND, State.FAILED, State.RETIRED),
            State.BOUND, Set.of(State.HEALTHY, State.FAILED, State.RETIRED),
            State.HEALTHY, Set.of(State.FAILED, State.RETIRED),
            State.FAILED, Set.of(State.BOUND, State.RETIRED),
            State.RETIRED, Set.of());
    private final String providerCode;
    private State state = State.REGISTERED;

    public ProviderLifecycle(String providerCode) {
        if (providerCode == null || !providerCode.matches("[A-Za-z][A-Za-z0-9_.-]{0,127}")) throw new IllegalArgumentException("provider code is invalid");
        this.providerCode = providerCode;
    }
    public String providerCode() { return providerCode; }
    public synchronized State state() { return state; }
    public synchronized State advance(State next) {
        Objects.requireNonNull(next, "next state");
        if (next == state) return state;
        if (!TRANSITIONS.getOrDefault(state, Set.of()).contains(next)) throw new IllegalStateException("Invalid provider transition: " + state + " -> " + next);
        state = next;
        return state;
    }
}
