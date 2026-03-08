package org.base.mobile.dto;

import java.util.Map;

public record TreemapDTO(
        Map<String, Map<String, Integer>> results
) {
}
