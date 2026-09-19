package org.base.api.security.tenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** {@link CurrentCaller} over Spring Security's context, plus an explicit SYSTEM scope for workers. */
@Component
public class SecurityContextCurrentCaller implements CurrentCaller {
    private static final ThreadLocal<Caller> SYSTEM_SCOPE = new ThreadLocal<>();

    private final String tenantClaim;
    private final boolean oidcEnabled;

    public SecurityContextCurrentCaller(@Value("${platform.oidc.tenant-claim:tenant_id}") String tenantClaim,
                                        @Value("${platform.oidc.enabled:false}") boolean oidcEnabled) {
        if (tenantClaim == null || tenantClaim.isBlank()) throw new IllegalArgumentException("Tenant claim name is required");
        this.tenantClaim = tenantClaim.trim();
        this.oidcEnabled = oidcEnabled;
    }

    @Override
    public Caller current() {
        Caller system = SYSTEM_SCOPE.get();
        if (system != null) return system;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Caller.anonymous();
        }
        Set<String> authorities = authentication.getAuthorities() == null ? Set.of()
                : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
        if (authentication instanceof JwtAuthenticationToken jwt) {
            return new Caller(jwt.getToken().getSubject(), jwt.getToken().getClaimAsString(tenantClaim), authorities, Caller.Kind.OIDC);
        }
        /* Not a resource-server token. While OIDC is enabled this is not a tenant-bearing identity and
           must not be treated as one; the policy denies it. While OIDC is disabled the platform runs in
           legacy/local mode and the policy applies its documented legacy branch. */
        return new Caller(authentication.getName(), null, authorities, oidcEnabled ? Caller.Kind.ANONYMOUS : Caller.Kind.LEGACY);
    }

    @Override
    public <T> T asSystem(String reason, Supplier<T> action) {
        if (action == null) throw new IllegalArgumentException("A system scope needs an action");
        Caller previous = SYSTEM_SCOPE.get();
        SYSTEM_SCOPE.set(Caller.system(reason));
        try {
            return action.get();
        } finally {
            if (previous == null) SYSTEM_SCOPE.remove(); else SYSTEM_SCOPE.set(previous);
        }
    }
}
