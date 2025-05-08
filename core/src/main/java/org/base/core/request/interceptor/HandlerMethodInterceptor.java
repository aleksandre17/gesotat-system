package org.base.core.request.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class HandlerMethodInterceptor implements HandlerInterceptor {

    public static final String HANDLER_METHOD_ATTRIBUTE = "handlerMethod";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (handler instanceof HandlerMethod) {
            request.setAttribute(HANDLER_METHOD_ATTRIBUTE, handler);
        }

        if (!request.getRequestURI().startsWith("/api/") && !request.getRequestURI().startsWith("/sign/")) {
            String uri = request.getRequestURI();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

            if (isAuthenticated && uri.equals("/login")) {
                response.sendRedirect("/dashboard");
                return false;
            }
        }

        return true;
    }
}
