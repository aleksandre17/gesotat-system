package org.base.mobile.params;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Parameters for the trade endpoint.
 */
public class TradeParams extends CommonParams {
    @Pattern(regexp = "^(E|I)?$", message = "Invalid e_i value")
    private final String eI;
    @Pattern(regexp = "^(1|2)?$", message = "Invalid type value")
    private final String type;
    @Pattern(regexp = "^[0-9]*$", message = "Invalid fuel value")
    private final String fuel;
    @Pattern(regexp = "^[0-9]*$", message = "Invalid vehicle value")
    private final String vehicle;
    private final boolean currency;
    @Pattern(regexp = "^(1)?$", message = "Invalid selector value")
    private final String selector;
    @NotBlank(message = "Language name is required")
    private final String langName;

    public TradeParams(
            String eI,
            String type,
            String fuel,
            String vehicle,
            String currency,
            String selector,
            String langName) {
        super(null, null, null, null, null);
        this.eI = eI != null && !eI.isEmpty() ? eI : "E";
        this.type = type != null ? type : "";
        this.fuel = fuel != null && !fuel.isEmpty() ? fuel : "1";
        this.vehicle = vehicle != null && !vehicle.isEmpty() ? vehicle : "1";
        this.currency = "true".equalsIgnoreCase(currency);
        this.selector = selector != null && !selector.isEmpty() ? selector : "";
        this.langName = langName;
    }

    public String getEI() {
        return eI;
    }

    public String getType() {
        return type;
    }

    public String getFuel() {
        return fuel;
    }

    public String getVehicle() {
        return vehicle;
    }

    public boolean isCurrency() {
        return currency;
    }

    public String getSelector() {
        return selector;
    }

    public String getLangName() {
        return langName;
    }

    public String getAttribute() {
        return "1".equals(selector) ? "quantity" : (currency ? "gel1000" : "usd1000");
    }
}
