package com.zhoolg.manage.entity.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SystemStatusDTO(
        String status,
        Instant checkedAt,
        RuntimeInfo runtime,
        ResourceInfo resources,
        HealthComponent database,
        HealthComponent redis,
        AiFactoryInfo aiFactory,
        SecurityInfo security,
        List<StatusCheck> checks
) {
    public record RuntimeInfo(
            String application,
            String environment,
            String springBootVersion,
            String javaVersion,
            String os,
            Instant startedAt,
            long uptimeSeconds
    ) {
    }

    public record ResourceInfo(
            int processors,
            double systemCpuLoad,
            double processCpuLoad,
            long heapUsedBytes,
            long heapMaxBytes,
            long diskUsedBytes,
            long diskTotalBytes
    ) {
    }

    public record HealthComponent(
            String status,
            String message,
            long latencyMs,
            Map<String, Object> details
    ) {
    }

    public record AiFactoryInfo(
            String status,
            int enabledDynamicModules,
            String message
    ) {
    }

    public record SecurityInfo(
            long activeSessions,
            long recentLoginFailures,
            long lockedIpCount,
            Instant lastAdminLoginTime
    ) {
    }

    public record StatusCheck(
            String key,
            String label,
            String status,
            String message,
            long latencyMs
    ) {
    }
}
