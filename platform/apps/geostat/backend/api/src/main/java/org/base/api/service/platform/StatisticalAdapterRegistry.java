package org.base.api.service.platform;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Explicit provider-to-adapter registry; no storage table names leak to callers. */
@Component
public final class StatisticalAdapterRegistry {
    private final Map<String, StatisticalQueryAdapter> adapters = new ConcurrentHashMap<>();

    public void register(String providerCode, StatisticalQueryAdapter adapter) {
        if (providerCode == null || !providerCode.matches("[A-Za-z][A-Za-z0-9_.-]{0,127}"))
            throw new IllegalArgumentException("Invalid provider code");
        Objects.requireNonNull(adapter, "adapter");
        if (adapters.putIfAbsent(providerCode, adapter) != null)
            throw new IllegalStateException("Statistical adapter already registered: " + providerCode);
    }

    public StatisticalQueryAdapter require(String providerCode) {
        StatisticalQueryAdapter adapter = adapters.get(providerCode);
        if (adapter == null) throw new IllegalArgumentException("No approved statistical adapter: " + providerCode);
        return adapter;
    }

    public int size() { return adapters.size(); }
}
