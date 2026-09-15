package org.base.mobile.dto;

public class VehicleDualDTO {
    private Double data1;
    private Long data2;
    private Integer name;

    public VehicleDualDTO(Double data1, Long data2, Integer name) {
        this.data1 = data1;
        this.data2 = data2;
        this.name = name;
    }

    public Double getData1() { return data1; }
    public void setData1(Double data1) { this.data1 = data1; }
    public Long getData2() { return data2; }
    public void setData2(Long data2) { this.data2 = data2; }
    public Integer getName() { return name; }
    public void setName(Integer name) { this.name = name; }
}
