package org.base.mobile.dto;

import java.util.List;

public class StackedSeriesDTO {
    private String name;
    private String hex;
    private List<Long> data;

    public StackedSeriesDTO(String name, String hex, List<Long> data) {
        this.name = name;
        this.hex = hex;
        this.data = data;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHex() { return hex; }
    public void setHex(String hex) { this.hex = hex; }
    public List<Long> getData() { return data; }
    public void setData(List<Long> data) { this.data = data; }
}
