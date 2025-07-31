package org.base.api.model.response.mobile_portal;

import lombok.Data;

@Data
public class MobileTopFiveDTO {
    private String name;
    private Integer value;

    public MobileTopFiveDTO(String brand, String model, Long value) {
        this.name = brand + " " + model;
        this.value = value.intValue();
    }
}
