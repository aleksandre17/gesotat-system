package org.base.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Locale;

/** Applies a conservative, explicit negotiation boundary to governed contract endpoints. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class ContractContentNegotiationFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String path=req.getRequestURI();
        boolean governed=path.contains("/platform/contracts/") || path.startsWith("/api/v1/sdmx/") || path.startsWith("/sdmx/");
        if(!governed){chain.doFilter(req,res);return;}
        if (req.getMethod().matches("POST|PUT|PATCH")) {
            String contentType = req.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
                res.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                res.setContentType("application/problem+json");
                res.getWriter().write("{\"type\":\"https://api.geostat.ge/problems/unsupported-media-type\",\"title\":\"JSON content type is required\",\"status\":415,\"code\":\"UNSUPPORTED_MEDIA_TYPE\"}");
                return;
            }
        }
        String accept=req.getHeader("Accept");
        if(accept!=null && !accept.isBlank() && !accept.contains("*/*") && !accept.toLowerCase(Locale.ROOT).contains("application/json") && !(path.startsWith("/sdmx/") && accept.toLowerCase(Locale.ROOT).contains("application/vnd.sdmx"))){
            res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE); res.setContentType("application/problem+json");
            res.getWriter().write("{\"type\":\"https://api.geostat.ge/problems/not-acceptable\",\"title\":\"Requested representation is not supported\",\"status\":406,\"code\":\"NOT_ACCEPTABLE\"}"); return;
        }
        res.addHeader("Vary","Accept, Accept-Language, Accept-Profile");
        chain.doFilter(req,res);
    }
}
