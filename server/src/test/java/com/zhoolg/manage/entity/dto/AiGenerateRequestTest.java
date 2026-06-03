package com.zhoolg.manage.entity.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiGenerateRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void supportsLocalRequestShape() throws Exception {
        String json = """
                {
                  "description": "生成合同管理模块",
                  "moduleKey": "contract",
                  "moduleName": "合同管理",
                  "businessType": "crud-workflow"
                }
                """;

        AiGenerateRequest request = objectMapper.readValue(json, AiGenerateRequest.class);

        assertThat(request.description()).isEqualTo("生成合同管理模块");
        assertThat(request.moduleKey()).isEqualTo("contract");
        assertThat(request.moduleName()).isEqualTo("合同管理");
        assertThat(request.businessType()).isEqualTo("crud-workflow");
    }

    @Test
    void supportsOpenAiChatMessagesShape() throws Exception {
        String json = """
                {
                  "model": "gpt-4.1",
                  "messages": [
                    {"role": "system", "content": "你是后台模块生成助手"},
                    {"role": "user", "content": [
                      {"type": "text", "text": "生成报修管理模块，支持派单和完工"}
                    ]}
                  ],
                  "metadata": {
                    "module_key": "repair_order",
                    "module_name": "报修管理",
                    "business_type": "crud-workflow"
                  }
                }
                """;

        AiGenerateRequest request = objectMapper.readValue(json, AiGenerateRequest.class);

        assertThat(request.description()).contains("生成报修管理模块，支持派单和完工");
        assertThat(request.moduleKey()).isEqualTo("repair_order");
        assertThat(request.moduleName()).isEqualTo("报修管理");
        assertThat(request.businessType()).isEqualTo("crud-workflow");
    }

    @Test
    void supportsClaudeMessagesShape() throws Exception {
        String json = """
                {
                  "model": "claude-3-5-sonnet-latest",
                  "provider": "claude",
                  "api_key": "sk-ant-test",
                  "base_url": "https://api.anthropic.com",
                  "system": "你是业务建模助手",
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {"type": "text", "text": "我要做一个客户回访模块"}
                      ]
                    }
                  ],
                  "module": {
                    "key": "customer_revisit",
                    "name": "客户回访"
                  }
                }
                """;

        AiGenerateRequest request = objectMapper.readValue(json, AiGenerateRequest.class);

        assertThat(request.description()).contains("客户回访模块");
        assertThat(request.moduleKey()).isEqualTo("customer_revisit");
        assertThat(request.moduleName()).isEqualTo("客户回访");
        assertThat(request.provider()).isEqualTo("claude");
        assertThat(request.apiKey()).isEqualTo("sk-ant-test");
        assertThat(request.baseUrl()).isEqualTo("https://api.anthropic.com");
    }
}
