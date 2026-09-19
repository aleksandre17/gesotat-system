package org.base.api.security.tenancy;

/** How a controller class is declared for tenancy. Undeclared is a defect, not a mode. */
public enum TenantScopeClassification {
    /** Serves or mutates product data; every handler must resolve to a product or declare an exemption. */
    SCOPED,
    /** Carries no product identity; the annotation records the honest reason. */
    NEUTRAL,
    /** Neither annotation is present. Inside the governed controller package this is denied. */
    UNDECLARED
}
