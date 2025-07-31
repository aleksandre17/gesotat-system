package org.base.api.service.mobile_services.dto;

import java.util.List;

/**
 * DTO for trade endpoint response.
 */
public record TradeDTO(
        int pointStart,
        List<CountryData> data
) {
    public record CountryData(
            String name,
            List<Long> data
    ) {
        public CountryData {
            //if (name == null) throw new IllegalArgumentException("Country name cannot be null");
            //if (data == null) throw new IllegalArgumentException("Country data cannot be null");
        }
    }
}


//public class TradeDTO {
//    private Integer pointStart;
//    private List<TradeSeriesDTO> data;
//
//    public TradeDTO(Integer pointStart, List<TradeSeriesDTO> data) {
//        this.pointStart = pointStart;
//        this.data = data;
//    }
//
//    public Integer getPointStart() { return pointStart; }
//    public void setPointStart(Integer pointStart) { this.pointStart = pointStart; }
//    public List<TradeSeriesDTO> getData() { return data; }
//    public void setData(List<TradeSeriesDTO> data) { this.data = data; }
//}