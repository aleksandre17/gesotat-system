package org.base.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OidcAuthoritiesConverterTest {
    private static Jwt token(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), claims);
    }

    @Test void mapsScopesAndNestedRoles() {
        var authorities = new OidcAuthoritiesConverter("realm_access.roles", "contract.read=READ_RESOURCE").convert(token(Map.of(
                "scope", "read write",
                "realm_access", Map.of("roles", List.of("contract.read", "ROLE_ADMIN")))));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_read")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_contract.read")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("READ_RESOURCE")));
        assertEquals(5, authorities.size());
    }

    @Test void missingRoleClaimIsSafeAndNullTokenIsEmpty() {
        assertTrue(new OidcAuthoritiesConverter("realm_access.roles").convert(token(Map.of("sub", "u"))).isEmpty());
        assertTrue(new OidcAuthoritiesConverter("realm_access.roles").convert(null).isEmpty());
    }

    @Test void acceptsSingleStringRoleClaim() {
        var authorities = new OidcAuthoritiesConverter("roles", "contract.read=READ_RESOURCE")
                .convert(token(Map.of("roles", "contract.read")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("READ_RESOURCE")));
    }

    @Test void rejectsBlankClaimPath() {
        assertThrows(IllegalArgumentException.class, () -> new OidcAuthoritiesConverter(" "));
        assertThrows(IllegalArgumentException.class, () -> new OidcAuthoritiesConverter("roles", "broken"));
        assertThrows(IllegalArgumentException.class, () -> new OidcAuthoritiesConverter("roles", "contract.read=READ_RESOURCE;contract.read=WRITE_RESOURCE"));
    }
}
