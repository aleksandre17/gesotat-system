package org.base.api.service.platform;

/** Provider-neutral persistence port for lifecycle state. Implementations may use SQL, Redis or an event store. */
public interface LifecycleStateStore {
    DataFamilyLifecycle.State load(String lifecycleId);
    void save(String lifecycleId, DataFamilyLifecycle.State state);
}
