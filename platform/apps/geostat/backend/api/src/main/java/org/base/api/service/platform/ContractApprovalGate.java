package org.base.api.service.platform;

import java.util.Map;
import java.util.Objects;

/** Single fail-closed gate joining lifecycle, checksum and semantic approval. */
public final class ContractApprovalGate {
    public record Decision(boolean allowed, String reason) {}
    private ContractApprovalGate() {}
    public static Decision evaluate(ContractLifecycle lifecycle, String document, String expectedChecksum,
                                    Map<String,?> previous, Map<String,?> current) {
        Objects.requireNonNull(lifecycle); Objects.requireNonNull(document);
        if (lifecycle.state() != ContractLifecycle.State.APPROVED) return new Decision(false, "CONTRACT_NOT_APPROVED");
        if (!ContractChecksumBinding.matches(document, expectedChecksum)) return new Decision(false, "CHECKSUM_MISMATCH");
        if (previous != null && current != null && SemanticCompatibilityAnalyzer.compare(previous, current).breaking())
            return new Decision(false, "BREAKING_CHANGE_REQUIRES_APPROVAL");
        return new Decision(true, "APPROVED");
    }
}
