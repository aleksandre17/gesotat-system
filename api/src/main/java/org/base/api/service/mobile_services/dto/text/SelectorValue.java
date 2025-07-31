package org.base.api.service.mobile_services.dto.text;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectorValue {
    private Object name;
    private Object code;

    public SelectorValue(Object name, Object code) {
        this.name = name;
        this.code = code;
    }
}
