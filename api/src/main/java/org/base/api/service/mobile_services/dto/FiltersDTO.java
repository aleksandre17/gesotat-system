package org.base.api.service.mobile_services.dto;

import java.util.List;

public record FiltersDTO(
        List<List<Object>> data,
        List<String> hex_codes
) {
}
