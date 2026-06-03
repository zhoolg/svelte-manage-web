package com.zhoolg.manage.entity.dto;

import java.util.List;
import java.util.Map;

public record AiPreviewResponse(
        String taskNo,
        String status,
        String moduleKey,
        String moduleName,
        Map<String, Object> schema,
        Map<String, Object> metadata,
        List<AiGenerateResponse.GeneratedFile> files,
        AiValidationReport validation
) {
}
