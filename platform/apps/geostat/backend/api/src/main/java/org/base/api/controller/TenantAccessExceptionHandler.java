package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.base.api.security.tenancy.TenantAccessDeniedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC 9457 shape of a tenancy denial, for every governed controller. The body carries one constant
 * detail and no attribute of the denied object, so it cannot be used to probe another tenant's data.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantAccessExceptionHandler {

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> denied(TenantAccessDeniedException ex, HttpServletRequest request) {
        return ApiProblems.problem(HttpStatus.FORBIDDEN, "tenant-access-denied", TenantAccessDeniedException.DETAIL, request);
    }
}
