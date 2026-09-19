package org.base.api.security.tenancy;

/** Every admissible result of one tenancy decision. Deny is the default; allow is always named. */
public enum TenantAccessOutcome {
    /** Enforcement is switched off for local development. Refused in production by a startup guard. */
    ALLOWED_ENFORCEMENT_DISABLED(true),
    /** An explicit internal scheduler/worker/audit scope. */
    ALLOWED_SYSTEM(true),
    /** The caller holds the platform-wide cross-tenant authority. Audited on every use. */
    ALLOWED_CROSS_TENANT_AUTHORITY(true),
    /** Legacy/local mode: OIDC is disabled, so no token carries a tenant claim. */
    ALLOWED_LEGACY_MODE(true),
    /** The caller's tenant claim equals the product's owning tenant. */
    ALLOWED_TENANT_MATCH(true),

    /** No authenticated caller. */
    DENIED_NO_CALLER(false),
    /** The object does not exist, or its product could not be resolved. */
    DENIED_PRODUCT_UNRESOLVED(false),
    /** The product has no owning tenant yet; nobody but the cross-tenant authority may reach it. */
    DENIED_PRODUCT_UNASSIGNED(false),
    /** The caller presented no tenant claim, or a blank one. */
    DENIED_CALLER_TENANT_MISSING(false),
    /** The caller's tenant is not the product's tenant (OWASP API1). */
    DENIED_TENANT_MISMATCH(false);

    private final boolean allowed;

    TenantAccessOutcome(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean allowed() {
        return allowed;
    }
}
