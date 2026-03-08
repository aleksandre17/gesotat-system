package org.base.mobile.dto;

import java.util.List;

class TradeSeriesDTO {
    private String name;
    private List<Integer> data;

    public TradeSeriesDTO(String name, List<Integer> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Integer> getData() { return data; }
    public void setData(List<Integer> data) { this.data = data; }
}
