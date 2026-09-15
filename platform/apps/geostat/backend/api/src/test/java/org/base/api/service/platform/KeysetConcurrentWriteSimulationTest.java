package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Provider-neutral regression for cursor stability when rows arrive between pages. */
class KeysetConcurrentWriteSimulationTest {
    @Test void forwardCursorDoesNotRepeatOrLoseExistingRowsAfterInsert() {
        var spec = new StableSortSpec(List.of(
                new StableSortSpec.Column("sequence", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST)), "IX_SEQUENCE");
        var before = new ArrayList<>(List.of(1, 2, 3, 4));
        var cursor = List.of((Object) 2);
        before.add(0, 0); // concurrent insert before the last-seen key
        var visible = before.stream().filter(v -> KeysetTupleComparator.compare(spec, List.of(v), cursor, false) > 0).toList();
        assertEquals(List.of(3, 4), visible);
        assertEquals(2, new HashSet<>(visible).size());
    }

    @Test void backwardCursorTraversesTheSameTupleInReverse() {
        var spec = new StableSortSpec(List.of(
                new StableSortSpec.Column("sequence", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST)), "IX_SEQUENCE");
        var cursor = List.of((Object) 3);
        var visible = List.of(1, 2, 3, 4).stream()
                .filter(v -> KeysetTupleComparator.compare(spec, List.of(v), cursor, true) > 0).toList();
        var reverse = new ArrayList<>(visible); Collections.reverse(reverse);
        assertEquals(List.of(2, 1), reverse);
    }
}
