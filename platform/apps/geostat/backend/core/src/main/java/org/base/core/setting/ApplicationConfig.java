package org.base.core.setting;

import lombok.RequiredArgsConstructor;
import org.base.core.exeption.api.ApiAccessDeniedHandler;
import org.base.core.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    @Value("${platform.cors.allowed-origins:http://192.168.1.199:5175,http://192.168.2.87:8085,http://localhost:8085,http://192.168.2.87,http://localhost,http://localhost:80,http://localhost:5173,http://localhost:5174,http://localhost:8080,http://localhost:8081,https://automobile.geostat.ge}")
    private String allowedOrigins;
    @Value("${platform.cors.allowed-origin-patterns:http://localhost:*}")
    private String allowedOriginPatterns;


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(csv(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(csv(allowedOriginPatterns));
        configuration.setMaxAge(3600L);


        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        //source.registerCorsConfiguration("**", configuration); ///api/**
        source.registerCorsConfiguration("/sign/**", configuration);
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        return source;
    }

    private static List<String> csv(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isBlank()).distinct().toList();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userService;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean("ApiAccessDeniedMatcher")
    public ApiAccessDeniedHandler apiAccessDeniedHandler() {
        return new ApiAccessDeniedHandler();
    }

}
