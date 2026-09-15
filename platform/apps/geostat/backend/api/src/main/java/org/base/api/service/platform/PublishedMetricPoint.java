package org.base.api.service.platform;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PublishedMetricPoint(LocalDate periodStart, BigDecimal value) {
}
