package org.base.api.service.dynamic;

import java.util.List;
import java.util.Map;

public record DynamicTableResponse(List<String> columns, List<Map<String, Object>> rows,
                                   int page, int limit) {
}
