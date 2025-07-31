package org.base.api.service.mobile_services.dto.text;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SelectorDTO {
    private String type;
    private String placeholder;
    private SelectorValue defaultValue;
    private String icon;
    private String setSelected;
    private String setTitle;
    private Boolean hasSearch;
    private List<SelectorValue> selectValues;

    public SelectorDTO(String type, String placeholder, SelectorValue defaultValue, List<SelectorValue> selectValues) {
        this.type = type;
        this.placeholder = placeholder;
        this.defaultValue = defaultValue;
        this.selectValues = selectValues;
    }

}

