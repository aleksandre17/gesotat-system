package org.base.api.service.mobile_services.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class FilterDataDTO {
    private List<List<Object>> data;
    private List<String> hexCodes; // Only for color filter

    public FilterDataDTO() {
        this.data = new ArrayList<>();
    }

    public void addData(String itemName, Long totalQuantity) {
        data.add(Arrays.asList(itemName, totalQuantity.intValue()));
    }

    public void addHexCode(String hexCode) {
        if (hexCodes == null) {
            hexCodes = new ArrayList<>();
        }
        hexCodes.add(hexCode);
    }

}
