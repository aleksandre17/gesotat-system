package org.base.api.service.platform;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * Provider-neutral lifecycle coordinator. It owns lifecycle instances and
 * applies the same governed transition contract to every data family.
 */
public final class DataFamilyLifecycleOrchestrator {
    private final Map<String, DataFamilyLifecycle> lifecycles = new ConcurrentHashMap<>();
    private final LifecycleStateStore store;

    public DataFamilyLifecycleOrchestrator() { this(new LifecycleStateStore() {
        private final Map<String, DataFamilyLifecycle.State> states = new ConcurrentHashMap<>();
        public DataFamilyLifecycle.State load(String id) { return states.get(id); }
        public void save(String id, DataFamilyLifecycle.State state) { states.put(id, state); }
    }); }
    public DataFamilyLifecycleOrchestrator(LifecycleStateStore store) { this.store = Objects.requireNonNull(store, "store"); }

    public DataFamilyLifecycle.State begin(String lifecycleId, PageFamily family) {
        requireFamily(family);
        String identity = key(lifecycleId, family);
        DataFamilyLifecycle.State persisted = store.load(identity);
        DataFamilyLifecycle lifecycle = new DataFamilyLifecycle(identity, persisted == null ? DataFamilyLifecycle.State.RECEIVED : persisted);
        DataFamilyLifecycle previous = lifecycles.putIfAbsent(lifecycle.id(), lifecycle);
        if (previous == null) store.save(identity, lifecycle.state());
        return previous == null ? lifecycle.state() : previous.state();
    }

    public DataFamilyLifecycle.State advance(String lifecycleId, PageFamily family,
                                             DataFamilyLifecycle.State next) {
        requireFamily(family);
        DataFamilyLifecycle lifecycle = lifecycles.get(key(lifecycleId, family));
        if (lifecycle == null) throw new IllegalStateException("Lifecycle has not been started");
        DataFamilyLifecycle.State state = lifecycle.advance(Objects.requireNonNull(next, "next state"));
        store.save(lifecycle.id(), state);
        return state;
    }

    public DataFamilyLifecycle.State state(String lifecycleId, PageFamily family) {
        requireFamily(family);
        DataFamilyLifecycle lifecycle = lifecycles.get(key(lifecycleId, family));
        if (lifecycle == null) throw new IllegalArgumentException("Unknown lifecycle");
        return lifecycle.state();
    }

    public int activeCount() { return lifecycles.size(); }

    /** Validates the common relation/projection contract surface before publication. */
    public void acceptSurface(String lifecycleId, PageFamily family, Map<String, ?> contract) {
        requireFamily(family);
        if (!lifecycles.containsKey(key(lifecycleId, family))) throw new IllegalStateException("Lifecycle has not been started");
        Objects.requireNonNull(contract, "contract");
        validateList(contract, "relations");
        validateList(contract, "projections");
    }

    private static void validateList(Map<String, ?> contract, String name) {
        Object value = contract.get(name);
        if (value == null) return;
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException(name + " must be an array");
        if (list.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException(name + " contains null entry");
    }

    private static String key(String id, PageFamily family) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("lifecycle id is required");
        return family.name() + ":" + id;
    }

    private static void requireFamily(PageFamily family) {
        if (family == null || family == PageFamily.UNKNOWN || family == PageFamily.ROOT)
            throw new IllegalArgumentException("A concrete data family is required");
    }
}
