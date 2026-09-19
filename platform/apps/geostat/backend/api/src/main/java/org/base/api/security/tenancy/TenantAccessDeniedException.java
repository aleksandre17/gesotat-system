package org.base.api.security.tenancy;

/**
 * Object- or function-level tenancy denial. The message is a constant: it must never reveal whether
 * the object exists, which tenant owns it, or which rule denied it (OWASP API1 existence oracle).
 */
public class TenantAccessDeniedException extends RuntimeException {
    public static final String DETAIL = "This resource is not available to the caller's tenant";

    private final transient TenantAccessOutcome outcome;

    public TenantAccessDeniedException(TenantAccessOutcome outcome) {
        super(DETAIL);
        this.outcome = outcome;
    }

    /** For logging and tests only. It is never serialized into a response. */
    public TenantAccessOutcome outcome() {
        return outcome;
    }
}
