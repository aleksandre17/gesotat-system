package org.base.api.security.tenancy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This controller serves or mutates data that belongs to a data product. Every handler method must
 * carry a route identity a {@link org.base.api.security.tenancy.scope.TenantScopeResolver} can turn
 * into a product, or declare a {@link TenantScopeExemption} with a reason. A scoped route for which
 * no resolver yields a product is DENIED.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TenantScoped {
}
