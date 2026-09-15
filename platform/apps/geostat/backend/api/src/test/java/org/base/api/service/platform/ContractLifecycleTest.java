package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContractLifecycleTest {
    @Test
    void followsApprovalAndSupersedePath() {
        var lifecycle = new ContractLifecycle("KIDS_PORTAL_V1:8");
        assertEquals(ContractLifecycle.State.DRAFT, lifecycle.state());
        lifecycle.advance(ContractLifecycle.State.REVIEW_REQUIRED);
        lifecycle.advance(ContractLifecycle.State.APPROVED);
        assertEquals(ContractLifecycle.State.APPROVED, lifecycle.advance(ContractLifecycle.State.APPROVED));
        assertEquals(ContractLifecycle.State.SUPERSEDED, lifecycle.advance(ContractLifecycle.State.SUPERSEDED));
    }

    @Test
    void rejectsSkippingReviewAndMutationAfterTerminalState() {
        var lifecycle = new ContractLifecycle("P:1");
        assertThrows(IllegalStateException.class, () -> lifecycle.advance(ContractLifecycle.State.APPROVED));
        lifecycle.advance(ContractLifecycle.State.REVIEW_REQUIRED);
        lifecycle.advance(ContractLifecycle.State.APPROVED);
        lifecycle.advance(ContractLifecycle.State.ROLLED_BACK);
        assertThrows(IllegalStateException.class, () -> lifecycle.advance(ContractLifecycle.State.DRAFT));
    }
}
