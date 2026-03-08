package org.base.mobile.dto;

/**
 * DTO for colors endpoint response item.
 */
public record ColorsDTO(
        String name,
        int value,
        String hex
) {
}
