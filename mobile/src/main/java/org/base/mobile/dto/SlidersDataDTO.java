package org.base.mobile.dto;

import java.util.List;

/**
 * DTO for sliders-data endpoint response.
 */
public class SlidersDataDTO {
    private final String title;
    private final List<DataItem> data;

    public SlidersDataDTO(String title, List<DataItem> data) {
        this.title = title;
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public List<DataItem> getData() {
        return data;
    }

    public static class DataItem {
        private final String name;
        private final String unit;
        private final String period;
        private final float value;

        public DataItem(String name, String unit, String period, float value) {
            this.name = name;
            this.unit = unit;
            this.period = period;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public String getPeriod() {
            return period;
        }

        public float getValue() {
            return value;
        }
    }
}
