package com.zhoolg.manage.cache;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService {
    private final StringRedisTemplate redisTemplate;
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable;

    @Value("${app.cache.redis-enabled:false}")
    private boolean redisEnabled;

    public CacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.redisAvailable = this.redisTemplate != null;
    }

    @PostConstruct
    void verifyConnection() {
        if (!hasRedis()) {
            return;
        }
        try (RedisConnection connection = redisTemplate.getRequiredConnectionFactory().getConnection()) {
            connection.ping();
            redisAvailable = true;
        } catch (Exception ex) {
            // Redis 不可用时退回本地缓存，保证单机部署和测试环境可启动。
            redisAvailable = false;
        }
    }

    public Optional<String> get(String key) {
        if (!hasRedis()) {
            return Optional.ofNullable(readLocal(key));
        }
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (Exception ex) {
            redisAvailable = false;
            return Optional.ofNullable(readLocal(key));
        }
    }

    public void set(String key, String value, Duration ttl) {
        if (!hasRedis()) {
            writeLocal(key, value, ttl);
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ex) {
            redisAvailable = false;
            writeLocal(key, value, ttl);
        }
    }

    public void delete(String key) {
        if (!hasRedis()) {
            localCache.remove(key);
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            redisAvailable = false;
            localCache.remove(key);
        }
    }

    public long increment(String key, Duration ttl) {
        if (!hasRedis()) {
            return incrementLocal(key, ttl);
        }
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L && ttl != null) {
                redisTemplate.expire(key, ttl);
            }
            return value == null ? 0L : value;
        } catch (Exception ex) {
            redisAvailable = false;
            return incrementLocal(key, ttl);
        }
    }

    public boolean exists(String key) {
        if (!hasRedis()) {
            return readLocal(key) != null;
        }
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception ex) {
            redisAvailable = false;
            return readLocal(key) != null;
        }
    }

    private boolean hasRedis() {
        return redisEnabled && redisAvailable && redisTemplate != null;
    }

    private String readLocal(String key) {
        CacheEntry entry = localCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt() != null && entry.expiresAt().isBefore(Instant.now())) {
            localCache.remove(key);
            return null;
        }
        return entry.value();
    }

    private void writeLocal(String key, String value, Duration ttl) {
        Instant expiresAt = ttl == null ? null : Instant.now().plus(ttl);
        localCache.put(key, new CacheEntry(value, expiresAt));
    }

    private long incrementLocal(String key, Duration ttl) {
        CacheEntry entry = localCache.compute(key, (ignored, current) -> {
            long nextValue = 1L;
            Instant expiresAt = ttl == null ? null : Instant.now().plus(ttl);
            if (current != null && (current.expiresAt() == null || current.expiresAt().isAfter(Instant.now()))) {
                nextValue = Long.parseLong(current.value()) + 1L;
                expiresAt = current.expiresAt();
            }
            return new CacheEntry(String.valueOf(nextValue), expiresAt);
        });
        return Long.parseLong(entry.value());
    }

    private record CacheEntry(String value, Instant expiresAt) {
    }
}
