package org.base.api.service.platform;

import java.util.List;

public record PublishedVisualization(String code, String chartType, String configJson, java.util.Map<String,Object> metadata, List<PublishedMetricPoint> points) {
}
