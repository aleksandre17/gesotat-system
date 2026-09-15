package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StableSortSpecTest {
    @Test void acceptsContractDeclaredTupleAndNullPolicy() {
        var spec = new StableSortSpec(List.of(
                new StableSortSpec.Column("published_at", StableSortSpec.Direction.DESC, StableSortSpec.NullOrder.LAST),
                new StableSortSpec.Column("record_id", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.FIRST)),
                "IX_PUBLICATION_PUBLISHED_AT_RECORD_ID");
        assertEquals(2, spec.columns().size());
        assertEquals(StableSortSpec.NullOrder.LAST, spec.columns().get(0).nullOrder());
    }

    @Test void rejectsUnsafeDuplicateOrMissingIndexMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new StableSortSpec(List.of(new StableSortSpec.Column("id;drop", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST)), "IX_ID"));
        var duplicate = List.of(new StableSortSpec.Column("id", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST), new StableSortSpec.Column("id", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST));
        assertThrows(IllegalArgumentException.class, () -> new StableSortSpec(duplicate, "IX_ID"));
        assertThrows(IllegalArgumentException.class, () -> new StableSortSpec(List.of(new StableSortSpec.Column("id", StableSortSpec.Direction.ASC, StableSortSpec.NullOrder.LAST)), ""));
    }
}
