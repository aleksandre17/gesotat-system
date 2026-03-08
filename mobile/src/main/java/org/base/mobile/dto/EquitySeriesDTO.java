package org.base.mobile.dto;


import java.util.List;

public class EquitySeriesDTO {
    private final String name;
    private final List<Integer> data;

    public EquitySeriesDTO(String name, List<Integer> data) {
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
