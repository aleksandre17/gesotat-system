package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.base.api.service.contract.approval.ContractApprovalBlockedException;
import org.base.api.service.contract.approval.SiteContractRevisionApprovalService;
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

import java.util.NoSuchElementException;

/** Governed site contract revision approval: validate (no write), then confirm. */
@Api
@RestController
@RequestMapping("/platform/site-contracts/{contractCode}/revisions/{revision}/approval")
@RequiredArgsConstructor
public class PlatformSiteContractRevisionController {
    private final SiteContractRevisionApprovalService approvals;

    public record ApprovalRequest(String breakingAcknowledgement) {}

    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('PUBLISH_RESOURCE')")
    public ResponseEntity<SiteContractRevisionApprovalService.ApprovalPreview> preview(
            @PathVariable String contractCode, @PathVariable int revision, @RequestBody(required = false) ApprovalRequest request) {
        var preview = approvals.preview(contractCode, revision, acknowledgement(request));
        return ResponseEntity.status(preview.approvable() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY)
                .cacheControl(CacheControl.noStore()).body(preview);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PUBLISH_RESOURCE')")
    public ResponseEntity<SiteContractRevisionApprovalService.ApprovalReceipt> approve(
            @PathVariable String contractCode, @PathVariable int revision, @RequestBody(required = false) ApprovalRequest request,
            Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(approvals.approve(contractCode, revision, acknowledgement(request), authentication.getName()));
    }

    private static String acknowledgement(ApprovalRequest request) {
        return request == null ? null : request.breakingAcknowledgement();
    }

    @ExceptionHandler(ContractApprovalBlockedException.class)
    public ResponseEntity<ProblemDetail> blocked(ContractApprovalBlockedException ex, HttpServletRequest request) {
        var response = ApiProblems.problem(HttpStatus.UNPROCESSABLE_ENTITY, "contract-approval-blocked", ex.getMessage(), request);
        response.getBody().setProperty("checks", ex.checks());
        return response;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> notFound(NoSuchElementException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.NOT_FOUND, "contract-revision-not-found", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> conflict(IllegalStateException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.CONFLICT, "contract-revision-state-conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.BAD_REQUEST, "contract-approval-request-invalid", ex.getMessage(), request);
    }
}
