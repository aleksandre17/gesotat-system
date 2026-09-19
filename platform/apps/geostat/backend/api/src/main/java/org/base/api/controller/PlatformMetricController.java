package org.base.api.controller;

import org.base.api.service.platform.PlatformMetricQueryService;
import org.base.api.service.platform.PublishedMetricPoint;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.base.api.security.tenancy.TenantScoped;

@TenantScoped
@Api
@RestController
@RequestMapping("/platform/metrics")
public class PlatformMetricController {
    private final PlatformMetricQueryService metrics;
    public PlatformMetricController(PlatformMetricQueryService metrics) { this.metrics=metrics; }
    @GetMapping("/{metricCode}/points") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<List<PublishedMetricPoint>> points(@PathVariable String metricCode,@RequestParam long productId){ return ResponseEntity.ok(metrics.points(productId,metricCode)); }
}
