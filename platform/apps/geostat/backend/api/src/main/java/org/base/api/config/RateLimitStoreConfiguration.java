package org.base.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Default rate-limit store wiring. A deployment may provide a distributed
 * {@link RateLimitStore} bean (Redis, gateway, etc.); in that case this
 * single-node fallback is deliberately not created.
 */
@Configuration(proxyBeanMethods = false)
public class RateLimitStoreConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "platform.rate-limit.redis", name = "enabled", havingValue = "true")
    RedisTemplate<String, String> rateLimitRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setConnectionFactory(factory);
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.rate-limit.redis", name = "enabled", havingValue = "true")
    RateLimitStore redisRateLimitStore(RedisTemplate<String, String> template,
                                       org.springframework.core.env.Environment environment) {
        return new RedisRateLimitStore(template,
                environment.getProperty("platform.rate-limit.redis.key-prefix", "geostat:rate-limit"));
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitStore.class)
    RateLimitStore boundedInMemoryRateLimitStore() {
        return new BoundedInMemoryRateLimitStore();
    }
}
