package org.base.mobile.params;

/**
 * Parameters for the regional-quantity endpoint.
 */
public class RegionalQuantityParams extends CommonParams {
    public RegionalQuantityParams(Integer year, String quarter, String brand, String yearOfProduction, String region) {
        super(year, quarter, brand, yearOfProduction, region);
    }
}
