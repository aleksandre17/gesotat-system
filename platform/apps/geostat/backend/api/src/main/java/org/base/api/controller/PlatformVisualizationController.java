package org.base.api.controller;

import org.base.api.service.platform.PlatformVisualizationQueryService;
import org.base.api.service.platform.PublishedVisualization;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@RequestMapping("/platform/visualizations")
public class PlatformVisualizationController {
    private final PlatformVisualizationQueryService visuals;
    public PlatformVisualizationController(PlatformVisualizationQueryService visuals) { this.visuals=visuals; }
    @GetMapping("/{code}") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<PublishedVisualization> read(@PathVariable String code,@RequestParam long productId){return ResponseEntity.ok(visuals.read(productId,code));}
}
