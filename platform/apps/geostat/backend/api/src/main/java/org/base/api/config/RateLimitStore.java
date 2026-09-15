package org.base.api.config;

/** Provider-neutral quota state boundary; distributed stores can implement this without changing the filter. */
public interface RateLimitStore {
    Window acquire(String key, long nowMillis, long windowMillis, int maxKeys);
    record Window(long startedAt, int used) {}
}
