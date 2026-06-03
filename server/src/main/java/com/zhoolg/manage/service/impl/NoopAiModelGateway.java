package com.zhoolg.manage.service.impl;

import com.zhoolg.manage.service.AiModelGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "app.ai.spring-ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopAiModelGateway implements AiModelGateway {
    private final AiHttpModelClient aiHttpModelClient;

    @Value("${app.ai.spring-ai.max-output-chars:12000}")
    private int maxOutputChars;

    public NoopAiModelGateway(AiHttpModelClient aiHttpModelClient) {
        this.aiHttpModelClient = aiHttpModelClient;
    }

    @Override
    public Optional<Map<String, Object>> generateSchema(GenerationRequest request) {
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            return aiHttpModelClient.generateSchema(request, maxOutputChars);
        }
        return Optional.empty();
    }
}
