package org.base.api.service.mobile_services.params;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/**
 * Parameters for the fuel-quantity endpoint.
 */
public class FuelQuantityParams {
    @Getter
    @Pattern(regexp = "[IE]|null", message = "e_i must be 'I' or 'E' or null")
    private final String e_i;

    @Getter
    @Pattern(regexp = "\\d+|null", message = "Fuel must be a number or null")
    private final String fuel;

    @Getter
    @Pattern(regexp = "[12]|null", message = "anualOrMonthly must be '1' or '2' or null")
    private final Boolean anualOrMonthly;

    public FuelQuantityParams(String e_i, String fuel, Boolean anualOrMonthly) {
        this.e_i = e_i;
        this.fuel = fuel;
        this.anualOrMonthly = anualOrMonthly;
    }
}
