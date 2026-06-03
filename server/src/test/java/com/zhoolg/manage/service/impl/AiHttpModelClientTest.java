package com.zhoolg.manage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.AiModelGateway;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiHttpModelClientTest {

    @Test
    void rejectsPrivateAiBaseUrlBeforeCallingProvider() {
        AiHttpModelClient client = new AiHttpModelClient(RestClient.builder(), new ObjectMapper());

        assertThatThrownBy(() -> client.generateSchema(request("https://127.0.0.1"), 12000))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("内网或本机地址");
    }

    @Test
    void rejectsHostOutsideConfiguredAllowList() {
        AiHttpModelClient client = new AiHttpModelClient(RestClient.builder(), new ObjectMapper());
        ReflectionTestUtils.setField(client, "allowedBaseUrlHosts", new String[]{"api.openai.com"});

        assertThatThrownBy(() -> client.generateSchema(request("https://example.com"), 12000))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("允许列表");
    }

    private AiModelGateway.GenerationRequest request(String baseUrl) {
        return new AiModelGateway.GenerationRequest(
                "客户管理",
                "customer",
                "客户管理",
                "crud-workflow",
                "openai",
                "gpt-4o-mini",
                "sk-test",
                baseUrl
        );
    }
}
