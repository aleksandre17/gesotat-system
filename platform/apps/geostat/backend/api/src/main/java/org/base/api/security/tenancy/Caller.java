package org.base.api.security.tenancy;

import java.util.Set;

/**
 * The authenticated identity a tenancy decision is made for, reduced to the attributes the policy
 * needs (NIST SP 800-162 subject attributes). It never carries the token or any credential.
 *
 * @param subject   pseudonymous subject identifier, for audit only; may be {@code null}
 * @param tenant    value of the configured tenant claim; {@code null} when the caller has none
 * @param authorities granted authorities of the caller
 * @param kind      how the caller was authenticated, which decides the admissible policy branches
 */
public record Caller(String subject, String tenant, Set<String> authorities, Caller.Kind kind) {

    /** Authentication mode a caller arrived in. Each mode has an explicit, documented policy branch. */
    public enum Kind {
        /** Standards-based resource-server mode: an RS256 token with a validated tenant claim. */
        OIDC,
        /** Legacy/local HMAC mode; only admissible while {@code platform.oidc.enabled} is false. */
        LEGACY,
        /** An internal scheduler, worker or audit job with no request caller. */
        SYSTEM,
        /** No authentication at all. Always denied. */
        ANONYMOUS
    }

    public Caller {
        authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
        tenant = tenant == null || tenant.isBlank() ? null : tenant.trim();
    }

    public static Caller anonymous() {
        return new Caller(null, null, Set.of(), Kind.ANONYMOUS);
    }

    public static Caller system(String reason) {
        return new Caller(reason == null || reason.isBlank() ? "system" : reason.trim(), null, Set.of(), Kind.SYSTEM);
    }

    public boolean hasAuthority(String authority) {
        return authority != null && !authority.isBlank() && authorities.contains(authority);
    }

    /** Stable, non-sensitive label for audit lines. */
    public String auditLabel() {
        return kind + ":" + (subject == null ? "-" : subject);
    }
}
