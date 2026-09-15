package org.base.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OidcRequiredClaimValidatorTest {
    private static Jwt token(Object tenant) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"), Map.of("tenant_id", tenant));
    }

    @Test void acceptsPresentTenantClaim() {
        assertTrue(new OidcRequiredClaimValidator("tenant_id").validate(token("tenant-a")).getErrors().isEmpty());
    }

    @Test void rejectsMissingOrBlankTenantClaim() {
        assertFalse(new OidcRequiredClaimValidator("tenant_id").validate(token(" ")).getErrors().isEmpty());
        assertFalse(new OidcRequiredClaimValidator("tenant_id").validate(new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), Map.of("sub", "user-1"))).getErrors().isEmpty());
        assertFalse(new OidcRequiredClaimValidator("tenant_id").validate(null).getErrors().isEmpty());
    }

    @Test void rejectsBlankClaimName() {
        assertThrows(IllegalArgumentException.class, () -> new OidcRequiredClaimValidator(" "));
    }
}
