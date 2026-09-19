package org.base.api.security.tenancy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A handler of a {@link TenantScoped} controller whose route carries no resolvable product identity.
 * Without this annotation such a handler is denied; with it, the named {@link Kind} says how the
 * product is established instead, and the reason records why.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TenantScopeExemption {

    Kind value();

    /** Why the route cannot carry the identity itself. Must not be blank. */
    String reason();

    enum Kind {
        /**
         * The route addresses the platform's approved default contract (the controller itself falls
         * back to it). The interceptor resolves that contract and enforces against its product, so the
         * route is still fail-closed.
         */
        DEFAULT_CONTRACT,
        /**
         * The identity arrives in a request body or an uploaded file, which an interceptor cannot read
         * without consuming it. Enforcement happens in the service at the single point where that
         * identity is resolved. The reason must name that point.
         */
        SERVICE_ENFORCED,
        /**
         * The object belongs to the caller rather than to a product, and the service filters by owner.
         * The reason must name the owner check.
         */
        CALLER_OWNED
    }
}
