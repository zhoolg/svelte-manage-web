package com.zhoolg.manage.entity.dto;

public record AiProviderConfigRequest(
        String provider,
        String model,
        String baseUrl,
        String apiKey,
        boolean clearApiKey
) {
}
