package org.base.api.service.platform;

import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral ordering semantics for a declared stable tuple.  SQL
 * adapters use the same sign contract: positive means the row is after the
 * cursor in the requested direction, negative means before, and zero means
 * the same tuple.  Null ordering is explicit and never delegated to a DB's
 * vendor default.
 */
public final class KeysetTupleComparator {
    private KeysetTupleComparator() { }

    public static int compare(StableSortSpec spec, List<?> left, List<?> cursor, boolean backward) {
        Objects.requireNonNull(spec, "sort spec");
        if (left == null || cursor == null || left.size() != spec.columns().size() || cursor.size() != spec.columns().size())
            throw new IllegalArgumentException("Keyset tuple does not match declared sort spec");
        for (int i = 0; i < spec.columns().size(); i++) {
            var column = spec.columns().get(i);
            Object leftValue = left.get(i), cursorValue = cursor.get(i);
            int result = compareValue(leftValue, cursorValue, column.nullOrder());
            if (result != 0) {
                // NULLS FIRST/LAST is an explicit absolute placement.  Only
                // non-null values reverse under DESC; never the null policy.
                if (leftValue != null && cursorValue != null && column.direction() == StableSortSpec.Direction.DESC) result = -result;
                return backward ? -result : result;
            }
        }
        return 0;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareValue(Object left, Object right, StableSortSpec.NullOrder nullOrder) {
        if (left == right) return 0;
        if (left == null) return nullOrder == StableSortSpec.NullOrder.FIRST ? -1 : 1;
        if (right == null) return nullOrder == StableSortSpec.NullOrder.FIRST ? 1 : -1;
        if (!(left instanceof Comparable comparable) || !left.getClass().isInstance(right))
            throw new IllegalArgumentException("Keyset values must be mutually comparable");
        return comparable.compareTo(right);
    }
}
