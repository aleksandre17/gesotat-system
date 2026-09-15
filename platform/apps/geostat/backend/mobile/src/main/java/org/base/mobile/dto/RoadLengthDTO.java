package org.base.mobile.dto;

/**
 * DTO for road-length endpoint response.
 */
public class RoadLengthDTO {
    private final String name;
    private final double value;

    public RoadLengthDTO(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }
}
