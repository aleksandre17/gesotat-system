package org.base.mobile.dto;

import java.util.List;

public record RaceDTO(
        String name,
        int value
) {
    public record YearData(List<RaceDTO> brands) {}
}
