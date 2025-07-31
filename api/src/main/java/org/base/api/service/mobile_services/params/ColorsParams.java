package org.base.api.service.mobile_services.params;

import jakarta.validation.constraints.NotBlank;

/**
 * Parameters for the colors endpoint.
 */
public class ColorsParams extends CommonParams {
    @NotBlank(message = "Language name is required")
    private final String langName;
    @NotBlank(message = "Other translation is required")
    private final String otherTranslation;

    public ColorsParams(Integer year, String quarter, String langName, String otherTranslation) {
        super(year, quarter, null, null, null);
        this.langName = langName;
        this.otherTranslation = otherTranslation;
    }

    public String getLangName() {
        return langName;
    }

    public String getOtherTranslation() {
        return otherTranslation;
    }
}
