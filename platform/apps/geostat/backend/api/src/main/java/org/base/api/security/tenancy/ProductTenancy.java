package org.base.api.security.tenancy;

/**
 * Tenant ownership of one data product (NIST SP 800-162 object attribute).
 *
 * @param tenantKey the owning tenant, or {@code null} while the product is UNASSIGNED
 */
public record ProductTenancy(long productId, String productCode, String tenantKey) {
    public ProductTenancy {
        tenantKey = tenantKey == null || tenantKey.isBlank() ? null : tenantKey.trim();
    }

    public boolean assigned() {
        return tenantKey != null;
    }
}
