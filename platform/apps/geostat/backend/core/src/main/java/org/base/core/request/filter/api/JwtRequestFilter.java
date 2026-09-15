package org.base.core.request.filter.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.base.core.model.response.ApiExceptionResponse;
import org.base.core.exeption.ErrorCode;
import org.base.core.service.JwtTokenUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;

public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    public JwtRequestFilter(UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/platform/")
                && "true".equalsIgnoreCase(System.getenv("PLATFORM_BOOTSTRAP_AUTH_ENABLED"))
                && !System.getenv().getOrDefault("PLATFORM_BOOTSTRAP_AUTH_TOKEN", "").isBlank()
                && System.getenv("PLATFORM_BOOTSTRAP_AUTH_TOKEN").equals(request.getHeader("X-Platform-Bootstrap-Token"))) return true;
        return path.equals("/health") ||
                path.equals("/sign/login") ||
                path.equals("/sign/register") ||
                path.equals("/sign/refresh") ||
                path.equals("/error") ||
                path.equals("/") ||
                path.endsWith(".html") ||
                path.endsWith("/download-all") ||
                path.endsWith("/api/v1/pages/roots") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/static/") ||
                path.startsWith("/resources/") ||
                path.startsWith("/ws/") ||
                path.contains("mobile/") ||
                path.contains("mobile-text/");
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        // A separately authenticated, tightly scoped bootstrap identity may be
        // installed before this filter. Never overwrite or challenge it.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        final String signHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        if (signHeader == null || !signHeader.startsWith("Bearer ")) {
            //filterChain.doFilter(request, response);
            sendErrorResponse(
                    new AuthenticationException("No JWT token found") {},
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing JWT token"
            );

            return;
        }
        jwt = signHeader.substring(7);

        try {
            username = jwtTokenUtil.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtTokenUtil.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            sendErrorResponse(e, response, HttpServletResponse.SC_UNAUTHORIZED, "JWT token has expired");
        } catch (SignatureException | MalformedJwtException e) {
            sendErrorResponse(e, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT signature");
        } catch (UnsupportedJwtException e) {
            sendErrorResponse(e, response, HttpServletResponse.SC_UNAUTHORIZED, "Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            sendErrorResponse(e, response, HttpServletResponse.SC_BAD_REQUEST, "JWT claims string is empty");
        } catch (Exception e) {
            // Forward to /error endpoint to trigger global exception handling
            //request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, e);
            //request.getRequestDispatcher("/error").forward(request, response);
            log.warn("JWT authentication failed: {}", e.getClass().getSimpleName());
            sendErrorResponse(e, response, HttpServletResponse.SC_BAD_REQUEST, "Not Valid JWT token");
        }

    }

    private void sendErrorResponse(Exception e, HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiExceptionResponse errorResponse = new ApiExceptionResponse(
                HttpStatus.valueOf(status),
                message,
                Collections.singletonList(message)
        );

        errorResponse.setErrorCode(ErrorCode.UNSIGN.getCode());

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

}
