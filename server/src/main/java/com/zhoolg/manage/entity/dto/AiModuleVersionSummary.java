package com.zhoolg.manage.entity.dto;

import java.time.LocalDateTime;

public record AiModuleVersionSummary(
        String moduleKey,
        String moduleName,
        Integer versionNo,
        String taskNo,
        String schemaHash,
        boolean current,
        LocalDateTime createTime
) {
}
