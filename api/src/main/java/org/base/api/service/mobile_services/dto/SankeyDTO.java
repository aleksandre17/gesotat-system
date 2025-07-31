package org.base.api.service.mobile_services.dto;

public record SankeyDTO(
        String from,
        String to,
        long value,
        String id
) {
}
