package org.base.mobile.dto;

import java.util.List;

/**
 * DTO for fuel-quantity endpoint response.
 */
public class FuelQuantityDTO {
    private final String name;
    private final int pointStart;
    private final List<Double> data;

    public FuelQuantityDTO(String name, int pointStart, List<Double> data) {
        this.name = name;
        this.pointStart = pointStart;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public int getPointStart() {
        return pointStart;
    }

    public List<Double> getData() {
        return data;
    }
}
