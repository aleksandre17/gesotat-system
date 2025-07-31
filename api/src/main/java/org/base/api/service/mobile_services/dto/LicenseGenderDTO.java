package org.base.api.service.mobile_services.dto;

import java.util.List;

/**
 * DTO for license-gender endpoint response.
 */
public class LicenseGenderDTO {
    private final String name;
    private final List<DataPoint> data;

    public LicenseGenderDTO(String name, List<DataPoint> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public List<DataPoint> getData() {
        return data;
    }

    public static class DataPoint {
        private final String name;
        private final int value;

        public DataPoint(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
