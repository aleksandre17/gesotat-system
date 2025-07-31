package org.base.api.service.mobile_services.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SankeyLinkDTO {
    private String from;
    private String to;
    private Long value;
    private String id;

}
