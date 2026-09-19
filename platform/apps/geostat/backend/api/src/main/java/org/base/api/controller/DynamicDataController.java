package org.base.api.controller;

import org.base.api.service.dynamic.DynamicTableResponse;
import org.base.api.service.dynamic.DynamicTableService;
import org.base.api.service.dynamic.DynamicChartService;
import org.base.api.service.dynamic.DynamicChartResponse;
import org.base.api.service.platform.CanonicalPageDataService;
import org.base.api.service.platform.ApprovedContractResolver;
import org.base.api.service.platform.LegacyFallbackPolicy;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.base.api.security.tenancy.TenantScoped;
import org.base.api.security.tenancy.TenantScopeExemption;

/** Read-only dynamic data endpoint. Legacy profile mode remains for compatibility;
 * callers may pass contractCode to use the governed canonical contract engine. */
@TenantScoped
@Api
@RestController
@RequestMapping("/dynamic/pages")
public class DynamicDataController {
    private final DynamicTableService dynamicTableService;
    private final DynamicChartService dynamicChartService;
    private final CanonicalPageDataService canonicalPageDataService;
    private final ApprovedContractResolver approvedContracts;
    private final boolean legacyFallbackEnabled;
    private final Counter legacyFallbackCounter;

    public DynamicDataController(DynamicTableService dynamicTableService, DynamicChartService dynamicChartService,
                                 CanonicalPageDataService canonicalPageDataService, ApprovedContractResolver approvedContracts,
                                 boolean legacyFallbackEnabled) {
        this(dynamicTableService, dynamicChartService, canonicalPageDataService, approvedContracts, legacyFallbackEnabled, new SimpleMeterRegistry());
    }

    @Autowired
    public DynamicDataController(DynamicTableService dynamicTableService, DynamicChartService dynamicChartService,
                                 CanonicalPageDataService canonicalPageDataService, ApprovedContractResolver approvedContracts,
                                 @Value("${platform.legacy.dynamic-pages.enabled:true}") boolean legacyFallbackEnabled,
                                 MeterRegistry meterRegistry) {
        this.dynamicTableService = dynamicTableService;
        this.dynamicChartService = dynamicChartService;
        this.canonicalPageDataService = canonicalPageDataService;
        this.approvedContracts = approvedContracts;
        this.legacyFallbackEnabled = legacyFallbackEnabled;
        this.legacyFallbackCounter = Counter.builder("geostat.legacy.dynamic_pages.fallback").description("Legacy dynamic page fallback invocations").register(meterRegistry);
    }

    @GetMapping("/{pageId}/data")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    @TenantScopeExemption(value = TenantScopeExemption.Kind.DEFAULT_CONTRACT, reason = "Without an explicit contractCode the route serves the approved default contract; the legacy profile fallback behind it carries no product identity and is scheduled for retirement.")
    public ResponseEntity<?> data(@PathVariable Long pageId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "100") int limit,
                                                     @RequestParam(required = false) String contractCode) {
        if (contractCode != null && !contractCode.isBlank())
            return ResponseEntity.ok(canonicalPageDataService.read(contractCode, pageId.intValue(), page, limit,
                    null, null, null, null, null, java.util.Map.of()));
        // Canonical contract is the default path. The profile reader is retained only
        // for pre-contract pages that have no governed binding yet.
        try {
            String contract=approvedContracts.resolve();
            if(contract!=null) return ResponseEntity.ok(canonicalPageDataService.read(contract, pageId.intValue(), page, limit,
                    null, null, null, null, null, java.util.Map.of()));
            
        } catch (DataAccessException | IllegalArgumentException legacyPage) {
            // Compatibility boundary for old, explicitly profiled pages.
        }
        var fallbackDecision = LegacyFallbackPolicy.evaluate(legacyFallbackEnabled, false);
        if (!fallbackDecision.allowed())
            throw new IllegalStateException("Legacy dynamic page fallback is disabled; use an approved contract");
        legacyFallbackCounter.increment();
        return ResponseEntity.ok(dynamicTableService.read(pageId, page, limit));
    }

    @GetMapping("/{pageId}/charts/{chartCode}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    @TenantScopeExemption(value = TenantScopeExemption.Kind.DEFAULT_CONTRACT, reason = "Legacy chart definitions are addressed by page id and carry no product identity; the route is enforced against the approved default contract's product.")
    public ResponseEntity<DynamicChartResponse> chart(@PathVariable Long pageId, @PathVariable String chartCode) {
        return ResponseEntity.ok(dynamicChartService.read(pageId, chartCode));
    }
}
