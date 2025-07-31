package org.base.api.service.mobile_services.params;

/**
 * Parameters for the sliders-data endpoint.
 */
public class SlidersDataParams extends CommonParams {
    private final String period;
    private final String title;

    public SlidersDataParams(String langName, String period, String title) {
        super(null, null, null, null, langName);
        this.period = period;
        this.title = title;
    }

    public String getPeriod() {
        return period;
    }

    public String getTitle() {
        return title;
    }
}
