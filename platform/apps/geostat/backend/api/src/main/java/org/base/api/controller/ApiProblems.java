package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.UUID;

/** RFC 9457 response shape shared by the platform controllers. */
final class ApiProblems {
    private ApiProblems() {}

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
