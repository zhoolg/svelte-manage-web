package com.zhoolg.manage.entity.dto;

public record AiProviderConfigResponse(
        String provider,
        String model,
        String baseUrl,
        boolean hasApiKey,
        String maskedApiKey
) {
}
