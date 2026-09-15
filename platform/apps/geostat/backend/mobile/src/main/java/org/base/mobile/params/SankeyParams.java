package org.base.mobile.params;

import jakarta.validation.constraints.Pattern;

public class SankeyParams extends CommonParams {
    @Pattern(regexp = "^(fuel|color|body|engine|)?$", message = "Invalid filter value")
    private final String filter;
    private final String langName;

    public SankeyParams(Integer year, String quarter, String filter, String langName) {
        super(year, quarter, null, null, null);
        this.filter = filter != null ? filter : "";
        this.langName = langName;
    }

    public String getFilter() {
        return filter;
    }

    public String getLangName() {
        return langName;
    }
}
