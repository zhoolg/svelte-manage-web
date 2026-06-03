package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.cache.CacheService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {
    private final CacheService cacheService;

    public RateLimitService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public boolean overLimit(String key, int maxAttempts, Duration window) {
        long count = cacheService.increment(key, window);
        return count > maxAttempts;
    }
}
