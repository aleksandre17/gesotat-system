package org.base.api.service.mobile_services.params;

/**
 * Parameters for the road-length endpoint.
 */
public class RoadLengthParams extends CommonParams {
    public RoadLengthParams(Integer year, String region) {
        super(year, null, null, null, region);
    }
}
