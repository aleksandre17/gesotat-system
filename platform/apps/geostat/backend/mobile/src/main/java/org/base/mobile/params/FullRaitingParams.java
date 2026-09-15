package org.base.mobile.params;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

/**
 * Parameters for the full-raiting endpoint.
 */
public class FullRaitingParams extends CommonParams {
    private final String transport;
    @Pattern(regexp = "^(ascQuantity|ascModel|descModel|descQuantity)?$", message = "Invalid sort value")
    private final String sort;
    private final String search;
    @Min(value = 1, message = "Page must be at least 1")
    private final Integer page;

    public FullRaitingParams(Integer year, String transport, String sort, String search, Integer page) {
        super(year, null, null, null, null);
        this.transport = transport;
        this.sort = sort != null ? sort : "";
        this.search = search;
        this.page = page != null ? page : 1;
    }

    public String getTransport() {
        return transport;
    }

    public String getSort() {
        return sort;
    }

    public String getSearch() {
        return search;
    }

    public Integer getPage() {
        return page;
    }
}
