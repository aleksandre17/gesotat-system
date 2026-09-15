package org.base.core.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Provider-neutral scope/role mapper with configurable dotted claim path. */
public final class OidcAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    private final String rolesClaimPath;
    private final Map<String, Set<String>> roleAuthorities;

    public OidcAuthoritiesConverter(String rolesClaimPath) {
        this(rolesClaimPath, "");
    }

    public OidcAuthoritiesConverter(String rolesClaimPath, String roleAuthorityMapping) {
        if (rolesClaimPath == null || rolesClaimPath.isBlank()) {
            throw new IllegalArgumentException("OIDC roles claim path must not be blank");
        }
        this.rolesClaimPath = rolesClaimPath.trim();
        this.roleAuthorities = parseMapping(roleAuthorityMapping);
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        LinkedHashSet<GrantedAuthority> result = new LinkedHashSet<>();
        if (jwt == null) return result;
        Collection<GrantedAuthority> scopeAuthorities = scopes.convert(jwt);
        if (scopeAuthorities != null) result.addAll(scopeAuthorities);
        Object value = jwt.getClaims();
        for (String part : rolesClaimPath.split("\\.")) {
            if (!(value instanceof Map<?, ?> map)) { value = null; break; }
            value = map.get(part);
        }
        Collection<?> roles = value instanceof Collection<?> collection ? collection
                : value instanceof String single ? java.util.List.of(single) : java.util.List.of();
        for (Object role : roles) {
            if (role != null && !role.toString().isBlank()) {
                String normalized = role.toString().trim();
                result.add(new SimpleGrantedAuthority(normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized));
                for (String authority : roleAuthorities.getOrDefault(normalized, Set.of())) {
                    result.add(new SimpleGrantedAuthority(authority));
                }
            }
        }
        return result;
    }

    private static Map<String, Set<String>> parseMapping(String specification) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (specification == null || specification.isBlank()) return result;
        for (String entry : specification.split(";")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length != 2 || pair[0].isBlank() || pair[1].isBlank()) {
                throw new IllegalArgumentException("Invalid OIDC role-authority mapping");
            }
            String role = pair[0].trim();
            if (result.containsKey(role)) throw new IllegalArgumentException("Duplicate OIDC role mapping: " + role);
            Set<String> authorities = new LinkedHashSet<>();
            for (String authority : pair[1].split(",")) {
                if (authority.isBlank()) throw new IllegalArgumentException("Invalid OIDC role-authority mapping");
                authorities.add(authority.trim());
            }
            result.put(role, Set.copyOf(authorities));
        }
        return Map.copyOf(result);
    }
}
