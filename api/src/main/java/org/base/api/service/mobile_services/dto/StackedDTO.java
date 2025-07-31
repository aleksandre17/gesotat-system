package org.base.api.service.mobile_services.dto;

import java.util.List;

/**
 * DTO for stacked endpoint response.
 */
public record StackedDTO(
        int pointStart,
        List<CategoryData> data
) {
    public record CategoryData(
            String name,
            String hex,
            List<Long> data
    ) {}
}
