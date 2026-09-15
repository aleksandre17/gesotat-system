package org.base.mobile.params;

/**
 * Parameters for the accidents-gender endpoint.
 */
public class AccidentsGenderParams extends CommonParams {
    private final String accidents;

    public AccidentsGenderParams(String accidents) {
        super(null, null, null, null, null);
        this.accidents = accidents;
    }

    public String getAccidents() {
        return accidents;
    }
}
