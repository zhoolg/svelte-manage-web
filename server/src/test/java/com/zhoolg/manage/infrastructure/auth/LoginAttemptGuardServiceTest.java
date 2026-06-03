package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.cache.CacheService;
import com.zhoolg.manage.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptGuardServiceTest {

    @Test
    void doesNotLockAccountWhenDifferentIpsFailSameAccount() {
        LoginAttemptGuardService guard = guard();

        guard.recordPasswordFailure("127.0.0.1");
        guard.recordPasswordFailure("127.0.0.2");
        guard.recordPasswordFailure("127.0.0.3");

        guard.ensureAllowed("127.0.0.1");
        guard.ensureAllowed("127.0.0.2");
        guard.ensureAllowed("127.0.0.3");
    }

    @Test
    void locksIpAfterThreePasswordFailures() {
        LoginAttemptGuardService guard = guard();

        guard.recordPasswordFailure("127.0.0.1");
        guard.recordPasswordFailure("127.0.0.1");
        guard.ensureAllowed("127.0.0.1");
        guard.recordPasswordFailure("127.0.0.1");

        assertThatThrownBy(() -> guard.ensureAllowed("127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("IP 登录失败次数过多")
                .hasMessageContaining("已锁定 1 分钟");
    }

    @Test
    void lockMessageReportsEscalatedLockoutMinutes() {
        LoginAttemptGuardService guard = guard();

        for (int i = 0; i < 5; i++) {
            guard.recordPasswordFailure("127.0.0.1");
        }

        assertThatThrownBy(() -> guard.ensureAllowed("127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已锁定 3 分钟");
    }

    @Test
    void resetIpFailuresClearsIpLock() {
        LoginAttemptGuardService guard = guard();

        guard.recordPasswordFailure("127.0.0.1");
        guard.recordPasswordFailure("127.0.0.1");
        guard.recordPasswordFailure("127.0.0.1");
        guard.resetIpFailures("127.0.0.1");

        guard.ensureAllowed("127.0.0.1");
    }

    private LoginAttemptGuardService guard() {
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        CacheService cacheService = new CacheService(factory.getBeanProvider(StringRedisTemplate.class));
        LoginAttemptGuardService guard = new LoginAttemptGuardService(cacheService);
        ReflectionTestUtils.setField(guard, "failureWindowMinutes", 180L);
        return guard;
    }
}
