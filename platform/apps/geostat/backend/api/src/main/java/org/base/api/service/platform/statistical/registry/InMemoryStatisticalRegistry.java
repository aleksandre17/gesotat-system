package org.base.api.service.platform.statistical.registry;

import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.StructureDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic registry used by previews, conformance suites and tests. Visibility is explicit per entry:
 * {@code GLOBAL} entries are visible to every scope, product entries only to their owner (register Q07).
 */
public final class InMemoryStatisticalRegistry implements StatisticalRegistry {
    public static final String GLOBAL = "*";

    private record Entry(Object value, Lifecycle lifecycle, Set<String> visibleTo) { }

    private final Map<Ref, Entry> entries = new HashMap<>();

    public InMemoryStatisticalRegistry put(Ref ref, Object value, Lifecycle lifecycle, String... visibleTo) {
        entries.put(ref, new Entry(value, lifecycle, visibleTo.length == 0 ? Set.of(GLOBAL) : Set.of(visibleTo)));
        return this;
    }

    public InMemoryStatisticalRegistry approved(Ref ref) { return put(ref, ref, Lifecycle.APPROVED); }

    public InMemoryStatisticalRegistry approved(Ref ref, Object value) { return put(ref, value, Lifecycle.APPROVED); }

    private <T> Optional<T> visible(Ref ref, Scope scope, Class<T> type) {
        Entry e = entries.get(ref);
        if (e == null || !(e.visibleTo().contains(GLOBAL) || e.visibleTo().contains(scope.productCode()))) return Optional.empty();
        return type.isInstance(e.value()) ? Optional.of(type.cast(e.value())) : Optional.empty();
    }

    @Override public Optional<StructureDefinition> structure(Ref ref, Scope scope) { return visible(ref, scope, StructureDefinition.class); }

    @Override public Optional<MeasureDefinition> measure(Ref ref, Scope scope) { return visible(ref, scope, MeasureDefinition.class); }

    @Override public Optional<Codelist> codelist(Ref ref, Scope scope) { return visible(ref, scope, Codelist.class); }

    @Override public Optional<Lifecycle> lifecycle(Ref ref, Scope scope) {
        return visible(ref, scope, Object.class).map(v -> entries.get(ref).lifecycle());
    }
}
