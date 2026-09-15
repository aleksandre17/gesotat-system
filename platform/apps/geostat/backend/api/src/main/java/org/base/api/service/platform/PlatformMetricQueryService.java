package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/** Serving read-model: a chart consumes published semantic observations, never a chart-specific copy. */
@Service
public class PlatformMetricQueryService {
    private final JdbcTemplate control;
    private final JdbcTemplate data;
    private final StatisticalQueryAdapter statistical;
    public PlatformMetricQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control, @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data) { this(control,data,new CanonicalStatisticalQueryAdapter()); }
    @Autowired
    public PlatformMetricQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control, @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data, StatisticalQueryAdapter statistical) { this.control=control; this.data=data; this.statistical=java.util.Objects.requireNonNull(statistical,"statistical adapter"); }

    public List<PublishedMetricPoint> points(long productId, String metricCode) {
        if(productId<=0 || metricCode==null || metricCode.isBlank()) throw new IllegalArgumentException("productId and metricCode are required");
        Long metric=control.query("SELECT m.metric_id FROM platform.metric m JOIN platform.dataset d ON d.dataset_id=m.source_dataset_id WHERE m.metric_code=? AND d.product_id=? AND m.status='PUBLISHED'",rs->rs.next()?rs.getLong(1):null,metricCode,productId);
        if(metric==null) throw new IllegalArgumentException("Published metric not found for product");
        return statistical.points(data, productId, metric);
    }
}
