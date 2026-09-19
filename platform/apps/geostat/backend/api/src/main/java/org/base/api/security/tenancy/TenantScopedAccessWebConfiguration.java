package org.base.api.security.tenancy;

import org.base.api.security.tenancy.scope.TenantScopeResolver;
import org.base.api.service.platform.ApprovedContractResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Binds the tenancy enforcement point to the whole API surface. Every governed route is mapped under
 * {@code /api/v1} by {@code CustomRequestMappingHandlerMapping} (it prefixes every controller that is
 * not {@code @Sign}, {@code @Web} or {@code @NoApiPrefix}), so one pattern covers them all and the
 * controller's own annotation — not a URL list — decides what happens.
 */
@Configuration
public class TenantScopedAccessWebConfiguration implements WebMvcConfigurer {
    private final TenantAccessGuard guard;
    private final ApprovedContractResolver defaultContracts;
    private final List<TenantScopeResolver> resolvers;

    public TenantScopedAccessWebConfiguration(TenantAccessGuard guard, ApprovedContractResolver defaultContracts,
                                              List<TenantScopeResolver> resolvers) {
        this.guard = guard;
        this.defaultContracts = defaultContracts;
        this.resolvers = resolvers;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantScopedAccessInterceptor(guard, defaultContracts, resolvers))
                .addPathPatterns("/api/v1/**")
                .order(Ordered.HIGHEST_PRECEDENCE + 10);
    }
}
