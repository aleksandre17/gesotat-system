package org.base.api.service.platform;

/** Canonical, contract-declared data-family identity used by serving dispatch. */
public enum PageFamily {
    ENTITY, STATISTICAL, REFERENCE, RAW, GEO, RELATION, ROOT, UNKNOWN;

    public static PageFamily fromNodeKind(Object nodeKind) {
        if (nodeKind == null) return UNKNOWN;
        String value = String.valueOf(nodeKind).trim().toUpperCase(java.util.Locale.ROOT);
        if (value.contains("STATISTICAL")) return STATISTICAL;
        if (value.contains("REFERENCE")) return REFERENCE;
        if (value.contains("ENTITY")) return ENTITY;
        if (value.contains("RAW")) return RAW;
        if (value.contains("GEO")) return GEO;
        if (value.contains("RELATION")) return RELATION;
        if (value.contains("ROOT")) return ROOT;
        return UNKNOWN;
    }

    /** Resolve the canonical family declared by the dataset contract. */
    public static PageFamily fromDataFamily(Object dataFamily) {
        if (dataFamily == null) return UNKNOWN;
        String value = String.valueOf(dataFamily).trim().toUpperCase(java.util.Locale.ROOT);
        return switch (value) {
            case "ENTITY" -> ENTITY;
            case "STATISTICAL", "STATISTICAL_WIDE_JSON" -> STATISTICAL;
            case "REFERENCE", "CLASSIFICATION" -> REFERENCE;
            case "RAW" -> RAW;
            case "GEO", "GEOSPATIAL" -> GEO;
            case "RELATION" -> RELATION;
            case "ROOT" -> ROOT;
            default -> UNKNOWN;
        };
    }
}
