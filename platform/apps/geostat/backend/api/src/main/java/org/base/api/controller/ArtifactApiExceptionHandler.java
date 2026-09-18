package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.base.api.service.artifact.ArtifactAccessDeniedException;
import org.base.api.service.artifact.ArtifactNotFoundException;
import org.base.api.service.artifact.ArtifactStorageException;
import org.base.api.service.artifact.ArtifactMalwareDetectedException;
import org.base.api.service.artifact.ArtifactScannerUnavailableException;
import org.base.api.service.artifact.ArtifactUploadQuotaExceededException;
import org.base.api.service.artifact.ArtifactUploadTooLargeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.UUID;

/** RFC 9457 problem details for the artifact boundary. Storage internals never reach the response. */
@RestControllerAdvice(assignableTypes = PlatformArtifactController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ArtifactApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ArtifactApiExceptionHandler.class);

    @ExceptionHandler(ArtifactNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(ArtifactNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "artifact-not-found", ex.getMessage(), request);
    }

    @ExceptionHandler(ArtifactAccessDeniedException.class)
    public ResponseEntity<ProblemDetail> denied(ArtifactAccessDeniedException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "artifact-access-denied", ex.getMessage(), request);
    }

    @ExceptionHandler(ArtifactUploadQuotaExceededException.class)
    public ResponseEntity<ProblemDetail> quota(ArtifactUploadQuotaExceededException ex, HttpServletRequest request) {
        return problem(HttpStatus.INSUFFICIENT_STORAGE, "artifact-upload-quota-exceeded", ex.getMessage(), request);
    }

    @ExceptionHandler(ArtifactUploadTooLargeException.class)
    public ResponseEntity<ProblemDetail> tooLarge(ArtifactUploadTooLargeException ex, HttpServletRequest request) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "artifact-upload-too-large", ex.getMessage(), request);
    }

    @ExceptionHandler(ArtifactMalwareDetectedException.class)
    public ResponseEntity<ProblemDetail> malware(ArtifactMalwareDetectedException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "artifact-content-rejected", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "artifact-request-invalid", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> conflict(IllegalStateException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "artifact-state-conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(ArtifactStorageException.class)
    public ResponseEntity<ProblemDetail> storage(ArtifactStorageException ex, HttpServletRequest request) {
        log.error("Artifact storage failure: {}", ex.getMessage(), ex.getCause());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "artifact-storage-unavailable", "Artifact storage is temporarily unavailable", request);
    }

    @ExceptionHandler(ArtifactScannerUnavailableException.class)
    public ResponseEntity<ProblemDetail> scanner(ArtifactScannerUnavailableException ex, HttpServletRequest request) {
        log.error("Artifact malware scanner unavailable", ex.getCause());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "artifact-scanner-unavailable", "Artifact scanning is temporarily unavailable", request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> data(DataAccessException ex, HttpServletRequest request) {
        log.error("Artifact registry query failed", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "artifact-registry-unavailable", "Artifact registry is temporarily unavailable", request);
    }

    static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail == null ? status.getReasonPhrase() : detail);
        problem.setType(URI.create("https://api.geostat.ge/problems/" + code));
        problem.setTitle(code);
        problem.setProperty("code", code);
        problem.setProperty("instance", request.getRequestURI());
        String correlation = request.getHeader("X-Correlation-Id");
        problem.setProperty("correlationId", correlation != null ? correlation : UUID.randomUUID().toString());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).header(HttpHeaders.CACHE_CONTROL, "no-store").body(problem);
    }
}
