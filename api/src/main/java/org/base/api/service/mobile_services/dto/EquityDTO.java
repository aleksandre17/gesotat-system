package org.base.api.service.mobile_services.dto;

import java.util.List;

/**
 * DTO for equity endpoint response.
 */
public class EquityDTO {
    private final int pointStart;
    private final List<EquitySeriesDTO> data;

    public EquityDTO(int pointStart, List<EquitySeriesDTO> data) {
        this.pointStart = pointStart;
        this.data = data;
    }

    public int getPointStart() {
        return pointStart;
    }

    public List<EquitySeriesDTO> getData() {
        return data;
    }
}

