package org.base.api.service.mobile_services.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for license-sankey endpoint response.
 */

public record LicenseSankeyDTO(String from, String to, int value, String id) {
}
