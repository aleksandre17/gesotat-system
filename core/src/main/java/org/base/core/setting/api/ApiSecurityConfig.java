package org.base.core.setting.api;


import lombok.RequiredArgsConstructor;
import org.base.core.exeption.api.ApiAccessDeniedHandler;
import org.base.core.service.JwtTokenUtil;
import org.base.core.request.filter.api.JwtRequestFilter;
import org.base.core.setting.SecurityPaths;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@Order(1)
@ConditionalOnProperty(name = "security.type", havingValue = "api")
public class ApiSecurityConfig {

    //private final JwtRequestFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    private final AuthenticationProvider authenticationProvider;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        var apiFilter = new JwtRequestFilter(userDetailsService, jwtTokenUtil);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .securityMatchers(matchers -> matchers.requestMatchers(SecurityPaths.API_PATHS))
                .addFilterBefore(apiFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/mobile/**", "/api/v1/mobile-text/**").permitAll()
                        .requestMatchers("/api/v1/import", "/api/v1/import/**").permitAll()
                        .requestMatchers("/sign/**", "/ws/**").permitAll()
                        .requestMatchers("/api/v1/pages/roots").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> {
                    ex.accessDeniedHandler(apiAccessDeniedHandler);
                    ex.authenticationEntryPoint(apiAccessDeniedHandler);
                })
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .build();

    }


}