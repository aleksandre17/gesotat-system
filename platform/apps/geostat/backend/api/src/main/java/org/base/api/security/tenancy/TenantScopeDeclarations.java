package org.base.api.security.tenancy;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * Reads the tenancy declaration of a controller class. It is the one place that knows which package
 * is the governed controller surface, so the runtime enforcement point and the build-time coverage
 * guard test can never disagree about it.
 */
public final class TenantScopeDeclarations {
    /** Controllers of the governed API. A class here must declare its tenancy scope. */
    public static final String GOVERNED_CONTROLLER_PACKAGE = "org.base.api.controller";

    private TenantScopeDeclarations() {}

    public static TenantScopeClassification classify(Class<?> controller) {
        if (controller == null) return TenantScopeClassification.UNDECLARED;
        boolean scoped = AnnotatedElementUtils.hasAnnotation(controller, TenantScoped.class);
        boolean neutral = AnnotatedElementUtils.hasAnnotation(controller, TenantNeutral.class);
        if (scoped && neutral) throw new IllegalStateException("Controller declares both tenancy scopes: " + controller.getName());
        if (scoped) return TenantScopeClassification.SCOPED;
        return neutral ? TenantScopeClassification.NEUTRAL : TenantScopeClassification.UNDECLARED;
    }

    public static boolean isGovernedController(Class<?> controller) {
        return controller != null && controller.getName().startsWith(GOVERNED_CONTROLLER_PACKAGE + ".");
    }
}
