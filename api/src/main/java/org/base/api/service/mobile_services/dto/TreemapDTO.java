package org.base.api.service.mobile_services.dto;

import java.util.Map;

public record TreemapDTO(
        Map<String, Map<String, Integer>> results
) {
}
