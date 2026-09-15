package org.base.core.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OidcAudienceValidatorTest {
    private static Jwt token(String... audiences) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("alg", "RS256"), Map.of("aud", List.of(audiences)));
    }

    @Test void acceptsRequiredAudience() {
        assertTrue(new OidcAudienceValidator("geostat-api").validate(token("geostat-api")).getErrors().isEmpty());
    }

    @Test void rejectsMissingAudience() {
        assertFalse(new OidcAudienceValidator("geostat-api").validate(token("other-api")).getErrors().isEmpty());
    }

    @Test void rejectsBlankPolicy() {
        assertThrows(IllegalArgumentException.class, () -> new OidcAudienceValidator(" "));
    }

    @Test void rejectsNullTokenFailClosed() {
        assertFalse(new OidcAudienceValidator("geostat-api").validate(null).getErrors().isEmpty());
    }
}
