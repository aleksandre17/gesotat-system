package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractChecksumBindingTest {
    @Test void createsDeterministicSha256AndMatchesCaseInsensitively() {
        var checksum = ContractChecksumBinding.sha256("{\"revision\":8}");
        assertEquals(64, checksum.length());
        assertTrue(ContractChecksumBinding.matches("{\"revision\":8}", checksum.toUpperCase()));
        assertFalse(ContractChecksumBinding.matches("{\"revision\":9}", checksum));
    }

    @Test void rejectsMalformedOrMissingExpectedChecksum() {
        assertFalse(ContractChecksumBinding.matches("x", null));
        assertFalse(ContractChecksumBinding.matches("x", "not-a-digest"));
        assertThrows(IllegalArgumentException.class, () -> ContractChecksumBinding.sha256(null));
    }
}
