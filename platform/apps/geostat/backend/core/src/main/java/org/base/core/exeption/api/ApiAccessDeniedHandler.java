package org.base.core.exeption.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler, AuthenticationEntryPoint {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        harden(response);
        String correlationId = correlationId(request, response);
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("status", 403);
        data.put("error", "Forbidden");
        data.put("message", "Access Denied");
        data.put("path", request.getRequestURI());
        data.put("correlationId", correlationId);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(data));
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        harden(response);
        String correlationId = correlationId(request, response);
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("status", 401);
        data.put("error", "UnSign");
        data.put("message", "Sign required");
        data.put("path", request.getRequestURI());
        data.put("correlationId", correlationId);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(data));
    }

    private static void harden(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private static String correlationId(HttpServletRequest request, HttpServletResponse response) {
        String value = request.getHeader("X-Correlation-Id");
        if (value == null || !value.matches("[A-Za-z0-9._:-]{1,128}")) value = java.util.UUID.randomUUID().toString();
        response.setHeader("X-Correlation-Id", value);
        return value;
    }
}
