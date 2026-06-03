package com.zhoolg.manage.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import com.zhoolg.manage.entity.dto.SystemStatusDTO;
import com.zhoolg.manage.infrastructure.auth.LoginAttemptGuardService;
import com.zhoolg.manage.mapper.AuditLogMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemStatusService {

    private final Instant startedAt = Instant.now();
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final DynamicModuleService dynamicModuleService;
    private final Environment environment;
    private final IAuthService authService;
    private final LoginAttemptGuardService loginAttemptGuardService;
    private final AuditLogMapper auditLogMapper;

    public SystemStatusService(
            JdbcTemplate jdbcTemplate,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            DynamicModuleService dynamicModuleService,
            Environment environment,
            IAuthService authService,
            LoginAttemptGuardService loginAttemptGuardService,
            AuditLogMapper auditLogMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.dynamicModuleService = dynamicModuleService;
        this.environment = environment;
        this.authService = authService;
        this.loginAttemptGuardService = loginAttemptGuardService;
        this.auditLogMapper = auditLogMapper;
    }

    public SystemStatusDTO snapshot() {
        SystemStatusDTO.HealthComponent database = databaseHealth();
        SystemStatusDTO.HealthComponent redis = redisHealth();
        SystemStatusDTO.AiFactoryInfo aiFactory = aiFactoryInfo();
        List<SystemStatusDTO.StatusCheck> checks = List.of(
                check("database", "数据库", database),
                check("redis", "Redis", redis),
                new SystemStatusDTO.StatusCheck("aiFactory", "AI 工厂", aiFactory.status(), aiFactory.message(), 0)
        );
        return new SystemStatusDTO(
                overallStatus(checks),
                Instant.now(),
                runtimeInfo(),
                resourceInfo(),
                database,
                redis,
                aiFactory,
                securityInfo(),
                checks
        );
    }

    private SystemStatusDTO.SecurityInfo securityInfo() {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        LocalDateTime lastLogin = auditLogMapper.latestAdminLoginTime();
        return new SystemStatusDTO.SecurityInfo(
                authService.activeSessionCount(),
                auditLogMapper.countLoginFailuresSince(since),
                loginAttemptGuardService.activeLockedIpCount(),
                lastLogin == null ? null : lastLogin.atZone(ZoneId.systemDefault()).toInstant()
        );
    }

    private SystemStatusDTO.RuntimeInfo runtimeInfo() {
        String[] profiles = environment.getActiveProfiles();
        String environmentName = profiles.length == 0 ? "dev" : String.join(",", profiles);
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version") + " / " + System.getProperty("os.arch");
        return new SystemStatusDTO.RuntimeInfo(
                environment.getProperty("spring.application.name", "svelte-manage-web-server"),
                environmentName,
                SpringBootVersion.getVersion(),
                System.getProperty("java.version"),
                os,
                startedAt,
                Duration.between(startedAt, Instant.now()).toSeconds()
        );
    }

    private SystemStatusDTO.ResourceInfo resourceInfo() {
        Runtime runtime = Runtime.getRuntime();
        File root = new File(".").getAbsoluteFile();
        long heapUsed = runtime.totalMemory() - runtime.freeMemory();
        long diskTotal = root.getTotalSpace();
        long diskFree = root.getFreeSpace();
        double systemCpuLoad = -1;
        double processCpuLoad = -1;
        var bean = ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof com.sun.management.OperatingSystemMXBean osBean) {
            systemCpuLoad = normalizeLoad(osBean.getCpuLoad());
            processCpuLoad = normalizeLoad(osBean.getProcessCpuLoad());
        }
        return new SystemStatusDTO.ResourceInfo(
                runtime.availableProcessors(),
                systemCpuLoad,
                processCpuLoad,
                heapUsed,
                runtime.maxMemory(),
                Math.max(0, diskTotal - diskFree),
                diskTotal
        );
    }

    private SystemStatusDTO.HealthComponent databaseHealth() {
        Instant start = Instant.now();
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("result", result == null ? 0 : result);
            DataSource dataSource = jdbcTemplate.getDataSource();
            if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
                HikariPoolMXBean pool = hikari.getHikariPoolMXBean();
                details.put("activeConnections", pool.getActiveConnections());
                details.put("idleConnections", pool.getIdleConnections());
                details.put("totalConnections", pool.getTotalConnections());
                details.put("threadsAwaitingConnection", pool.getThreadsAwaitingConnection());
            }
            return new SystemStatusDTO.HealthComponent("UP", "数据库连接正常", elapsed(start), details);
        } catch (Exception ex) {
            return new SystemStatusDTO.HealthComponent("DOWN", safeMessage(ex), elapsed(start), Map.of());
        }
    }

    private SystemStatusDTO.HealthComponent redisHealth() {
        Instant start = Instant.now();
        if (redisTemplate == null) {
            return new SystemStatusDTO.HealthComponent("WARN", "Redis 未配置，当前使用本地兜底能力", 0, Map.of());
        }
        try (RedisConnection connection = redisTemplate.getRequiredConnectionFactory().getConnection()) {
            String pong = connection.ping();
            String status = "PONG".equalsIgnoreCase(pong) ? "UP" : "WARN";
            return new SystemStatusDTO.HealthComponent(status, "Redis ping: " + pong, elapsed(start), Map.of("ping", pong == null ? "" : pong));
        } catch (Exception ex) {
            return new SystemStatusDTO.HealthComponent("DOWN", safeMessage(ex), elapsed(start), Map.of());
        }
    }

    private SystemStatusDTO.AiFactoryInfo aiFactoryInfo() {
        try {
            int enabledModules = dynamicModuleService.enabledModules().size();
            return new SystemStatusDTO.AiFactoryInfo("UP", enabledModules, "AI 动态模块元数据可读取");
        } catch (Exception ex) {
            return new SystemStatusDTO.AiFactoryInfo("WARN", 0, "AI 动态模块读取异常：" + safeMessage(ex));
        }
    }

    private SystemStatusDTO.StatusCheck check(String key, String label, SystemStatusDTO.HealthComponent component) {
        return new SystemStatusDTO.StatusCheck(key, label, component.status(), component.message(), component.latencyMs());
    }

    private String overallStatus(List<SystemStatusDTO.StatusCheck> checks) {
        if (checks.stream().anyMatch(check -> "DOWN".equals(check.status()))) {
            return "DOWN";
        }
        if (checks.stream().anyMatch(check -> "WARN".equals(check.status()))) {
            return "WARN";
        }
        return "UP";
    }

    private long elapsed(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }

    private double normalizeLoad(double value) {
        return value < 0 ? -1 : Math.round(value * 10000.0) / 10000.0;
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
