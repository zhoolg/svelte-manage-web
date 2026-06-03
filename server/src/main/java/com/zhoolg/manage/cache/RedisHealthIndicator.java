package com.zhoolg.manage.cache;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "app.cache", name = "redis-enabled", havingValue = "true")
public class RedisHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redisTemplate;

    public RedisHealthIndicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try (RedisConnection connection = redisTemplate.getRequiredConnectionFactory().getConnection()) {
            String pong = connection.ping();
            if (pong != null && pong.equalsIgnoreCase("PONG")) {
                return Health.up().withDetail("ping", pong).build();
            }
            return Health.unknown().withDetail("ping", pong).build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
