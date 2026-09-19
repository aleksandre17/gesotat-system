package org.base.api.security.tenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.base.api.security.tenancy.scope.ResolvedScope;
import org.base.api.security.tenancy.scope.TenantScopeRequest;
import org.base.api.security.tenancy.scope.TenantScopeResolver;
import org.base.api.service.platform.ApprovedContractResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The single enforcement point of the governed HTTP boundary. Coverage is a declared property, not a
 * URL list: the interceptor is bound to the whole API surface and each controller's
 * {@link TenantScoped} / {@link TenantNeutral} annotation decides.
 *
 * <p>For a scoped handler the registered {@link TenantScopeResolver}s are asked in order; the first
 * resolved product is enforced. A scoped handler that no resolver answers for is <em>denied</em>
 * unless it declares a {@link TenantScopeExemption} with a reason. An undeclared controller inside
 * the governed controller package is denied too, so a new controller is covered by default and the
 * coverage guard test fails the build before it can ship.
 */
public class TenantScopedAccessInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TenantScopedAccessInterceptor.class);

    private final TenantAccessGuard guard;
    private final ApprovedContractResolver defaultContracts;
    private final List<TenantScopeResolver> resolvers;

    public TenantScopedAccessInterceptor(TenantAccessGuard guard, ApprovedContractResolver defaultContracts,
                                         List<TenantScopeResolver> resolvers) {
        this.guard = guard;
        this.defaultContracts = defaultContracts;
        this.resolvers = resolvers.stream().sorted(Comparator.comparingInt(TenantScopeResolver::order)).toList();
        if (this.resolvers.stream().map(TenantScopeResolver::name).distinct().count() != this.resolvers.size())
            throw new IllegalStateException("Tenant scope resolvers must have unique names");
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        Class<?> controller = method.getBeanType();
        TenantScopeClassification classification = TenantScopeDeclarations.classify(controller);
        if (classification == TenantScopeClassification.NEUTRAL) return true;
        if (classification == TenantScopeClassification.UNDECLARED) {
            if (!TenantScopeDeclarations.isGovernedController(controller)) return true;
            // Fail closed: a governed controller that declares no tenancy scope serves nothing.
            log.error("tenancy.undeclared controller={} uri={}", controller.getName(), request.getRequestURI());
            throw new TenantAccessDeniedException(TenantAccessOutcome.DENIED_PRODUCT_UNRESOLVED);
        }

        TenantScopeRequest scopeRequest = new TenantScopeRequest(request.getRequestURI(),
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE), request);
        Optional<ResolvedScope> resolved = resolve(scopeRequest);
        if (resolved.isPresent()) {
            guard.require(resolved.get().kind(), resolved.get().key(), resolved.get().product());
            return true;
        }

        TenantScopeExemption exemption = method.getMethodAnnotation(TenantScopeExemption.class);
        if (exemption == null) {
            log.warn("tenancy.unscoped-route controller={} method={} uri={}", controller.getSimpleName(),
                    method.getMethod().getName(), request.getRequestURI());
            throw new TenantAccessDeniedException(TenantAccessOutcome.DENIED_PRODUCT_UNRESOLVED);
        }
        switch (exemption.value()) {
            case DEFAULT_CONTRACT -> {
                String fallback = defaultContracts.resolve();
                // No approved contract at all means no product to serve; that is a denial, not a pass.
                guard.require("defaultContract", fallback,
                        fallback == null || fallback.isBlank() ? null : productOfContract(fallback));
            }
            case SERVICE_ENFORCED, CALLER_OWNED -> {
                /* The identity is not on the route. The declared service point enforces it; passing here
                   is what the annotation records, and the coverage guard test keeps the reason honest. */
            }
        }
        return true;
    }

    private Optional<ResolvedScope> resolve(TenantScopeRequest request) {
        for (TenantScopeResolver resolver : resolvers) {
            Optional<ResolvedScope> resolved = resolver.resolve(request);
            if (resolved.isPresent()) return resolved;
        }
        return Optional.empty();
    }

    private ProductTenancy productOfContract(String contractCode) {
        for (TenantScopeResolver resolver : resolvers) {
            if (!resolver.identities().contains("contractCode")) continue;
            Optional<ResolvedScope> resolved = resolver.resolve(
                    new TenantScopeRequest(null, Map.of("contractCode", contractCode), null));
            if (resolved.isPresent()) return resolved.get().product();
        }
        return null;
    }
}
