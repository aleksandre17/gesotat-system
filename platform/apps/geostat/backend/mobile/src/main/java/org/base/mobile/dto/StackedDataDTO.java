package org.base.mobile.dto;

import java.util.List;

public class StackedDataDTO {
    private Integer pointStart;
    private List<StackedSeriesDTO> data;

    public StackedDataDTO(Integer pointStart, List<StackedSeriesDTO> data) {
        this.pointStart = pointStart;
        this.data = data;
    }

    public Integer getPointStart() { return pointStart; }
    public void setPointStart(Integer pointStart) { this.pointStart = pointStart; }
    public List<StackedSeriesDTO> getData() { return data; }
    public void setData(List<StackedSeriesDTO> data) { this.data = data; }
}
