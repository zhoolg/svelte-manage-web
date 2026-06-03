package com.zhoolg.manage.entity.dto;

import java.util.List;
import java.util.Map;

public record AiGenerateResponse(
        String taskNo,
        String status,
        Map<String, Object> schema,
        List<GeneratedFile> files
) {
    public record GeneratedFile(String path, String type, String content) {
    }
}
