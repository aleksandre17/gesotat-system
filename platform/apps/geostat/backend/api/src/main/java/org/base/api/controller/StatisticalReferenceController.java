package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.base.api.security.tenancy.TenantScoped;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.Failure;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.MeasureSpec;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.RegistryException;
import org.base.api.service.platform.statistical.registry.ReferenceRegistryService.UnitSpec;
import org.base.core.anotation.Api;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Product-scoped registry of versioned statistical references: an author proposes, a steward decides.
 * The route carries {@code productCode}, so the tenant interceptor guards the product; every entry written
 * here is owned by that product. GLOBAL entries have their own platform-wide surface.
 */
@TenantScoped
@Api
@RestController
@RequestMapping("/platform/products/{productCode}/statistical-references")
public class StatisticalReferenceController {
    private final ReferenceRegistryService registry;

    public StatisticalReferenceController(ReferenceRegistryService registry) { this.registry = registry; }

    /** One closed request shape; {@code kind} of {@code ref} decides which definition fields are legal. */
    public record ProposeRequest(String ref, String conceptRef, String unitRef, Integer precision, Integer scale, Boolean approximate,
                                 String title, String quantityKind, Long classificationVersionId) { }

    public record DecisionRequest(String ref, String decision) { }

    public record ReferenceView(String ref, String owner) { }

    @PostMapping
    @PreAuthorize("hasAuthority(@statisticalContractAccessDecision.authorityName('AUTHOR'))")
    public ResponseEntity<ReferenceView> propose(@PathVariable String productCode, @RequestBody ProposeRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ReferenceView(propose(registry, request, productCode).wire(), productCode));
    }

    @PostMapping("/decisions")
    @PreAuthorize("hasAuthority(@statisticalContractAccessDecision.authorityName('APPROVE'))")
    public ResponseEntity<ReferenceView> decide(@PathVariable String productCode, @RequestBody DecisionRequest request, Authentication authentication) {
        Ref ref = decide(registry, request, productCode, authentication.getName());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ReferenceView(ref.wire(), productCode));
    }

    static Ref propose(ReferenceRegistryService registry, ProposeRequest request, String owner) {
        if (request == null || request.ref() == null) throw new IllegalArgumentException("ref is required");
        Ref ref = Ref.parse(request.ref());
        return switch (ref.kind()) {
            case CONCEPT, PROFILE -> registry.proposeIdentity(ref, owner);
            case UNIT -> registry.proposeUnit(ref, owner, new UnitSpec(request.quantityKind(), request.title()));
            case MEASURE -> registry.proposeMeasure(ref, owner, new MeasureSpec(
                    request.conceptRef() == null ? null : Ref.parse(request.conceptRef()), request.unitRef() == null ? null : Ref.parse(request.unitRef()),
                    request.precision() == null ? 0 : request.precision(), request.scale() == null ? 0 : request.scale(),
                    Boolean.TRUE.equals(request.approximate()), request.title()));
            case CODELIST -> {
                if (request.classificationVersionId() == null) throw new IllegalArgumentException("classificationVersionId is required");
                yield registry.proposeCodelist(ref, owner, request.classificationVersionId());
            }
            default -> throw new IllegalArgumentException("this kind is not registered through this surface: " + ref.kind().wire());
        };
    }

    /** @param owner product code, or {@code "*"} for the GLOBAL surface; an entry of another owner does not exist here */
    static Ref decide(ReferenceRegistryService registry, DecisionRequest request, String owner, String steward) {
        if (request == null || request.ref() == null || request.decision() == null) throw new IllegalArgumentException("ref and decision are required");
        Ref ref = Ref.parse(request.ref());
        if (!registry.ownerOf(ref).filter(owner::equals).isPresent()) throw new java.util.NoSuchElementException("reference not found");
        switch (request.decision()) {
            case "APPROVE" -> registry.approve(ref, steward);
            case "SUPERSEDE" -> registry.supersede(ref, steward);
            default -> throw new IllegalArgumentException("decision must be APPROVE or SUPERSEDE");
        }
        return ref;
    }

    static final Map<Failure, HttpStatus> STATUS = Map.of(
            Failure.ALREADY_DEFINED_DIFFERENTLY, HttpStatus.CONFLICT, Failure.ILLEGAL_TRANSITION, HttpStatus.CONFLICT,
            Failure.UNKNOWN_NAMESPACE, HttpStatus.UNPROCESSABLE_ENTITY, Failure.UNKNOWN_PRODUCT, HttpStatus.NOT_FOUND,
            Failure.UNRESOLVED_DEPENDENCY, HttpStatus.UNPROCESSABLE_ENTITY, Failure.NOT_FOUND, HttpStatus.NOT_FOUND,
            Failure.INVALID_DEFINITION, HttpStatus.BAD_REQUEST);

    @ExceptionHandler(RegistryException.class)
    public ResponseEntity<ProblemDetail> registry(RegistryException ex, HttpServletRequest request) {
        return ApiProblems.problem(STATUS.get(ex.failure()), "statistical-reference-" + ex.failure().name().toLowerCase().replace('_', '-'), ex.getMessage(), request);
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
