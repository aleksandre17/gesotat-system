package org.base.api.service.artifact.run;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable outputs accumulated by the stages of one run. Stages communicate only through named
 * values, so a new stage adds its own keys without changing this type or the persisted shape.
 */
public record PackageRunState(Map<String, String> values) {
    public PackageRunState {
        values = Map.copyOf(values);
    }

    public static PackageRunState empty() {
        return new PackageRunState(Map.of());
    }

    public PackageRunState with(String key, Object value) {
        Map<String, String> next = new LinkedHashMap<>(values);
        next.put(key, String.valueOf(value));
        return new PackageRunState(next);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public String require(String key) {
        String value = values.get(key);
        if (value == null) throw new IllegalStateException("Run state is missing " + key);
        return value;
    }

    public long requireLong(String key) {
        return Long.parseLong(require(key));
    }
}
