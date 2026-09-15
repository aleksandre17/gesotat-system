package org.base.api.service.platform;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Contract-selected serving adapters.  The generic serving service knows only
 * this port; family semantics live behind registered adapters and can be
 * replaced by a provider/site without changing the orchestration layer.
 */
public final class PageDataAdapterRegistry {
    public record ReadContext(Map<String, Object> metadata, int page, int limit,
                              Map<String, Object> filters, Map<String, Object> where,
                              String metricCode, String carrierCode, String periodFrom,
                              String periodTo, String ageGroup) {
        public ReadContext {
            metadata = Map.copyOf(metadata == null ? Map.of() : metadata);
            filters = Map.copyOf(filters == null ? Map.of() : filters);
            where = Map.copyOf(where == null ? Map.of() : where);
        }
    }

    @FunctionalInterface
    public interface Reader extends Function<ReadContext, List<Map<String, Object>>> {}

    private final EnumMap<PageFamily, Reader> readers = new EnumMap<>(PageFamily.class);

    public void register(PageFamily family, Reader reader) {
        PageFamily key = Objects.requireNonNull(family, "family");
        if (readers.putIfAbsent(key, Objects.requireNonNull(reader, "reader")) != null) {
            throw new IllegalArgumentException("Duplicate page data adapter: " + key);
        }
    }

    public Reader require(PageFamily family) {
        Reader reader = readers.get(Objects.requireNonNull(family, "family"));
        if (reader == null) throw new IllegalArgumentException("No page data adapter registered for: " + family);
        return reader;
    }
}
