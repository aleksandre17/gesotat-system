package org.base.mobile.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.base.mobile.LanguageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Filter to set language-specific attributes based on the 'lang' query parameter.
 */
@Component
public class LanguageFilter implements Filter {

    @Autowired
    private LanguageService languageService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String lang = httpRequest.getParameter("lang");
        String effectiveLang = "en".equals(lang) ? "en" : "ka";

        Map<String, String> columnNames = languageService.getColumnNames(effectiveLang);
        httpRequest.setAttribute("lang", effectiveLang);
        httpRequest.setAttribute("langName", columnNames.get("langName"));
        httpRequest.setAttribute("period", columnNames.get("period"));
        httpRequest.setAttribute("title", columnNames.get("title"));
        httpRequest.setAttribute("langTranslations", languageService.getTranslations(effectiveLang));

        chain.doFilter(request, response);
    }
}

