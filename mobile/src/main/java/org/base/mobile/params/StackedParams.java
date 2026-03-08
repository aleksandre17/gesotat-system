package org.base.mobile.params;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Parameters for the stacked endpoint.
 */
public class StackedParams extends CommonParams {
    @Pattern(regexp = "^(fuel|color|body|engine|year_of_production|model|brand)?$", message = "Invalid filter value")
    private final String filter;
    @NotBlank(message = "Language name is required")
    private final String langName;
    @NotBlank(message = "Other translation is required")
    private final String otherTranslation;

    public StackedParams(String filter, String langName, String otherTranslation) {
        super(null, null, null, null, null);
        this.filter = filter != null && !filter.isEmpty() ? filter : "brand";
        this.langName = langName;
        this.otherTranslation = otherTranslation;
    }

    public String getFilter() {
        return filter;
    }

    public String getLangName() {
        return langName;
    }

    public String getOtherTranslation() {
        return otherTranslation;
    }
}
