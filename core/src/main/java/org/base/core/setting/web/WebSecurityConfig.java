package org.base.core.setting.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.base.core.request.filter.web.LoginPageFilter;
import org.base.core.setting.SecurityPaths;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@Order(2)
@ConditionalOnProperty(name = "security.type", havingValue = "web")
public class WebSecurityConfig implements WebMvcConfigurer {

    private final AuthenticationProvider authenticationProvider;

    @Bean
    public AuthenticationSuccessHandler signSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                if (request.getRequestURI().equals("/login")) {
                    response.sendRedirect("/dashboard");
                    return;
                }
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }


    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(request ->
                        !request.getRequestURI().startsWith("/api/") &&
                        !request.getRequestURI().startsWith("/sign/")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/dashboard")).authenticated()
                        .requestMatchers("/error").permitAll()  // Add explicit /error path
                        .requestMatchers("/error/**").permitAll()
                        .requestMatchers("/.well-known/**").permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/login/**")).permitAll()
                        .requestMatchers("/static/**", "/*.css", "/*.js", "/*.ico").permitAll()
                        .requestMatchers(SecurityPaths.PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(new LoginPageFilter(), UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.successHandler(signSuccessHandler())
                        .defaultSuccessUrl("/dashboard")
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login") //logout=true
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                        .ignoringRequestMatchers("/sign/**")
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                        .defaultAuthenticationEntryPointFor((request, response, authException) ->
                                        response.sendRedirect("/error/401"),
                                new AntPathRequestMatcher("/**")
                        )

                )
                .build();

    }
}

