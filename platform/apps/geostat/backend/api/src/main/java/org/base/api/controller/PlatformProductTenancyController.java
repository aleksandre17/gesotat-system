package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.base.api.security.tenancy.ProductTenancy;
import org.base.api.security.tenancy.ProductTenancyService;
import org.base.core.anotation.Api;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;
import org.base.api.security.tenancy.TenantNeutral;

/**
 * Governed tenant assignment of a data product. Only the platform-wide cross-tenant authority may
 * call it; the authority name comes from the policy bean, so the property is the single source.
 */
@TenantNeutral(reason = "The tenancy administration surface itself; it is guarded by the platform-wide cross-tenant authority and would deadlock against its own rule.")
@Api
@RestController
@RequestMapping("/platform/products/{productCode}/tenant")
@RequiredArgsConstructor
public class PlatformProductTenancyController {
    private final ProductTenancyService tenancy;

    /** @param transfer must be true, with a reason, to move a product away from its current tenant */
    public record AssignTenantRequest(String tenantKey, boolean transfer, String reason) {}

    public record TenantView(String productCode, String tenantKey, boolean assigned) {}

    @GetMapping
    @PreAuthorize("hasAuthority(@tenantAccessPolicy.crossTenantAuthority())")
    public ResponseEntity<TenantView> read(@PathVariable String productCode) {
        ProductTenancy product = tenancy.read(productCode);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new TenantView(product.productCode(), product.tenantKey(), product.assigned()));
    }

    @PutMapping
    @PreAuthorize("hasAuthority(@tenantAccessPolicy.crossTenantAuthority())")
    public ResponseEntity<ProductTenancyService.AssignmentReceipt> assign(@PathVariable String productCode,
                                                                         @RequestBody AssignTenantRequest request,
                                                                         Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("An assignment body is required");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(tenancy.assign(productCode, request.tenantKey(), request.transfer(), request.reason(), authentication.getName()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(NoSuchElementException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.NOT_FOUND, "data-product-not-found", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> conflict(IllegalStateException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.CONFLICT, "product-tenant-assignment-conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.BAD_REQUEST, "product-tenant-assignment-invalid", ex.getMessage(), request);
    }
}
