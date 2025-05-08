package org.base.core.setting;

public class SecurityPaths {
    public static final String[] API_PATHS = {"/api/v1/**", "/sign/**"};
    public static final String[] WEB_PATHS = {"/**"};
    public static final String[] PUBLIC_PATHS = {
            "/",
            "/error",
            "/error/**",  // Add this line to allow access to all error pages
            "/static/**",
            "/*.css",
            "/*.html",
            "/favicon.ico",
            "/css/**",
            "/*.css",
            "/default-ui.css",
            "/js/**",
            "/*.js",
            "/static/**",
            "/resources/**"


    };
}

