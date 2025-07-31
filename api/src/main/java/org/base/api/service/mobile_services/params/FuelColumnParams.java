package org.base.api.service.mobile_services.params;

import jakarta.validation.constraints.Pattern;

/**
 * Parameters for the fuel-column endpoint.
 */
public class FuelColumnParams {
    @Pattern(regexp = "[IE]|null", message = "e_i must be 'I' or 'E' or null")
    private final String e_i;

    @Pattern(regexp = "\\d+|null", message = "Fuel must be a number or null")
    private final String fuel;

    private final Boolean currency;

    public FuelColumnParams(String e_i, String fuel, Boolean currency) {
        this.e_i = e_i;
        this.fuel = fuel;
        this.currency = currency;
    }

    public String getE_i() {
        return e_i;
    }

    public String getFuel() {
        return fuel;
    }

    public Boolean getCurrency() {
        return currency;
    }
}
