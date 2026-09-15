package org.base.mobile.params;

/**
 * Parameters for the accidents-main endpoint.
 */
public class AccidentsMainParams extends CommonParams {
    private final String accidents;

    public AccidentsMainParams(String region, String accidents) {
        super(null, null, null, null, region);
        this.accidents = accidents;
    }

    public String getAccidents() {
        return accidents;
    }
}
