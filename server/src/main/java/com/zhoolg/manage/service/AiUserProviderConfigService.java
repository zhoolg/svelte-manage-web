package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import com.zhoolg.manage.entity.dto.AiProviderConfigRequest;
import com.zhoolg.manage.entity.dto.AiProviderConfigResponse;
import com.zhoolg.manage.entity.pojo.AiUserProviderConfigDO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.infrastructure.auth.SecretCryptoService;
import com.zhoolg.manage.mapper.AiUserProviderConfigMapper;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class AiUserProviderConfigService {
    private static final Set<String> PROVIDERS = Set.of("template", "openai", "claude");

    private final AiUserProviderConfigMapper mapper;
    private final SecretCryptoService secretCryptoService;

    public AiUserProviderConfigService(
            AiUserProviderConfigMapper mapper,
            SecretCryptoService secretCryptoService
    ) {
        this.mapper = mapper;
        this.secretCryptoService = secretCryptoService;
    }

    public AiProviderConfigResponse get(long userId) {
        AiUserProviderConfigDO entity = mapper.findByUserId(userId);
        if (entity == null) {
            return new AiProviderConfigResponse("template", "", "", false, "");
        }
        return response(entity);
    }

    public AiProviderConfigResponse save(long userId, AiProviderConfigRequest request) {
        String provider = normalizeProvider(request.provider());
        if (provider.isBlank()) {
            provider = "template";
        }
        AiUserProviderConfigDO existing = mapper.findByUserId(userId);
        AiUserProviderConfigDO entity = new AiUserProviderConfigDO();
        entity.setUserId(userId);
        entity.setProvider(provider);
        entity.setModel(text(request.model()));
        entity.setBaseUrl(text(request.baseUrl()));
        if ("template".equals(provider) || request.clearApiKey()) {
            entity.setApiKeyCiphertext("");
        } else if (!text(request.apiKey()).isBlank()) {
            entity.setApiKeyCiphertext(secretCryptoService.encrypt(text(request.apiKey())));
        } else {
            entity.setApiKeyCiphertext(existing == null ? "" : existing.getApiKeyCiphertext());
        }
        mapper.upsert(entity);
        AiUserProviderConfigDO saved = mapper.findByUserId(userId);
        return saved == null ? response(entity) : response(saved);
    }

    public AiGenerateRequest applySavedConfig(long userId, AiGenerateRequest request) {
        String provider = normalizeProvider(request.provider());
        if ("template".equals(provider)) {
            return request;
        }
        AiUserProviderConfigDO saved = mapper.findByUserId(userId);
        if (saved == null) {
            return request;
        }
        if (provider.isBlank()) {
            request.setProvider(saved.getProvider());
            provider = normalizeProvider(saved.getProvider());
        }
        if ("template".equals(provider)) {
            return request;
        }
        if (text(request.getModel()).isBlank()) {
            request.setModel(saved.getModel());
        }
        if (text(request.getBaseUrl()).isBlank()) {
            request.setBaseUrl(saved.getBaseUrl());
        }
        if (text(request.getApiKey()).isBlank() && !text(saved.getApiKeyCiphertext()).isBlank()) {
            request.setApiKey(secretCryptoService.decrypt(saved.getApiKeyCiphertext()));
        }
        return request;
    }

    private AiProviderConfigResponse response(AiUserProviderConfigDO entity) {
        String apiKey = "";
        if (!text(entity.getApiKeyCiphertext()).isBlank()) {
            apiKey = secretCryptoService.decrypt(entity.getApiKeyCiphertext());
        }
        return new AiProviderConfigResponse(
                text(entity.getProvider()).isBlank() ? "template" : entity.getProvider(),
                text(entity.getModel()),
                text(entity.getBaseUrl()),
                !apiKey.isBlank(),
                mask(apiKey)
        );
    }

    private String normalizeProvider(String value) {
        String provider = text(value).toLowerCase(Locale.ROOT);
        if (provider.isBlank()) {
            return "";
        }
        if ("anthropic".equals(provider)) {
            return "claude";
        }
        if (!PROVIDERS.contains(provider)) {
            throw new ApiException(400, "不支持的 AI 协议：" + provider);
        }
        return provider;
    }

    private String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String trimmed = apiKey.trim();
        String suffix = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
        return "****" + suffix;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
