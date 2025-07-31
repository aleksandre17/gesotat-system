package org.base.api.service.mobile_services.params;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LicenseGenderParams extends CommonParams {
    private final Boolean table;
    public LicenseGenderParams(Boolean table) {
        super(null, null, null, null, null);
        this.table = table;
    }
}
