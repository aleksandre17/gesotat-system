package org.base.mobile.dto;

import java.util.List;

public class RegionalBarDTO {
    private String name;
    private List<BarDataDTO> data;

    public RegionalBarDTO(String name, List<BarDataDTO> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<BarDataDTO> getData() { return data; }
    public void setData(List<BarDataDTO> data) { this.data = data; }
}
