package org.base.mobile.params;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

public abstract class CommonParams {
    @Getter
    @Setter
    @Pattern(regexp = "\\d{4}|null", message = "Year must be a 4-digit number or null")
    private Integer year;

    @Getter
    @Pattern(regexp = "[1-4]|99|null", message = "Quarter must be 1-4, 99, or null")
    private final String quarter;

    @Getter
    private final String brand;
    @Getter
    private final String yearOfProduction;

    @Getter
    @Pattern(regexp = "[A-Z]{2}|1|null", message = "Region must be a 2-letter code, 1, or null")
    private final String region;

    protected CommonParams(Integer year, String quarter, String brand, String yearOfProduction, String region) {
        this.year = year;
        this.quarter = convertQuarterToFloat(quarter);
        this.brand = brand;
        this.yearOfProduction = yearOfProduction;
        this.region = region;
    }

    public static String convertQuarterToFloat(String quarter) {
        if (quarter == null) {
            return null;
        }
        // Handle Roman numerals
        switch (quarter.trim().toUpperCase()) {
            case "I":
                return "1";
            case "II":
                return "2";
            case "III":
                return "3";
            case "IV":
                return "4";
            default:
                // Try parsing as a numeric string (e.g., '1', '2.0')
                try {
                    return String.valueOf(quarter.trim());
                } catch (NumberFormatException e) {
                    // Return null for invalid values
                    return null;
                }
        }
    }

}
