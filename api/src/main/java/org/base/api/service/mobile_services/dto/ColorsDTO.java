package org.base.api.service.mobile_services.dto;

/**
 * DTO for colors endpoint response item.
 */
public record ColorsDTO(
        String name,
        int value,
        String hex
) {
}
