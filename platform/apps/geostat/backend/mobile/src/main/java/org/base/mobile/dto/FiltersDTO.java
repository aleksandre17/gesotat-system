package org.base.mobile.dto;

import java.util.List;

public record FiltersDTO(
        List<List<Object>> data,
        List<String> hex_codes
) {
}
