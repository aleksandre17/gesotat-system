package org.base.api.service.mobile_services.dto;

import java.util.List;

public record RaceDTO(
        String name,
        int value
) {
    public record YearData(List<RaceDTO> brands) {}
}
