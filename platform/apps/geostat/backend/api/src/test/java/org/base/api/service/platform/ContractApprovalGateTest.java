package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ContractApprovalGateTest {
    @Test void gateRequiresApprovedLifecycleAndMatchingChecksum() {
        var lifecycle = new ContractLifecycle("X:1");
        String doc = "{\"revision\":1}"; String checksum = ContractChecksumBinding.sha256(doc);
        assertFalse(ContractApprovalGate.evaluate(lifecycle, doc, checksum, null, null).allowed());
        lifecycle.advance(ContractLifecycle.State.REVIEW_REQUIRED); lifecycle.advance(ContractLifecycle.State.APPROVED);
        assertTrue(ContractApprovalGate.evaluate(lifecycle, doc, checksum, null, null).allowed());
        assertFalse(ContractApprovalGate.evaluate(lifecycle, doc, "bad", null, null).allowed());
    }
    @Test void breakingSemanticChangeIsBlocked() {
        var lifecycle = new ContractLifecycle("X:1"); lifecycle.advance(ContractLifecycle.State.REVIEW_REQUIRED); lifecycle.advance(ContractLifecycle.State.APPROVED);
        String doc = "{}"; var result = ContractApprovalGate.evaluate(lifecycle, doc, ContractChecksumBinding.sha256(doc), Map.of("unit", "PERSON"), Map.of("unit", "HOUSEHOLD"));
        assertFalse(result.allowed()); assertEquals("BREAKING_CHANGE_REQUIRES_APPROVAL", result.reason());
    }
}
