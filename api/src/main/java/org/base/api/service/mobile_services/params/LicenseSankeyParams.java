package org.base.api.service.mobile_services.params;

import lombok.Getter;

public class LicenseSankeyParams extends CommonParams {
    @Getter
    public Boolean table;
    public LicenseSankeyParams(Integer year, Boolean table) {
        super(year, null, null, null, null);
        this.table = table;
    }
}
