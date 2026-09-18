package org.base.core.service;

import org.junit.jupiter.api.Test;
import org.base.core.repository.TokenRepository;
import java.time.Instant;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtTokenUtilConfigurationTest {
    @Test
    void productionRejectsPlaceholderSecret() throws Exception {
        JwtTokenUtil util = configured("CHANGE_ME_" + "x".repeat(80), 60_000L, 60_000L);
        String previous = System.getProperty("spring.profiles.active");
        System.setProperty("spring.profiles.active", "prod");
        try { assertThrows(IllegalStateException.class, util::validateProductionConfiguration); }
        finally { restore(previous); }
    }

    @Test
    void productionAcceptsStrongSecretAndPositiveExpirations() throws Exception {
        JwtTokenUtil util = configured("a".repeat(64), 60_000L, 120_000L);
        String previous = System.getProperty("spring.profiles.active");
        System.setProperty("spring.profiles.active", "prod");
        try { assertDoesNotThrow(util::validateProductionConfiguration); }
        finally { restore(previous); }
    }

    @Test
    void scheduledCleanupHasAnInjectedRepository() {
        TokenRepository repository = mock(TokenRepository.class);
        new JwtTokenUtil(repository).removeExpiredTokens();
        verify(repository).deleteAllExpiredTokens(org.mockito.ArgumentMatchers.any(Instant.class));
    }

    private static JwtTokenUtil configured(String secret, Long expiration, Long refresh) throws Exception {
        JwtTokenUtil util = new JwtTokenUtil(mock(TokenRepository.class));
        set(util, "secret", secret); set(util, "expiration", expiration); set(util, "refreshExpiration", refresh);
        set(util, "issuer", "geostat-api"); set(util, "audience", "geostat-platform");
        return util;
    }
    private static void set(Object target, String name, Object value) throws Exception { Field f=JwtTokenUtil.class.getDeclaredField(name); f.setAccessible(true); f.set(target,value); }
    private static void restore(String previous) { if (previous == null) System.clearProperty("spring.profiles.active"); else System.setProperty("spring.profiles.active", previous); }
}
