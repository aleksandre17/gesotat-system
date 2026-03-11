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

import java.io.IOException;
import java.util.Collections;

public class JwtRequestFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    public JwtRequestFilter(UserDetailsService userDetailsService, JwtTokenUtil jwtTokenUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/health") ||
                path.startsWith("/sign/") ||
                path.equals("/error") ||
                path.equals("/") ||
                path.endsWith(".html") ||
                path.endsWith("/download-all") ||
                path.contains("/api/v1/xlsx-to-csv/convert") ||
                path.contains("/api/v1/xlsx-to-csv/download-zip") ||
                path.endsWith("/api/v1/pages/roots") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/static/") ||
                path.startsWith("/resources/") ||
                path.startsWith("/ws/") ||
                path.contains("import/") ||
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
            e.printStackTrace();
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
                Collections.singletonList(e.getMessage() == null ? message : e.getMessage())
        );

        errorResponse.setErrorCode(ErrorCode.UNSIGN.getCode());

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

}