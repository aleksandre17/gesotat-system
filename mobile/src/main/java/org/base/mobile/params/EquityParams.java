package org.base.mobile.params;

/**
 * Parameters for the equity endpoint.
 */
public class EquityParams extends CommonParams {
    public EquityParams(Integer year, String quarter, String brand, String yearOfProduction, String region) {
        super(year, quarter, brand, yearOfProduction, region);
    }
}
