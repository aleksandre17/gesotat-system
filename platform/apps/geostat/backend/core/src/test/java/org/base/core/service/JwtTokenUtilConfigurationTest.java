package org.base.core.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

    private static JwtTokenUtil configured(String secret, Long expiration, Long refresh) throws Exception {
        JwtTokenUtil util = new JwtTokenUtil();
        set(util, "secret", secret); set(util, "expiration", expiration); set(util, "refreshExpiration", refresh);
        set(util, "issuer", "geostat-api"); set(util, "audience", "geostat-platform");
        return util;
    }
    private static void set(Object target, String name, Object value) throws Exception { Field f=JwtTokenUtil.class.getDeclaredField(name); f.setAccessible(true); f.set(target,value); }
    private static void restore(String previous) { if (previous == null) System.clearProperty("spring.profiles.active"); else System.setProperty("spring.profiles.active", previous); }
}
