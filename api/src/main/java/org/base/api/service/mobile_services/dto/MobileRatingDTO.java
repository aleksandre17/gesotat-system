package org.base.api.service.mobile_services.dto;

import lombok.Data;

@Data
public class MobileRatingDTO {
    private String name;
    private Integer value;

    public MobileRatingDTO(String brand, String model, Long value) {
        this.name = brand + " " + model;
        this.value = value.intValue();
    }

}

