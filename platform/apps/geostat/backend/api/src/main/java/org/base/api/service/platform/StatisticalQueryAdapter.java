package org.base.api.service.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

/** Provider-neutral port for published statistical observations. */
public interface StatisticalQueryAdapter {
    List<PublishedMetricPoint> points(JdbcTemplate dataPlane, long productId, long metricId);
}
