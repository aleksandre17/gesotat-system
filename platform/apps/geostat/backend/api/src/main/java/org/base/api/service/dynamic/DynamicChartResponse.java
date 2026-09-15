package org.base.api.service.dynamic;

import java.util.List;
import java.util.Map;

public record DynamicChartResponse(String chartCode, String chartType, List<Map<String, Object>> rows) {
}
