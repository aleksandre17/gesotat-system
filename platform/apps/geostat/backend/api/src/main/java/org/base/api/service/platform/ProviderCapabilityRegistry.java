package org.base.api.service.platform;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Provider-neutral capability registry. The registry stores approved metadata,
 * never caller-supplied SQL or executable provider configuration.
 */
@Component
public final class ProviderCapabilityRegistry {
    public record Capability(String providerCode, Set<String> families, Set<String> operations,
                             Set<String> features, int maxPageSize, boolean transactional) {
        public Capability {
            providerCode = requireCode(providerCode, "providerCode");
            families = normalized(families, "families");
            operations = normalized(operations, "operations");
            features = normalized(features, "features");
            if (maxPageSize < 1 || maxPageSize > 1_000_000) throw new IllegalArgumentException("maxPageSize out of bounds");
        }
        private static Set<String> normalized(Set<String> values, String name) {
            if (values == null || values.isEmpty()) throw new IllegalArgumentException(name + " is required");
            TreeSet<String> out = new TreeSet<>();
            for (String value : values) out.add(requireCode(value, name + " entry"));
            return Collections.unmodifiableSet(out);
        }
        private static String requireCode(String value, String name) {
            if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_.-]{0,127}")) throw new IllegalArgumentException(name + " is invalid");
            return value;
        }
    }

    private final Map<String, Capability> active = new ConcurrentHashMap<>();

    public void register(Capability capability) {
        Objects.requireNonNull(capability, "capability");
        if (active.putIfAbsent(capability.providerCode(), capability) != null)
            throw new IllegalStateException("Provider already registered: " + capability.providerCode());
    }

    /** Atomically replaces the discovered ACTIVE set; stale providers cannot survive a reload. */
    public synchronized void replaceAll(Collection<Capability> capabilities) {
        Objects.requireNonNull(capabilities, "capabilities");
        Map<String, Capability> next = new HashMap<>();
        for (Capability capability : capabilities) {
            Objects.requireNonNull(capability, "capability");
            if (next.putIfAbsent(capability.providerCode(), capability) != null)
                throw new IllegalStateException("Duplicate provider: " + capability.providerCode());
        }
        active.clear();
        active.putAll(next);
    }

    public Capability require(String providerCode, String family, String operation) {
        Capability capability = active.get(providerCode);
        if (capability == null) throw new IllegalArgumentException("Unknown provider: " + providerCode);
        if (!capability.families().contains(family) || !capability.operations().contains(operation))
            throw new IllegalArgumentException("Provider capability is not approved for requested operation");
        return capability;
    }

    public Optional<Capability> find(String providerCode) { return Optional.ofNullable(active.get(providerCode)); }
    public void retire(String providerCode) { if (active.remove(providerCode) == null) throw new IllegalArgumentException("Unknown provider: " + providerCode); }
}
