package org.base.api.service.mobile_services.params;

import jakarta.validation.constraints.Pattern;

/**
 * Parameters for the filters endpoint.
 */
public class FiltersParams extends CommonParams {
    @Pattern(regexp = "^(fuel|color|body|engine|)?$", message = "Invalid filter value")
    private final String filter;
    private final String transport;
    private final String langName;

    public FiltersParams(Integer year, String quarter, String filter, String transport, String langName) {
        super(year, quarter, null, null, null);
        this.filter = filter != null ? filter : "";
        this.transport = transport;
        this.langName = langName;
    }

    public String getFilter() {
        return filter;
    }

    public String getTransport() {
        return transport;
    }

    public String getLangName() {
        return langName;
    }
}
