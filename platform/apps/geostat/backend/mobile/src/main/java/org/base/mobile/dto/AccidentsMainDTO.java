package org.base.mobile.dto;

import java.util.List;

/**
 * DTO for accidents-main endpoint response.
 */
public class AccidentsMainDTO {
    private final int pointStart;
    private final List<SeriesDTO> data;

    public AccidentsMainDTO(int pointStart, List<SeriesDTO> data) {
        this.pointStart = pointStart;
        this.data = data;
    }

    public int getPointStart() {
        return pointStart;
    }

    public List<SeriesDTO> getData() {
        return data;
    }

    public static class SeriesDTO {
        private final String name;
        private final List<Integer> data;

        public SeriesDTO(String name, List<Integer> data) {
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
