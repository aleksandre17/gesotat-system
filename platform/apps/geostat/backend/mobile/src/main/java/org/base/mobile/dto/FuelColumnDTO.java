package org.base.mobile.dto;

import java.util.List;

/**
 * DTO for fuel-column endpoint response.
 */
public class FuelColumnDTO {
    private final List<Integer> categories;
    private final List<SeriesDTO> series;

    public FuelColumnDTO(List<Integer> categories, List<SeriesDTO> series) {
        this.categories = categories;
        this.series = series;
    }

    public List<Integer> getCategories() {
        return categories;
    }

    public List<SeriesDTO> getSeries() {
        return series;
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