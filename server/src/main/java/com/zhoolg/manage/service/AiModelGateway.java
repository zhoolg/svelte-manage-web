package com.zhoolg.manage.service;

import java.util.Map;
import java.util.Optional;

/**
 * AI 模型网关。
 * 职责：把自然语言业务需求转换为项目可校验的结构化模块 schema。
 */
public interface AiModelGateway {
    Optional<Map<String, Object>> generateSchema(GenerationRequest request);

    record GenerationRequest(
            String description,
            String moduleKey,
            String moduleName,
            String businessType,
            String provider,
            String model,
            String apiKey,
            String baseUrl
    ) {
    }
}
