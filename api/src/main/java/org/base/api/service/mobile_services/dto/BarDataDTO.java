package org.base.api.service.mobile_services.dto;

public class BarDataDTO {
    private String name;
    private int value;

    public BarDataDTO(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
