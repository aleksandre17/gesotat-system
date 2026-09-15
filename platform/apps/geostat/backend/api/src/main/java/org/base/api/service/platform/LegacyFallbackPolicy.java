package org.base.api.service.platform;

/** Explicit compatibility boundary for pre-contract dynamic pages. */
public final class LegacyFallbackPolicy {
    private LegacyFallbackPolicy() { }

    public static Decision evaluate(boolean enabled, boolean approvedContractResolved) {
        if (approvedContractResolved) return new Decision(false, "APPROVED_CONTRACT");
        if (enabled) return new Decision(true, "EXPLICIT_COMPATIBILITY_FLAG");
        return new Decision(false, "LEGACY_FALLBACK_DISABLED");
    }

    public record Decision(boolean allowed, String reason) { }
}
