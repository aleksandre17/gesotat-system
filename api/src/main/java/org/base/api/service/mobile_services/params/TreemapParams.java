package org.base.api.service.mobile_services.params;

import jakarta.validation.constraints.NotBlank;

/**
 * Parameters for the treemap endpoint.
 */
public class TreemapParams extends CommonParams {
    @NotBlank(message = "Language translation for 'other' is required")
    private String otherTranslation;

    public TreemapParams(Integer year, String quarter, String otherTranslation) {
        super(year, quarter, null, null, null);
        this.otherTranslation = otherTranslation;
    }

    public String getOtherTranslation() {
        return otherTranslation;
    }

    public void setOtherTranslation(String otherTranslation) {
        this.otherTranslation = otherTranslation;
    }
}
