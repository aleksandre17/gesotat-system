package org.base.api.service.platform;

/** Provider-neutral persistence port for contract revision lifecycle state. */
public interface ContractLifecycleStateStore {
    ContractLifecycle.State load(String revisionKey);
    void save(String revisionKey, ContractLifecycle.State state);
}
