package org.base.api.service.platform;

import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.List;

/**
 * Configurable relational adapter. Physical identifiers are provider metadata,
 * validated once at construction; callers depend only on the statistical port.
 */
public final class RelationalStatisticalQueryAdapter implements StatisticalQueryAdapter {
    private final String snapshotTable, memberTable, seriesTable, observationTable;

    public RelationalStatisticalQueryAdapter(String snapshotTable, String memberTable,
                                             String seriesTable, String observationTable) {
        this.snapshotTable = identifier(snapshotTable); this.memberTable = identifier(memberTable);
        this.seriesTable = identifier(seriesTable); this.observationTable = identifier(observationTable);
    }

    @Override public List<PublishedMetricPoint> points(JdbcTemplate data, long productId, long metricId) {
        String sql = "SELECT o.period_start,SUM(o.numeric_value) AS value FROM " + snapshotTable + " p "
                + "JOIN " + memberTable + " sm ON sm.snapshot_id=p.snapshot_id "
                + "JOIN " + seriesTable + " s ON s.dataset_snapshot_id=sm.dataset_snapshot_id "
                + "JOIN " + observationTable + " o ON o.series_id=s.series_id "
                + "WHERE p.product_id=? AND p.status='PUBLISHED' AND s.metric_id=? AND o.is_current=1 "
                + "GROUP BY o.period_start ORDER BY o.period_start";
        return data.query(sql, (rs, row) -> new PublishedMetricPoint(
                rs.getObject(1, LocalDate.class), rs.getBigDecimal(2)), productId, metricId);
    }

    private static String identifier(String value) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_.]*"))
            throw new IllegalArgumentException("Unapproved statistical table identifier");
        return value;
    }
}
