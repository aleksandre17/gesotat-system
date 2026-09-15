package org.base.api.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Redis-backed distributed window counter. The increment and first-key TTL
 * assignment are one Lua operation, so concurrent API replicas cannot lose
 * increments or create an unbounded key. The public boundary remains the
 * provider-neutral {@link RateLimitStore} SPI.
 */
public final class RedisRateLimitStore implements RateLimitStore {
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]); " +
            "if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; " +
            "return n;", Long.class);

    private final RedisTemplate<String, String> redis;
    private final String keyPrefix;

    public RedisRateLimitStore(RedisTemplate<String, String> redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Window acquire(String key, long nowMillis, long windowMillis, int maxKeys) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("rate-limit key is required");
        if (windowMillis <= 0 || maxKeys <= 0) throw new IllegalArgumentException("rate-limit bounds must be positive");
        long windowStart = Math.floorDiv(nowMillis, windowMillis) * windowMillis;
        String redisKey = keyPrefix + ":" + sha256(key) + ":" + windowStart;
        Long value = redis.execute(INCREMENT, List.of(redisKey), Long.toString(windowMillis));
        if (value == null) throw new IllegalStateException("distributed rate-limit store returned no counter");
        if (value > Integer.MAX_VALUE) return new Window(windowStart, Integer.MAX_VALUE);
        return new Window(windowStart, value.intValue());
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the runtime", e);
        }
    }
}
