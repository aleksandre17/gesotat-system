package org.base.api.service.mobile_services.dto;

import java.util.List;

public class CompareLineDTO {
    private Integer pointStart;
    private List<CompareSeriesDTO> data;

    public CompareLineDTO(Integer pointStart, List<CompareSeriesDTO> data) {
        this.pointStart = pointStart;
        this.data = data;
    }

    public Integer getPointStart() { return pointStart; }
    public void setPointStart(Integer pointStart) { this.pointStart = pointStart; }
    public List<CompareSeriesDTO> getData() { return data; }
    public void setData(List<CompareSeriesDTO> data) { this.data = data; }
}
