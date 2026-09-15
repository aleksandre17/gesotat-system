package org.base.api.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AccessFileImporterTest {

    @Test
    void convertsWholeNumberYearValuesWithoutDecimalSuffix() {
        assertEquals(2025L, AccessFileImporter.normalizeYearValue("year", 2025.0d));
        assertEquals(2025L, AccessFileImporter.normalizeYearValue("year_of_production", new BigDecimal("2025.0")));
        assertEquals("2025", AccessFileImporter.normalizeYearValue("YEAR", " 2025.0 "));
    }

    @Test
    void preservesNonYearAndFractionalValues() {
        Double fractionalYear = 2025.5d;
        Double nonYear = 2025.0d;

        assertSame(fractionalYear, AccessFileImporter.normalizeYearValue("year", fractionalYear));
        assertSame(nonYear, AccessFileImporter.normalizeYearValue("quantity", nonYear));
    }
}
