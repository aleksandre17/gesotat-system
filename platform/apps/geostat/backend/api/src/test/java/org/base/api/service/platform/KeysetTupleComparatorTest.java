package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeysetTupleComparatorTest {
    private static StableSortSpec spec() {
        return new StableSortSpec(List.of(
                new StableSortSpec.Column("published_at", StableSortSpec.Direction.DESC, StableSortSpec.NullOrder.LAST),
                new StableSortSpec.Column("id", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.FIRST)), "IX_PUBLISHED_ID");
    }

    @Test void appliesMixedDirectionAndExplicitNullOrdering() {
        assertTrue(KeysetTupleComparator.compare(spec(), List.of("2026-02-01", 1), List.of("2026-01-01", 9), false) < 0);
        assertTrue(KeysetTupleComparator.compare(spec(), java.util.Arrays.asList(null, 1), List.of("2026-01-01", 9), false) > 0);
        assertTrue(KeysetTupleComparator.compare(spec(), java.util.Arrays.asList(null, 1), java.util.Arrays.asList(null, 9), false) < 0);
    }

    @Test void backwardTraversalIsExactReverseAndEqualTupleIsStable() {
        List<Object> cursor = List.of("2026-01-01", 9);
        List<Object> row = List.of("2026-01-02", 1);
        int forward = KeysetTupleComparator.compare(spec(), row, cursor, false);
        int backward = KeysetTupleComparator.compare(spec(), row, cursor, true);
        assertEquals(-Integer.signum(forward), Integer.signum(backward));
        assertEquals(0, KeysetTupleComparator.compare(spec(), cursor, cursor, false));
    }

    @Test void rejectsWrongArityAndIncomparableValues() {
        assertThrows(IllegalArgumentException.class, () -> KeysetTupleComparator.compare(spec(), List.of(1), List.of(1), false));
        assertThrows(IllegalArgumentException.class, () -> KeysetTupleComparator.compare(spec(), List.of(1, 1), List.of("x", 1), false));
    }
}
