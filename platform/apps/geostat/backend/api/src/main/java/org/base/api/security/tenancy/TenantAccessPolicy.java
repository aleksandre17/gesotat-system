package org.base.api.security.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * The single authority on "may this caller reach this data product?" (OWASP API1/API5, NIST ABAC).
 *
 * <p>One responsibility: decide. It reads nothing, writes nothing and knows no site, tenant or
 * contract literal. Deny is the default; every allow branch is named by a {@link TenantAccessOutcome}.
 */
@Component
public class TenantAccessPolicy {
    private static final Logger log = LoggerFactory.getLogger(TenantAccessPolicy.class);
    /** Default authority for platform operators. Deliberately not one of the business roles. */
    public static final String DEFAULT_CROSS_TENANT_AUTHORITY = "PLATFORM_CROSS_TENANT";

    private final boolean enforcementEnabled;
    private final boolean oidcEnabled;
    private final String crossTenantAuthority;

    public TenantAccessPolicy(@Value("${platform.tenancy.enforcement-enabled:true}") boolean enforcementEnabled,
                              @Value("${platform.oidc.enabled:false}") boolean oidcEnabled,
                              @Value("${platform.tenancy.cross-tenant-authority:" + DEFAULT_CROSS_TENANT_AUTHORITY + "}") String crossTenantAuthority) {
        if (crossTenantAuthority == null || crossTenantAuthority.isBlank())
            throw new IllegalArgumentException("platform.tenancy.cross-tenant-authority must not be blank");
        String profiles = System.getProperty("spring.profiles.active", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", ""))
                .toLowerCase(Locale.ROOT);
        boolean production = profiles.contains("prod");
        // Fail-closed production guards, in the style of the existing bootstrap/cursor/issuer guards.
        if (production && !enforcementEnabled)
            throw new IllegalStateException("platform.tenancy.enforcement-enabled must stay true in production");
        if (production && !oidcEnabled)
            throw new IllegalStateException("Production tenant-scoped authorization requires platform.oidc.enabled=true; "
                    + "legacy mode carries no tenant claim and must never serve production");
        this.enforcementEnabled = enforcementEnabled;
        this.oidcEnabled = oidcEnabled;
        this.crossTenantAuthority = crossTenantAuthority.trim();
        if (!enforcementEnabled) log.warn("tenancy.enforcement disabled; every product is reachable by any authenticated caller");
    }

    /** The authority that may cross tenant boundaries. Referenced by {@code @PreAuthorize} expressions. */
    public String crossTenantAuthority() {
        return crossTenantAuthority;
    }

    public boolean enforcementEnabled() {
        return enforcementEnabled;
    }

    /**
     * @param product the object's tenancy, or {@code null} when the object or its product does not exist
     */
    public TenantAccessOutcome decide(Caller caller, ProductTenancy product) {
        if (!enforcementEnabled) return TenantAccessOutcome.ALLOWED_ENFORCEMENT_DISABLED;
        if (caller == null) return TenantAccessOutcome.DENIED_NO_CALLER;
        if (caller.kind() == Caller.Kind.SYSTEM) return TenantAccessOutcome.ALLOWED_SYSTEM;
        if (caller.kind() == Caller.Kind.ANONYMOUS) return TenantAccessOutcome.DENIED_NO_CALLER;
        if (caller.hasAuthority(crossTenantAuthority)) {
            // Audit without token content: who, in which mode, against which product.
            log.info("tenancy.cross-tenant-access caller={} authority={} product={} productTenantAssigned={}",
                    caller.auditLabel(), crossTenantAuthority,
                    product == null ? "-" : product.productCode(), product != null && product.assigned());
            return TenantAccessOutcome.ALLOWED_CROSS_TENANT_AUTHORITY;
        }
        /* Legacy/local mode. OIDC is disabled, so no identity in this deployment carries a tenant claim
           and tenant scoping cannot be evaluated. This is admissible only outside production, which the
           constructor guard enforces. */
        if (caller.kind() == Caller.Kind.LEGACY && !oidcEnabled) return TenantAccessOutcome.ALLOWED_LEGACY_MODE;
        if (product == null) return TenantAccessOutcome.DENIED_PRODUCT_UNRESOLVED;
        if (!product.assigned()) return TenantAccessOutcome.DENIED_PRODUCT_UNASSIGNED;
        if (caller.tenant() == null) return TenantAccessOutcome.DENIED_CALLER_TENANT_MISSING;
        return caller.tenant().equals(product.tenantKey())
                ? TenantAccessOutcome.ALLOWED_TENANT_MATCH
                : TenantAccessOutcome.DENIED_TENANT_MISMATCH;
    }
}
