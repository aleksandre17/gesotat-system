package org.base.core.request.filter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoginPageFilter extends GenericFilterBean {
    private static final Logger log = LoggerFactory.getLogger(LoginPageFilter.class);
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && ((HttpServletRequest)request).getRequestURI().equals("/login")) {
            log.debug("Authenticated principal requested login page; redirecting");
            ((HttpServletResponse)response).sendRedirect("/");
        }
        chain.doFilter(request, response);
    }
}
