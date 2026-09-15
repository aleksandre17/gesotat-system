package org.base.mobile.params;

import jakarta.validation.constraints.Pattern;

/**
 * Parameters for the fuel-av-price endpoint.
 */
public class FuelAvPriceParams {
    @Pattern(regexp = "\\d+|null", message = "Fuel must be a number or null")
    private final String fuel;

    private final Boolean currency;

    public FuelAvPriceParams(String fuel, Boolean currency) {
        this.fuel = fuel;
        this.currency = currency;
    }

    public String getFuel() {
        return fuel;
    }

    public Boolean getCurrency() {
        return currency;
    }
}
