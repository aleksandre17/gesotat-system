package org.base.api.security.tenancy.scope;

import java.util.Optional;

/**
 * Strategy that turns one family of route identities into the owning data product. The enforcement
 * point iterates the registered resolvers in {@link #order()} and takes the first that answers; a
 * scoped route that no resolver answers for is denied.
 */
public interface TenantScopeResolver {

    /** Stable name for audit lines and for the coverage test. */
    String name();

    /** Lower runs first. Identities that name the product most directly come first. */
    int order();

    Optional<ResolvedScope> resolve(TenantScopeRequest request);

    /** The identity names this resolver reads, so the coverage guard test can check routes statically. */
    java.util.Set<String> identities();
}
