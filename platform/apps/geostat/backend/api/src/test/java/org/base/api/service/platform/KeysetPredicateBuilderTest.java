package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class KeysetPredicateBuilderTest {
    @Test void buildsAscendingLexicographicTuplePredicate() {
        var result = KeysetPredicateBuilder.build(List.of("created_at", "record_id"), List.of("2026-01-01", 42), false);
        assertEquals(" AND (([created_at] > ?) OR ([created_at] = ? AND [record_id] > ?))", result.sql());
        assertEquals(List.of("2026-01-01", "2026-01-01", 42), result.parameters());
    }

    @Test void buildsDescendingLexicographicTuplePredicate() {
        var result = KeysetPredicateBuilder.build(List.of("priority", "id"), List.of(10, 7), true);
        assertTrue(result.sql().contains("[priority] < ?"));
        assertTrue(result.sql().contains("[id] < ?"));
        assertEquals(3, result.parameters().size());
    }

    @Test void rejectsMismatchedTupleAndUnsafeIdentifier() {
        assertThrows(IllegalArgumentException.class, () -> KeysetPredicateBuilder.build(List.of("id"), List.of(), false));
        assertThrows(IllegalArgumentException.class, () -> KeysetPredicateBuilder.build(List.of("id;drop"), List.of(1), false));
        assertThrows(IllegalArgumentException.class, () -> KeysetPredicateBuilder.build(List.of("id"), List.of(1, 2), false));
    }

    @Test void buildsMixedDirectionForwardAndBackwardPredicates() {
        var spec = new StableSortSpec(List.of(
                new StableSortSpec.Column("published_at", StableSortSpec.Direction.DESC, StableSortSpec.NullOrder.LAST),
                new StableSortSpec.Column("id", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST)), "ix_page_sort");
        var forward = KeysetPredicateBuilder.build(spec, List.of("2026-01-01", 7), false);
        assertTrue(forward.sql().contains("[published_at] < ?"));
        assertTrue(forward.sql().contains("[id] > ?"));
        var backward = KeysetPredicateBuilder.build(spec, List.of("2026-01-01", 7), true);
        assertTrue(backward.sql().contains("[published_at] > ?"));
        assertTrue(backward.sql().contains("[id] < ?"));
    }

    @Test void encodesNullOrderingWithoutVendorDefaults() {
        var first = new StableSortSpec(List.of(
                new StableSortSpec.Column("rank", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.FIRST)), "ix_rank");
        var afterNull = KeysetPredicateBuilder.build(first, java.util.Collections.singletonList(null), false);
        assertTrue(afterNull.sql().contains("[rank] IS NOT NULL"));
        assertTrue(afterNull.parameters().isEmpty());

        var last = new StableSortSpec(List.of(
                new StableSortSpec.Column("rank", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST)), "ix_rank");
        var afterValue = KeysetPredicateBuilder.build(last, List.of(10), false);
        assertTrue(afterValue.sql().contains("[rank] IS NULL"));
        assertEquals(List.of(10), afterValue.parameters());
    }
}
