package org.base.api.service.mobile_services.params.text;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LicensesParams {
    private Integer year;
    private String tableName;
    private String lang;
}
