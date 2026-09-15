package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;

/**
 * Provider-neutral deterministic admission policy for contract queries.
 * The policy is deliberately based only on the declared request shape; it never
 * accepts SQL, table names, or provider-specific hints from a caller.
 */
@Component
public final class QueryAdmissionBudget {
    private final long maximumCost;
    private final Counter accepted;
    private final Counter rejected;

    @Autowired
    public QueryAdmissionBudget(
            @Value("${platform.query.cost.max:5000}") long maximumCost,
            MeterRegistry registry) {
        this(maximumCost, registry.counter("geostat.query.admission.accepted"),
                registry.counter("geostat.query.admission.rejected"));
    }

    /** Constructor retained for deterministic unit tests without a metrics registry. */
    public QueryAdmissionBudget(long maximumCost) {
        this(maximumCost, null, null);
    }

    private QueryAdmissionBudget(long maximumCost, Counter accepted, Counter rejected) {
        if (maximumCost < 1) throw new IllegalArgumentException("platform.query.cost.max must be positive");
        this.maximumCost = maximumCost; this.accepted = accepted; this.rejected = rejected;
    }

    public long maximumCost() { return maximumCost; }

    public long cost(ContractQueryRequest request, int limit) {
        long value = 1;
        value = add(value, request.filters().size(), 2);
        value = add(value, request.select().size(), 1);
        value = add(value, request.groupBy().size(), 8);
        value = add(value, request.include().size(), 10);
        value = add(value, request.includeLimits().size(), 3);
        value = add(value, request.orderBy().size(), 2);
        value = add(value, astNodes(request.where()), 2);
        value = add(value, Math.max(1, limit), 1);
        if (request.aggregation() != null && !request.aggregation().isBlank()) value = add(value, 15, 1);
        if (request.distinct()) value = add(value, 5, 1);
        return value;
    }

    public void admit(ContractQueryRequest request, int limit) {
        long actual = cost(request, limit);
        if (actual > maximumCost) {
            if (rejected != null) rejected.increment();
            throw new QueryAdmissionRejectedException(actual, maximumCost);
        }
        if (accepted != null) accepted.increment();
    }

    private static long add(long current, long count, long weight) {
        if (count <= 0) return current;
        try { return Math.addExact(current, Math.multiplyExact(count, weight)); }
        catch (ArithmeticException overflow) { return Long.MAX_VALUE; }
    }

    /** Counts the complete declarative predicate tree, including map keys and list members. */
    private static long astNodes(Object root) {
        if (root == null) return 0;
        long nodes = 0;
        java.util.ArrayDeque<Object> stack = new java.util.ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Object value = stack.pop();
            nodes++;
            if (value instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    if (entry.getKey() != null) stack.push(entry.getKey());
                    if (entry.getValue() != null) stack.push(entry.getValue());
                }
            } else if (value instanceof Iterable<?> iterable) {
                for (Object child : iterable) if (child != null) stack.push(child);
            }
        }
        return nodes;
    }

    public static final class QueryAdmissionRejectedException extends IllegalArgumentException {
        private final long cost;
        private final long maximum;
        public QueryAdmissionRejectedException(long cost, long maximum) {
            super("Query cost exceeds admission budget");
            this.cost = cost; this.maximum = maximum;
        }
        public long cost() { return cost; }
        public long maximum() { return maximum; }
    }
}
