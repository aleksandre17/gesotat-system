package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.base.api.service.artifact.ArtifactMalwareDetectedException;
import org.base.api.service.artifact.ArtifactScannerUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Malware admission outcomes for Access package uploads, with the same RFC 9457 codes as the
 * artifact boundary. Only admission exceptions are handled here; the controllers' other error
 * semantics are unchanged.
 */
@RestControllerAdvice(assignableTypes = {PlatformAccessIngestionController.class, PlatformSemanticAccessController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessAdmissionExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AccessAdmissionExceptionHandler.class);

    @ExceptionHandler(ArtifactMalwareDetectedException.class)
    public ResponseEntity<ProblemDetail> malware(ArtifactMalwareDetectedException ex, HttpServletRequest request) {
        return ArtifactApiExceptionHandler.problem(HttpStatus.UNPROCESSABLE_ENTITY, "artifact-content-rejected", ex.getMessage(), request);
    }

    @ExceptionHandler(ArtifactScannerUnavailableException.class)
    public ResponseEntity<ProblemDetail> scanner(ArtifactScannerUnavailableException ex, HttpServletRequest request) {
        log.error("Access package malware scanner unavailable", ex.getCause());
        return ArtifactApiExceptionHandler.problem(HttpStatus.SERVICE_UNAVAILABLE, "artifact-scanner-unavailable", "Artifact scanning is temporarily unavailable", request);
    }
}
