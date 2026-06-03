package com.zhoolg.manage.entity.dto;

import java.time.LocalDateTime;

public record AiGenerationTaskSummary(
        String taskNo,
        String moduleKey,
        String moduleName,
        String status,
        String smokeTestStatus,
        LocalDateTime smokeTestTime,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
