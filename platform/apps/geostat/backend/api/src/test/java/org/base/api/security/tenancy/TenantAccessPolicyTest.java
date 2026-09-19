package org.base.api.security.tenancy;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every allow and deny branch of the tenancy decision (OWASP API1/API5, NIST ABAC, deny by default). */
class TenantAccessPolicyTest {
    private static final String AUTHORITY = TenantAccessPolicy.DEFAULT_CROSS_TENANT_AUTHORITY;
    private static final ProductTenancy OWNED = new ProductTenancy(1, "PRODUCT_A", "tenant-a");
    private static final ProductTenancy UNASSIGNED = new ProductTenancy(2, "PRODUCT_B", null);

    private static TenantAccessPolicy oidcPolicy() {
        return new TenantAccessPolicy(true, true, AUTHORITY);
    }

    private static Caller oidc(String tenant, String... authorities) {
        return new Caller("sub-1", tenant, Set.of(authorities), Caller.Kind.OIDC);
    }

    @Test
    void matchingTenantIsAllowed() {
        assertEquals(TenantAccessOutcome.ALLOWED_TENANT_MATCH, oidcPolicy().decide(oidc("tenant-a"), OWNED));
    }

    @Test
    void foreignTenantIsDenied() {
        TenantAccessOutcome outcome = oidcPolicy().decide(oidc("tenant-b"), OWNED);
        assertEquals(TenantAccessOutcome.DENIED_TENANT_MISMATCH, outcome);
        assertFalse(outcome.allowed());
    }

    @Test
    void unassignedProductIsDeniedToEveryOrdinaryCaller() {
        assertEquals(TenantAccessOutcome.DENIED_PRODUCT_UNASSIGNED, oidcPolicy().decide(oidc("tenant-a"), UNASSIGNED));
    }

    @Test
    void unresolvedProductIsDenied() {
        assertEquals(TenantAccessOutcome.DENIED_PRODUCT_UNRESOLVED, oidcPolicy().decide(oidc("tenant-a"), null));
    }

    @Test
    void blankOrMissingTenantClaimIsDenied() {
        assertEquals(TenantAccessOutcome.DENIED_CALLER_TENANT_MISSING, oidcPolicy().decide(oidc("   "), OWNED));
        assertEquals(TenantAccessOutcome.DENIED_CALLER_TENANT_MISSING, oidcPolicy().decide(oidc(null), OWNED));
    }

    @Test
    void anonymousAndAbsentCallersAreDenied() {
        assertEquals(TenantAccessOutcome.DENIED_NO_CALLER, oidcPolicy().decide(Caller.anonymous(), OWNED));
        assertEquals(TenantAccessOutcome.DENIED_NO_CALLER, oidcPolicy().decide(null, OWNED));
    }

    @Test
    void crossTenantAuthorityReachesEveryTenantIncludingUnassignedProducts() {
        assertEquals(TenantAccessOutcome.ALLOWED_CROSS_TENANT_AUTHORITY, oidcPolicy().decide(oidc("tenant-b", AUTHORITY), OWNED));
        assertEquals(TenantAccessOutcome.ALLOWED_CROSS_TENANT_AUTHORITY, oidcPolicy().decide(oidc(null, AUTHORITY), UNASSIGNED));
    }

    @Test
    void businessAuthoritiesNeverCrossTenantBoundaries() {
        assertEquals(TenantAccessOutcome.DENIED_TENANT_MISMATCH,
                oidcPolicy().decide(oidc("tenant-b", "READ_RESOURCE", "WRITE_RESOURCE", "PUBLISH_RESOURCE", "ADMIN"), OWNED));
    }

    @Test
    void systemCallerIsAllowedAndIsNeverImplicit() {
        assertEquals(TenantAccessOutcome.ALLOWED_SYSTEM, oidcPolicy().decide(Caller.system("JOB"), UNASSIGNED));
        // an absent caller is not a system caller
        assertEquals(TenantAccessOutcome.DENIED_NO_CALLER, oidcPolicy().decide(Caller.anonymous(), UNASSIGNED));
    }

    @Test
    void legacyModeIsAllowedOnlyWhileOidcIsDisabled() {
        Caller legacy = new Caller("local-user", null, Set.of("READ_RESOURCE"), Caller.Kind.LEGACY);
        assertEquals(TenantAccessOutcome.ALLOWED_LEGACY_MODE, new TenantAccessPolicy(true, false, AUTHORITY).decide(legacy, OWNED));
        assertEquals(TenantAccessOutcome.DENIED_CALLER_TENANT_MISSING, oidcPolicy().decide(legacy, OWNED));
    }

    @Test
    void enforcementCanBeSwitchedOffForLocalDevelopmentOnly() {
        assertTrue(new TenantAccessPolicy(false, false, AUTHORITY).decide(oidc("tenant-b"), OWNED).allowed());
    }

    @Test
    void blankCrossTenantAuthorityIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TenantAccessPolicy(true, true, "  "));
    }

    @Test
    void productionRefusesDisabledEnforcementAndLegacyMode() throws Exception {
        String previous = System.getProperty("spring.profiles.active");
        System.setProperty("spring.profiles.active", "prod");
        try {
            assertThrows(IllegalStateException.class, () -> new TenantAccessPolicy(false, true, AUTHORITY));
            assertThrows(IllegalStateException.class, () -> new TenantAccessPolicy(true, false, AUTHORITY));
            assertEquals(AUTHORITY, new TenantAccessPolicy(true, true, AUTHORITY).crossTenantAuthority());
        } finally {
            if (previous == null) System.clearProperty("spring.profiles.active");
            else System.setProperty("spring.profiles.active", previous);
        }
    }
}
