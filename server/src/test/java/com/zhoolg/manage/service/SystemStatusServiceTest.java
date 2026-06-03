package com.zhoolg.manage.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import com.zhoolg.manage.infrastructure.auth.LoginAttemptGuardService;
import com.zhoolg.manage.mapper.AuditLogMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemStatusServiceTest {

    @Test
    void reportsWarnWhenRedisIsNotConfiguredAndDatabaseIsUp() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        Environment environment = mock(Environment.class);
        SystemStatusService service = service(jdbcTemplate, null, dynamicModuleService, environment);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(dynamicModuleService.enabledModules()).thenReturn(List.of(Map.of("id", "orders")));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(environment.getProperty("spring.application.name", "svelte-manage-web-server")).thenReturn("manage");

        var status = service.snapshot();

        assertThat(status.status()).isEqualTo("WARN");
        assertThat(status.database().status()).isEqualTo("UP");
        assertThat(status.redis().status()).isEqualTo("WARN");
        assertThat(status.aiFactory().enabledDynamicModules()).isEqualTo(1);
        assertThat(status.runtime().environment()).isEqualTo("dev");
        assertThat(status.security().activeSessions()).isEqualTo(2);
        assertThat(status.security().recentLoginFailures()).isEqualTo(3);
        assertThat(status.security().lockedIpCount()).isEqualTo(1);
    }

    @Test
    void reportsDownWhenDatabaseProbeFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DynamicModuleService dynamicModuleService = mock(DynamicModuleService.class);
        Environment environment = mock(Environment.class);
        SystemStatusService service = service(jdbcTemplate, null, dynamicModuleService, environment);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenThrow(new IllegalStateException("database offline"));
        when(dynamicModuleService.enabledModules()).thenReturn(List.of());
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(environment.getProperty("spring.application.name", "svelte-manage-web-server")).thenReturn("manage");

        var status = service.snapshot();

        assertThat(status.status()).isEqualTo("DOWN");
        assertThat(status.database().status()).isEqualTo("DOWN");
        assertThat(status.database().message()).contains("database offline");
    }

    @SuppressWarnings("unchecked")
    private SystemStatusService service(
            JdbcTemplate jdbcTemplate,
        StringRedisTemplate redisTemplate,
        DynamicModuleService dynamicModuleService,
        Environment environment
    ) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        IAuthService authService = mock(IAuthService.class);
        LoginAttemptGuardService loginAttemptGuardService = mock(LoginAttemptGuardService.class);
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        when(authService.activeSessionCount()).thenReturn(2L);
        when(loginAttemptGuardService.activeLockedIpCount()).thenReturn(1L);
        when(auditLogMapper.countLoginFailuresSince(org.mockito.ArgumentMatchers.any())).thenReturn(3L);
        return new SystemStatusService(jdbcTemplate, provider, dynamicModuleService, environment, authService, loginAttemptGuardService, auditLogMapper);
    }
}
