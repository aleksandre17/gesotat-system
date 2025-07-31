package org.base.api.service.mobile_services.dto;

import java.util.List;

/**
 * DTO for license-age endpoint response.
 */
public class LicenseAgeDTO {
    private final List<String> categories;
    private final List<Series> series;

    public LicenseAgeDTO(List<String> categories, List<Series> series) {
        this.categories = categories;
        this.series = series;
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<Series> getSeries() {
        return series;
    }

    public static class Series {
        private final String name;
        private final List<Integer> data;

        public Series(String name, List<Integer> data) {
            this.name = name;
            this.data = data;
        }

        public String getName() {
            return name;
        }

        public List<Integer> getData() {
            return data;
        }
    }
}
