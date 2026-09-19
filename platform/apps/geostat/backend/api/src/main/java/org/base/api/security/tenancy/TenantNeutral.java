package org.base.api.security.tenancy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This controller carries no data-product identity, so tenant scoping does not apply to it. The
 * reason is mandatory and is part of the security record: it must state honestly why the surface is
 * outside tenancy, never merely that scoping was inconvenient.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TenantNeutral {

    /** Why this surface has no product identity. Must not be blank. */
    String reason();

    /**
     * True for a legacy surface that does serve an organisation's data but carries no product
     * identity at all, so it cannot be tenant-scoped without a larger change. Such a surface is
     * recorded, not endorsed: it is a candidate for retirement or for a dedicated authority.
     */
    boolean legacy() default false;
}
