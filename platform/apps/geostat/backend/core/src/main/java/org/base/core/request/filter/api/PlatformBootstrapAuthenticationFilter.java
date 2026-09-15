package org.base.core.request.filter.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Short-lived, secret-header bootstrap identity for controlled platform operations. */
public final class PlatformBootstrapAuthenticationFilter extends OncePerRequestFilter {
    private final boolean enabled;
    private final String token;
    public PlatformBootstrapAuthenticationFilter(boolean enabled, String token) { this.enabled = enabled; this.token = token == null ? "" : token; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !enabled || !request.getRequestURI().startsWith("/api/v1/platform/"); }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader("X-Platform-Bootstrap-Token");
        if (!token.isBlank() && token.equals(supplied)) {
            var auth = new UsernamePasswordAuthenticationToken("platform-bootstrap", null,
                    List.of(new SimpleGrantedAuthority("READ_RESOURCE"), new SimpleGrantedAuthority("WRITE_RESOURCE"), new SimpleGrantedAuthority("PUBLISH_RESOURCE")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
