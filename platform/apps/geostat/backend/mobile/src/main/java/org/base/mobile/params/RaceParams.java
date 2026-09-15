package org.base.mobile.params;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameters for the race endpoint.
 */
@Getter
@Setter
public class RaceParams extends CommonParams {
    String tableName;
    public RaceParams() {
        super(null, null, null, null, null);
    }
}
