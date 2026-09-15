package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Resolves a published chart definition to its published semantic metric data. */
@Service
public class PlatformVisualizationQueryService {
    private final JdbcTemplate control;
    private final PlatformMetricQueryService metrics;
    private final PlatformMetadataService metadata;
    public PlatformVisualizationQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control, PlatformMetricQueryService metrics, PlatformMetadataService metadata) { this.control=control; this.metrics=metrics; this.metadata=metadata; }

    public PublishedVisualization read(long productId,String code) {
        var definition=control.query("SELECT v.visualization_code,v.chart_type,v.config_json,m.metric_code FROM platform.visualization_definition v JOIN platform.metric m ON m.metric_id=v.metric_id WHERE v.product_id=? AND v.visualization_code=? AND v.status='PUBLISHED'",rs->rs.next()?new String[]{rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4)}:null,productId,code);
        if(definition==null)throw new IllegalArgumentException("Published visualization not found for product");
        Map<String,Object> governed=metadata.subject("VISUALIZATION",definition[0]);
        if(governed.isEmpty()) governed=metadata.fromConfig(definition[2]);
        return new PublishedVisualization(definition[0],definition[1],definition[2],governed,metrics.points(productId,definition[3]));
    }
}
