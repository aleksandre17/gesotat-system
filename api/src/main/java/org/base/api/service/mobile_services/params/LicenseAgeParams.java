package org.base.api.service.mobile_services.params;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameters for the license-age endpoint.
 */
@Setter
@Getter
public class LicenseAgeParams extends CommonParams {
    private final Boolean table;
    public LicenseAgeParams(Boolean table) {
        super(null, null, null, null, null);
        this.table = table;
    }
}
