package org.base.core.setting.api;


import lombok.RequiredArgsConstructor;
import org.base.core.exeption.api.ApiAccessDeniedHandler;
import org.base.core.service.JwtTokenUtil;
import org.base.core.request.filter.api.JwtRequestFilter;
import org.base.core.request.filter.api.PlatformBootstrapAuthenticationFilter;
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
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.base.core.security.OidcAudienceValidator;
import org.base.core.security.OidcRequiredClaimValidator;
import org.base.core.security.OidcAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@Order(1)
@ConditionalOnProperty(name = "security.type", havingValue = "api")
public class ApiSecurityConfig {
    static final long MAX_CLOCK_SKEW_SECONDS = 120;

    //private final JwtRequestFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    private final AuthenticationProvider authenticationProvider;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;
    @Value("${platform.bootstrap-auth.enabled:false}") private boolean bootstrapAuthEnabled;
    @Value("${platform.bootstrap-auth.token:}") private String bootstrapAuthToken;
    @Value("${platform.oidc.enabled:false}") private boolean oidcEnabled;
    @Value("${platform.oidc.issuer-uri:}") private String oidcIssuer;
    @Value("${platform.oidc.audience:}") private String oidcAudience;
    @Value("${platform.oidc.roles-claim:realm_access.roles}") private String oidcRolesClaim;
    @Value("${platform.oidc.role-authority-map:contract.read=READ_RESOURCE;contract.write=WRITE_RESOURCE;ingest.execute=WRITE_RESOURCE;quality.approve=PUBLISH_RESOURCE;publish.execute=PUBLISH_RESOURCE;raw.read=READ_RESOURCE;admin=ADMIN}") private String oidcRoleAuthorityMap;
    @Value("${platform.oidc.tenant-claim:tenant_id}") private String oidcTenantClaim;
    /** Tolerated clock difference between the issuer and this API when exp/nbf are checked; an explicit, bounded number. */
    @Value("${platform.oidc.clock-skew-seconds:60}") private long oidcClockSkewSeconds;

    /** Optional standards-based resource-server mode; disabled unless explicitly enabled. */
    @Bean
    @ConditionalOnProperty(name = "platform.oidc.enabled", havingValue = "true")
    public JwtDecoder oidcJwtDecoder() {
        validateOidcSettings(oidcIssuer, oidcAudience);
        OAuth2TokenValidator<Jwt> issuer = new JwtIssuerValidator(oidcIssuer);
        OAuth2TokenValidator<Jwt> timestamps = new JwtTimestampValidator(clockSkew(oidcClockSkewSeconds));
        OidcAudienceValidator audience = new OidcAudienceValidator(oidcAudience);
        OidcRequiredClaimValidator tenant = new OidcRequiredClaimValidator(oidcTenantClaim);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(oidcIssuer).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(timestamps, issuer,
                audience, tenant);
        decoder.setJwtValidator(validator);
        return decoder;
    }

    /** Skew is a security parameter: negative, or wider than two minutes, is refused at startup. */
    static java.time.Duration clockSkew(long seconds) {
        if (seconds < 0 || seconds > MAX_CLOCK_SKEW_SECONDS)
            throw new IllegalStateException("platform.oidc.clock-skew-seconds must be between 0 and " + MAX_CLOCK_SKEW_SECONDS);
        return java.time.Duration.ofSeconds(seconds);
    }

    static void validateOidcSettings(String issuer, String audience) {
        if (issuer == null || issuer.isBlank()) throw new IllegalStateException("OIDC issuer URI is required when OIDC is enabled");
        if (audience == null || audience.isBlank()) throw new IllegalStateException("OIDC audience is required when OIDC is enabled");
        final URI parsed;
        try {
            parsed = URI.create(issuer.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("OIDC issuer URI is invalid", ex);
        }
        if (!parsed.isAbsolute() || parsed.getHost() == null || parsed.getHost().isBlank()) {
            throw new IllegalStateException("OIDC issuer URI must be an absolute URI with a host");
        }
        String profiles = System.getProperty("spring.profiles.active", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", ""));
        if (profiles.toLowerCase(Locale.ROOT).contains("prod")
                && !"https".equalsIgnoreCase(parsed.getScheme())) {
            throw new IllegalStateException("Production OIDC issuer must use HTTPS");
        }
    }

    @PostConstruct
    void validateBootstrapConfiguration() {
        String profiles = System.getProperty("spring.profiles.active", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", ""));
        if (bootstrapAuthEnabled && profiles.toLowerCase(Locale.ROOT).contains("prod")
                && (bootstrapAuthToken == null || bootstrapAuthToken.isBlank()
                || bootstrapAuthToken.startsWith("CHANGE_ME") || bootstrapAuthToken.length() < 32)) {
            throw new IllegalStateException("Production bootstrap authentication requires a non-placeholder token of at least 32 characters");
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        var apiFilter = new JwtRequestFilter(userDetailsService, jwtTokenUtil);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'self'; base-uri 'none'; form-action 'none'"))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                        .referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
                .securityMatchers(matchers -> matchers.requestMatchers(SecurityPaths.API_PATHS))
                .addFilterBefore(new PlatformBootstrapAuthenticationFilter(bootstrapAuthEnabled, bootstrapAuthToken), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/mobile/**", "/api/v1/mobile-text/**").permitAll()
                        // Ingest mutates controlled data and must never be anonymous.
                        // Authentication/authorization is evaluated by the method/resource policy below.
                        .requestMatchers("/sign/login", "/sign/register", "/sign/refresh").permitAll()
                        .requestMatchers("/ws/**").permitAll()
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
                .authenticationProvider(authenticationProvider);
        if (oidcEnabled) {
            JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
            authenticationConverter.setJwtGrantedAuthoritiesConverter(new OidcAuthoritiesConverter(oidcRolesClaim, oidcRoleAuthorityMap));
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .authenticationEntryPoint(apiAccessDeniedHandler)
                    .accessDeniedHandler(apiAccessDeniedHandler)
                    .jwt(jwt -> jwt.decoder(oidcJwtDecoder()).jwtAuthenticationConverter(authenticationConverter)));
        } else {
            http.addFilterBefore(apiFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();

    }


}
