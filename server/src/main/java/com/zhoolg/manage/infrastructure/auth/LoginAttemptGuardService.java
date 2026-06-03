package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.cache.CacheService;
import com.zhoolg.manage.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录尝试保护：只按 IP 累计密码失败次数，并按阶梯短时封禁。
 * 计数落在 CacheService 中，Redis 可用时可跨实例共享，单机环境自动退回本地缓存。
 */
@Service
public class LoginAttemptGuardService {

    private final CacheService cacheService;
    private final Map<String, Instant> lockedIps = new ConcurrentHashMap<>();

    @Value("${app.auth.failure-window-minutes:180}")
    private long failureWindowMinutes;

    public LoginAttemptGuardService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void ensureAllowed(String ip) {
        String normalizedIp = normalizeIp(ip);
        cacheService.get(ipLockKey(normalizedIp)).ifPresent(lockoutMinutes -> {
            throw new ApiException(429, "当前 IP 登录失败次数过多，已锁定 " + lockoutMinutes(lockoutMinutes) + " 分钟，请稍后再试");
        });
        lockedIps.remove(normalizedIp);
    }

    public void recordPasswordFailure(String ip) {
        String normalizedIp = normalizeIp(ip);
        Duration window = Duration.ofMinutes(failureWindowMinutes);
        long ipFailures = cacheService.increment(ipFailureKey(normalizedIp), window);
        long lockoutMinutes = lockoutMinutes(ipFailures);
        if (lockoutMinutes > 0) {
            cacheService.set(ipLockKey(normalizedIp), String.valueOf(lockoutMinutes), Duration.ofMinutes(lockoutMinutes));
            lockedIps.put(normalizedIp, Instant.now().plus(Duration.ofMinutes(lockoutMinutes)));
        }
    }

    public void resetIpFailures(String ip) {
        String normalizedIp = normalizeIp(ip);
        cacheService.delete(ipFailureKey(normalizedIp));
        cacheService.delete(ipLockKey(normalizedIp));
        lockedIps.remove(normalizedIp);
    }

    public long activeLockedIpCount() {
        Instant now = Instant.now();
        lockedIps.entrySet().removeIf(entry -> entry.getValue().isBefore(now) || !cacheService.exists(ipLockKey(entry.getKey())));
        return lockedIps.size();
    }

    private long lockoutMinutes(long failures) {
        if (failures >= 20) {
            return 180;
        }
        if (failures >= 15) {
            return 60;
        }
        if (failures >= 10) {
            return 20;
        }
        if (failures >= 5) {
            return 3;
        }
        if (failures >= 3) {
            return 1;
        }
        return 0;
    }

    private long lockoutMinutes(String cachedValue) {
        try {
            long minutes = Long.parseLong(cachedValue);
            return minutes > 0 ? minutes : 1;
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String ipFailureKey(String ip) {
        return "auth:password-fail:ip:" + normalizeIp(ip);
    }

    private String ipLockKey(String ip) {
        return "auth:password-lock:ip:" + normalizeIp(ip);
    }

    private String normalizeIp(String ip) {
        return ip == null || ip.isBlank() ? "unknown" : ip.trim();
    }
}
