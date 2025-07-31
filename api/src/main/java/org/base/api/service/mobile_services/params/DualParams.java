package org.base.api.service.mobile_services.params;

import jakarta.validation.constraints.Pattern;

/**
 * Parameters for the dual endpoint.
 */
public class DualParams extends CommonParams {
    @Pattern(regexp = "^[0-9]*$", message = "Invalid v_type value")
    private final String vType;
    private final String langName;

    public DualParams(String vType, String langName) {
        super(null, null, null, null, null);
        this.vType = vType != null ? vType : "0";
        this.langName = langName;
    }

    public String getVType() {
        return vType;
    }

    public String getLangName() {
        return langName;
    }
}
