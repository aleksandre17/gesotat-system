package org.base.api.service.mobile_services.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ColorDTO {
    private String name;
    private Integer value;
    private String hex;

}
