package org.base.mobile.dto;

public record SankeyDTO(
        String from,
        String to,
        long value,
        String id
) {
}
