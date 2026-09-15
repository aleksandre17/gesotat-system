package org.base.api.service.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;

/** Canonical SQL Server adapter; table names are isolated behind the statistical port. */
@Component
public final class CanonicalStatisticalQueryAdapter implements StatisticalQueryAdapter {
    @Override
    public List<PublishedMetricPoint> points(JdbcTemplate data, long productId, long metricId) {
        return data.query("SELECT o.period_start,SUM(o.numeric_value) AS value FROM publication.snapshot p " +
                        "JOIN publication.snapshot_member sm ON sm.snapshot_id=p.snapshot_id " +
                        "JOIN [statistics].series s ON s.dataset_snapshot_id=sm.dataset_snapshot_id " +
                        "JOIN [statistics].observation o ON o.series_id=s.series_id " +
                        "WHERE p.product_id=? AND p.status='PUBLISHED' AND s.metric_id=? AND o.is_current=1 " +
                        "GROUP BY o.period_start ORDER BY o.period_start", (rs,row)->new PublishedMetricPoint(rs.getObject(1,java.time.LocalDate.class),rs.getBigDecimal(2)),productId,metricId);
    }
}
