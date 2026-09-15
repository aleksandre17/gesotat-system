package org.base.core.model;

import java.util.Map;

public enum ClassificationTableType implements ClassificationTable {
    FUEL("CL.cl_fuel", "fuel", "cl_fuel", false),
    ROAD("CL.cl_road", "road", "cl_road", false),
    COLOR("CL.cl_color", "color", "cl_color", true),
    BODY("CL.cl_body", "body", "cl_body", false),
    ENGINE("CL.cl_engine", "engine", "cl_engine", false),
    VEHICLE_AGE("CL.cl_vehicle_age", "age", "cl_vehicle_age", false),
    LICENSE_AGE("CL.cl_licenses_age", "licenses_age", "cl_licenses_age_cl", false),
    COUNTRY("[CL].[cl_country]", "country", "cl_country", false),
    GENDER("[CL].[cl_gender]", "gender", "cl_gender", false),
    AGE("[CL].[cl_age]", "age", "cl_age", false);
    //BRAND("CL.cl_brand", "brand", "cl_cl_brand", false);

    private final String tableName;
    private final String keyColumn;
    private final String alias;
    private final boolean hasHexCode;

    ClassificationTableType(String tableName, String keyColumn, String alias, boolean hasHexCode) {
        this.tableName = tableName;
        this.keyColumn = keyColumn;
        this.alias = alias;
        this.hasHexCode = hasHexCode;
    }

    @Override
    public String getTableName() { return tableName; }
    @Override
    public String getKeyColumn() { return keyColumn; }
    @Override
    public String getAlias() { return alias; }
    @Override
    public boolean hasHexCode() { return hasHexCode; }

    public static ClassificationTableType fromFilter(String filter) {
        try {
            return valueOf(filter.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
