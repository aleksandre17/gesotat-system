package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PageFamilyTest {
    @Test void resolvesDeclaredFamiliesWithoutProductNames() {
        assertEquals(PageFamily.STATISTICAL, PageFamily.fromNodeKind("STATISTICAL_DATAFLOW"));
        assertEquals(PageFamily.REFERENCE, PageFamily.fromNodeKind("REFERENCE_REGISTRY"));
        assertEquals(PageFamily.ENTITY, PageFamily.fromNodeKind("ENTITY_COLLECTION"));
        assertEquals(PageFamily.UNKNOWN, PageFamily.fromNodeKind("CUSTOM_FAMILY"));
    }
}
