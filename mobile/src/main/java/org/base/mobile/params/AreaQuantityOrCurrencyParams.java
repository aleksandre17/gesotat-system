package org.base.mobile.params;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

/**
 * Parameters for the area-currency endpoint.
 */
public class AreaQuantityOrCurrencyParams extends CommonParams {
    @Getter
    @Pattern(regexp = "^(E|I)?$", message = "Invalid e_i value")
    private final String eI;
    @Getter
    @Pattern(regexp = "^(1|2)?$", message = "Invalid type value")
    private final String type;
    @Getter
    @Pattern(regexp = "^[0-9]*$", message = "Invalid fuel value")
    private final String fuel;
    @Getter
    @Pattern(regexp = "^[0-9]*$", message = "Invalid vehicle value")
    private final String vehicle;
    @Getter
    private final boolean currency;
    @Getter
    @NotBlank(message = "Language name is required")
    private final String langName;
    @Getter
    private final String selected;

    public AreaQuantityOrCurrencyParams(
            String eI,
            String type,
            String fuel,
            String vehicle,
            Boolean currency,
            String langName,
            String selected) {
        super(null, null, null, null, null);
        this.eI = eI != null && !eI.isEmpty() ? eI : "E";
        this.type = type != null ? type : "";
        this.fuel = fuel != null && !fuel.isEmpty() ? fuel : "1";
        this.vehicle = vehicle != null && !vehicle.isEmpty() ? vehicle : "1";
        this.currency = currency;
        this.langName = langName;
        this.selected = selected;
    }

    public String getCurrencyName() {
        return currency ? "gel1000" : "usd1000";
    }

    public String getAttribute() {
        return currency ? "gel1000" : "usd1000"; //"1".equals(selector) ? "quantity" : (currency ? "gel1000" : "usd1000");
    }
}
