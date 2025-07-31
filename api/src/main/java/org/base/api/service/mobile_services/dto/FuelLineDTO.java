package org.base.api.service.mobile_services.dto;

import java.util.List;

/**
 * DTO for fuel-line endpoint response.
 */
public class FuelLineDTO {
    private final int pointStart;
    private final List<SeriesDTO> data;

    public FuelLineDTO(int pointStart, List<SeriesDTO> data) {
        this.pointStart = pointStart;
        this.data = data;
    }

    public int getPointStart() {
        return pointStart;
    }

    public List<SeriesDTO> getData() {
        return data;
    }

    /**
     * Nested DTO for series data.
     */
    public static class SeriesDTO {
        private final String name;
        private final List<Double> data;

        public SeriesDTO(String name, List<Double> data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public List<Double> getData() {
            return data;
        }
    }
}
