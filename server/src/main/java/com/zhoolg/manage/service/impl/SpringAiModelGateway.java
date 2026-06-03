package com.zhoolg.manage.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.AiModelGateway;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "app.ai.spring-ai", name = "enabled", havingValue = "true")
public class SpringAiModelGateway implements AiModelGateway {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;
    private final AiHttpModelClient aiHttpModelClient;

    @Value("${app.ai.spring-ai.max-output-chars:12000}")
    private int maxOutputChars;

    public SpringAiModelGateway(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectMapper objectMapper,
            AiHttpModelClient aiHttpModelClient
    ) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.objectMapper = objectMapper;
        this.aiHttpModelClient = aiHttpModelClient;
    }

    @Override
    public Optional<Map<String, Object>> generateSchema(GenerationRequest request) {
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            return aiHttpModelClient.generateSchema(request, maxOutputChars);
        }
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new ApiException(500, "Spring AI 已启用但未配置可用 ChatClient");
        }

        String content;
        try {
            content = builder.build()
                    .prompt()
                    .system(systemPrompt())
                    .user(userPrompt(request))
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            throw new ApiException(500, "Spring AI 模型调用失败");
        }

        if (content == null || content.isBlank()) {
            throw new ApiException(500, "Spring AI 模型返回为空");
        }
        if (content.length() > maxOutputChars) {
            throw new ApiException(500, "Spring AI 模型返回过长");
        }
        return Optional.of(parseJsonObject(content));
    }

    private Map<String, Object> parseJsonObject(String content) {
        String json = stripMarkdownFence(content.trim());
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new ApiException(500, "Spring AI 模型返回不是合法 JSON 对象");
        }
    }

    private String stripMarkdownFence(String value) {
        if (!value.startsWith("```")) {
            return value;
        }
        String withoutStart = value.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutStart.replaceFirst("\\s*```$", "").trim();
    }

    private String systemPrompt() {
        return """
                你是企业后台管理系统架构师。你只能输出一个 JSON 对象，不能输出 Markdown、解释文字或代码块。
                目标：把用户的业务描述转换为可校验的后台模块 schema。
                字段约束：
                - module.key 必须使用调用方提供的模块标识。
                - entity.tableName 必须使用 biz_ + 模块标识。
                - fields[].type 只能是 String、Integer、Long、BigDecimal、LocalDate、LocalDateTime。
                - fields[].formType 只能是 input、textarea、select、number、date、datetime。
                - select 字段必须提供 options，options 元素包含 label 和 value。
                - 必须包含 status 字段，除非业务明确不需要状态。
                - 不要设计支付、优惠券、资金结算等交易逻辑。
                输出结构：
                {
                  "businessType": "crud-workflow",
                  "module": {"key": "...", "name": "...", "permissions": ["module:view"]},
                  "entity": {"className": "...Entity", "tableName": "biz_...", "fields": []},
                  "workflow": [],
                  "warnings": []
                }
                """;
    }

    private String userPrompt(GenerationRequest request) {
        return """
                模块标识：%s
                模块名称：%s
                业务类型：%s
                模型提供方：%s
                模型名称：%s
                业务描述：
                %s
                """.formatted(
                request.moduleKey(),
                request.moduleName(),
                blankToDefault(request.businessType(), "crud-workflow"),
                blankToDefault(request.provider(), "default"),
                blankToDefault(request.model(), "default"),
                request.description()
        );
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
