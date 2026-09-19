package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.base.api.controller.StatisticalReferenceController.DecisionRequest;
import org.base.api.controller.StatisticalReferenceController.ProposeRequest;
import org.base.api.controller.StatisticalReferenceController.ReferenceView;
import org.base.api.security.tenancy.TenantNeutral;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.RegistryException;
import org.base.core.anotation.Api;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GLOBAL registry surface: cross-domain concepts, units, codelists and measures that every product may pin
 * (register Q07). A GLOBAL entry belongs to no tenant, so only the platform-wide cross-tenant authority may
 * write it; the authority name comes from the tenancy policy bean.
 */
@TenantNeutral(reason = "GLOBAL statistical references belong to no product; the surface is guarded by the platform-wide cross-tenant authority instead of a product boundary.")
@Api
@RestController
@RequestMapping("/platform/statistical-references")
public class StatisticalGlobalReferenceController {
    private static final String GLOBAL = "*";
    private final ReferenceRegistryService registry;

    public StatisticalGlobalReferenceController(ReferenceRegistryService registry) { this.registry = registry; }

    @PostMapping
    @PreAuthorize("hasAuthority(@tenantAccessPolicy.crossTenantAuthority())")
    public ResponseEntity<ReferenceView> propose(@RequestBody ProposeRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ReferenceView(StatisticalReferenceController.propose(registry, request, null).wire(), GLOBAL));
    }

    @PostMapping("/decisions")
    @PreAuthorize("hasAuthority(@tenantAccessPolicy.crossTenantAuthority())")
    public ResponseEntity<ReferenceView> decide(@RequestBody DecisionRequest request, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new ReferenceView(StatisticalReferenceController.decide(registry, request, GLOBAL, authentication.getName()).wire(), GLOBAL));
    }

    @ExceptionHandler(RegistryException.class)
    public ResponseEntity<ProblemDetail> registry(RegistryException ex, HttpServletRequest request) {
        return ApiProblems.problem(StatisticalReferenceController.STATUS.get(ex.failure()),
                "statistical-reference-" + ex.failure().name().toLowerCase().replace('_', '-'), ex.getMessage(), request);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(java.util.NoSuchElementException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.NOT_FOUND, "statistical-reference-not-found", "reference not found", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.BAD_REQUEST, "statistical-reference-invalid", ex.getMessage(), request);
    }
}
