package org.base.api.security.tenancy;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The guard resolves each addressed object once and reports the decision in both boundary shapes. */
class TenantAccessGuardTest {
    private static final ProductTenancy OWNED = new ProductTenancy(1, "PRODUCT_A", "tenant-a");

    private static Caller caller(String tenant) {
        return new Caller("sub", tenant, Set.of("READ_RESOURCE"), Caller.Kind.OIDC);
    }

    @Test
    void everyScopeKindIsEnforced() {
        TenantAccessGuard denied = TenantAccessGuards.enforcing(caller("tenant-b"), OWNED);
        assertFalse(denied.permitsContract("ANY_CONTRACT"));
        assertFalse(denied.permitsDatasetVersion(7L));
        assertFalse(denied.permitsManifest(7));
        assertFalse(denied.permitsSnapshot(7));
        assertFalse(denied.permitsPackageRun(7));
        assertFalse(denied.permitsUploadSession(UUID.randomUUID()));
        assertFalse(denied.permitsProductCode("PRODUCT_A"));
    }

    @Test
    void ownTenantPassesEveryScopeKind() {
        TenantAccessGuard allowed = TenantAccessGuards.enforcing(caller("tenant-a"), OWNED);
        assertTrue(allowed.permitsContract("ANY_CONTRACT"));
        assertDoesNotThrow(() -> allowed.requireContract("ANY_CONTRACT"));
        assertDoesNotThrow(() -> allowed.requireManifest(7));
        assertDoesNotThrow(() -> allowed.requireSnapshot(7));
        assertDoesNotThrow(() -> allowed.requirePackageRun(7));
        assertDoesNotThrow(() -> allowed.requireDatasetVersion(7L));
    }

    @Test
    void requireThrowsADenialThatCarriesNoObjectAttribute() {
        TenantAccessGuard guard = TenantAccessGuards.enforcing(caller("tenant-b"), OWNED);
        TenantAccessDeniedException denial = assertThrows(TenantAccessDeniedException.class, () -> guard.requireContract("SECRET_CONTRACT"));
        assertEquals(TenantAccessDeniedException.DETAIL, denial.getMessage());
        assertFalse(denial.getMessage().contains("SECRET_CONTRACT"));
        assertFalse(denial.getMessage().contains("tenant-a"));
        assertEquals(TenantAccessOutcome.DENIED_TENANT_MISMATCH, denial.outcome());
    }

    @Test
    void anUnresolvedObjectIsDeniedIdenticallyToAForeignObject() {
        TenantAccessGuard missing = TenantAccessGuards.enforcing(caller("tenant-a"), null);
        TenantAccessGuard foreign = TenantAccessGuards.enforcing(caller("tenant-b"), OWNED);
        assertFalse(missing.permitsManifest(1));
        assertFalse(foreign.permitsManifest(1));
        assertEquals(assertThrows(TenantAccessDeniedException.class, () -> missing.requireManifest(1)).getMessage(),
                assertThrows(TenantAccessDeniedException.class, () -> foreign.requireManifest(1)).getMessage());
    }

    @Test
    void systemScopeIsExplicitAndBounded() {
        CurrentCaller callers = new SecurityContextCurrentCaller("tenant_id", true);
        assertEquals(Caller.Kind.ANONYMOUS, callers.current().kind());
        assertEquals(Caller.Kind.SYSTEM, callers.asSystem("JOB", () -> callers.current()).kind());
        assertEquals(Caller.Kind.ANONYMOUS, callers.current().kind(), "the system scope must not outlive the job");
    }
}
