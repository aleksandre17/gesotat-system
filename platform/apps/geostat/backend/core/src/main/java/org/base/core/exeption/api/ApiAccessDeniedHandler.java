package org.base.core.exeption.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 401 and 403 of the API boundary: an RFC 9457 problem body and the RFC 6750 {@code WWW-Authenticate}
 * challenge. The reason a token was refused is never disclosed beyond the standard error code. The members
 * {@code error}, {@code message}, {@code path} and {@code timestamp} are kept as problem extensions for
 * clients written against the earlier body.
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler, AuthenticationEntryPoint {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final String BEARER = "Bearer";
    private static final Pattern BEARER_SCHEME = Pattern.compile("^\s*bearer(\s|$)", Pattern.CASE_INSENSITIVE);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException denied) throws IOException {
        write(request, response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Access Denied", "Forbidden",
                BEARER + " error=\"insufficient_scope\"");
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure) throws IOException {
        String presented = request.getHeader(HttpHeaders.AUTHORIZATION);
        // Any Bearer credential that was presented and refused, including an empty one, is an invalid token (RFC 6750 3.1).
        boolean bearerPresented = presented != null && BEARER_SCHEME.matcher(presented).find();
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Sign required", "UnSign",
                bearerPresented ? BEARER + " error=\"invalid_token\"" : BEARER);
    }

    private static void write(HttpServletRequest request, HttpServletResponse response, int status, String title, String message,
                              String legacyError, String challenge) throws IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        String supplied = request.getHeader("X-Correlation-Id");
        String correlationId = supplied != null && SAFE_CORRELATION_ID.matcher(supplied).matches() ? supplied : java.util.UUID.randomUUID().toString();
        response.setHeader("X-Correlation-Id", correlationId);
        response.setStatus(status);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", title);
        problem.put("status", status);
        problem.put("correlationId", correlationId);
        problem.put("error", legacyError);
        problem.put("message", message);
        problem.put("path", request.getRequestURI());
        problem.put("timestamp", System.currentTimeMillis());
        response.getWriter().write(JSON.writeValueAsString(problem));
    }
}
