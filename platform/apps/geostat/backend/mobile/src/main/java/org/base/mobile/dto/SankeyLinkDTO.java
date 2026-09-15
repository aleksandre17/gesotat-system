package org.base.mobile.dto;

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
