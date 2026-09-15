package org.base.core.setting.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiSecurityConfigOidcPolicyTest {
    @Test void requiresIssuerAndAudienceWhenOidcIsEnabled() {
        assertThrows(IllegalStateException.class, () -> ApiSecurityConfig.validateOidcSettings("", "geostat-api"));
        assertThrows(IllegalStateException.class, () -> ApiSecurityConfig.validateOidcSettings("https://issuer.example", ""));
        assertThrows(IllegalStateException.class, () -> ApiSecurityConfig.validateOidcSettings("https://issuer.example", null));
        assertThrows(IllegalStateException.class, () -> ApiSecurityConfig.validateOidcSettings("not-a-uri", "geostat-api"));
        assertThrows(IllegalStateException.class, () -> ApiSecurityConfig.validateOidcSettings("/relative/path", "geostat-api"));
    }

    @Test void acceptsCompleteOidcPolicy() {
        assertDoesNotThrow(() -> ApiSecurityConfig.validateOidcSettings("https://issuer.example", "geostat-api"));
    }

    @Test void rejectsHttpIssuerInProductionProfile() {
        String previous = System.getProperty("spring.profiles.active");
        try {
            System.setProperty("spring.profiles.active", "prod");
            assertThrows(IllegalStateException.class,
                    () -> ApiSecurityConfig.validateOidcSettings("http://issuer.example", "geostat-api"));
        } finally {
            if (previous == null) System.clearProperty("spring.profiles.active");
            else System.setProperty("spring.profiles.active", previous);
        }
    }
}
