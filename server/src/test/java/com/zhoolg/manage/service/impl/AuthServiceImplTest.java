package com.zhoolg.manage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.infrastructure.auth.CaptchaService;
import com.zhoolg.manage.infrastructure.auth.CryptoService;
import com.zhoolg.manage.infrastructure.auth.LoginAttemptGuardService;
import com.zhoolg.manage.infrastructure.auth.RateLimitService;
import com.zhoolg.manage.service.AuditLogService;
import com.zhoolg.manage.service.IPermissionService;
import com.zhoolg.manage.service.UserDirectoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @Test
    void prodProfileRequiresHostPrefixedCookieName() {
        AuthServiceImpl service = newService();
        ReflectionTestUtils.setField(service, "profiles", "prod");
        ReflectionTestUtils.setField(service, "cookieSecure", true);
        ReflectionTestUtils.setField(service, "cookieName", "SESSION");

        assertThatThrownBy(service::validateProductionSecurityConfig)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("生产环境 Cookie 名称必须使用 __Host- 前缀，例如 __Host-SESSION");
    }

    @Test
    void prodProfileAcceptsSecureHostPrefixedCookieName() {
        AuthServiceImpl service = newService();
        ReflectionTestUtils.setField(service, "profiles", "prod");
        ReflectionTestUtils.setField(service, "cookieSecure", true);
        ReflectionTestUtils.setField(service, "cookieName", "__Host-SESSION");

        assertThatCode(service::validateProductionSecurityConfig).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    private AuthServiceImpl newService() {
        ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
        when(redisProvider.getIfAvailable()).thenReturn(null);
        return new AuthServiceImpl(
                mock(CryptoService.class),
                mock(PasswordEncoder.class),
                mock(UserDirectoryService.class),
                mock(CaptchaService.class),
                mock(IPermissionService.class),
                mock(AuditLogService.class),
                mock(RateLimitService.class),
                mock(LoginAttemptGuardService.class),
                redisProvider,
                new ObjectMapper()
        );
    }
}
