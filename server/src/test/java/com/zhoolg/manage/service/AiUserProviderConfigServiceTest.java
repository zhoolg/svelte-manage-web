package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import com.zhoolg.manage.entity.dto.AiProviderConfigRequest;
import com.zhoolg.manage.entity.dto.AiProviderConfigResponse;
import com.zhoolg.manage.entity.pojo.AiUserProviderConfigDO;
import com.zhoolg.manage.infrastructure.auth.SecretCryptoService;
import com.zhoolg.manage.mapper.AiUserProviderConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiUserProviderConfigServiceTest {
    private final SecretCryptoService cryptoService = new SecretCryptoService("unit-test-secret");

    @Test
    void savesApiKeyEncryptedAndReturnsOnlyMaskedKey() {
        AiUserProviderConfigMapper mapper = mock(AiUserProviderConfigMapper.class);
        AiUserProviderConfigService service = new AiUserProviderConfigService(mapper, cryptoService);

        AiProviderConfigResponse response = service.save(
                1L,
                new AiProviderConfigRequest("openai", "gpt-4o-mini", "https://api.openai.com", "sk-test-1234", false)
        );

        ArgumentCaptor<AiUserProviderConfigDO> captor = ArgumentCaptor.forClass(AiUserProviderConfigDO.class);
        verify(mapper).upsert(captor.capture());
        AiUserProviderConfigDO saved = captor.getValue();
        assertThat(saved.getProvider()).isEqualTo("openai");
        assertThat(saved.getApiKeyCiphertext()).doesNotContain("sk-test-1234");
        assertThat(cryptoService.decrypt(saved.getApiKeyCiphertext())).isEqualTo("sk-test-1234");
        assertThat(response.hasApiKey()).isTrue();
        assertThat(response.maskedApiKey()).isEqualTo("****1234");
    }

    @Test
    void appliesSavedCredentialWhenGenerateRequestOmitsApiKey() {
        AiUserProviderConfigMapper mapper = mock(AiUserProviderConfigMapper.class);
        AiUserProviderConfigDO saved = new AiUserProviderConfigDO();
        saved.setUserId(1L);
        saved.setProvider("claude");
        saved.setModel("claude-3-5-sonnet-latest");
        saved.setBaseUrl("https://api.anthropic.com");
        saved.setApiKeyCiphertext(cryptoService.encrypt("sk-ant-test"));
        when(mapper.findByUserId(1L)).thenReturn(saved);
        AiUserProviderConfigService service = new AiUserProviderConfigService(mapper, cryptoService);
        AiGenerateRequest request = new AiGenerateRequest();
        request.setProvider("claude");

        AiGenerateRequest resolved = service.applySavedConfig(1L, request);

        assertThat(resolved.provider()).isEqualTo("claude");
        assertThat(resolved.model()).isEqualTo("claude-3-5-sonnet-latest");
        assertThat(resolved.baseUrl()).isEqualTo("https://api.anthropic.com");
        assertThat(resolved.apiKey()).isEqualTo("sk-ant-test");
    }
}
