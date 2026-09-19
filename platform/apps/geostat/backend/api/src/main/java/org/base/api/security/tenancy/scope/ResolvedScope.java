package org.base.api.security.tenancy.scope;

import org.base.api.security.tenancy.ProductTenancy;

/**
 * One resolved route identity: which kind of identifier named the object, its value (for audit), and
 * the owning product — {@code null} when the identifier exists but resolves to no product, which the
 * policy denies exactly as it denies a foreign product.
 */
public record ResolvedScope(String kind, Object key, ProductTenancy product) {
}
