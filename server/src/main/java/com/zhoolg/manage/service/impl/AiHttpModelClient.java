package com.zhoolg.manage.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.AiModelGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiHttpModelClient {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.allowed-base-url-hosts:}")
    private String[] allowedBaseUrlHosts;

    public AiHttpModelClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Optional<Map<String, Object>> generateSchema(AiModelGateway.GenerationRequest request, int maxOutputChars) {
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            return Optional.empty();
        }
        String provider = normalizeProvider(request.provider());
        String content = switch (provider) {
            case "openai", "openai-compatible" -> callOpenAi(request);
            case "claude", "anthropic" -> callClaude(request);
            default -> throw new ApiException(400, "不支持的 AI 协议：" + provider);
        };
        if (content == null || content.isBlank()) {
            throw new ApiException(502, "AI 模型返回为空");
        }
        if (content.length() > maxOutputChars) {
            throw new ApiException(502, "AI 模型返回过长");
        }
        return Optional.of(parseJsonObject(content));
    }

    private String callOpenAi(AiModelGateway.GenerationRequest request) {
        JsonNode response = postJson(
                endpoint(request.baseUrl(), "https://api.openai.com", "/v1/chat/completions"),
                Map.of(
                        "model", blankToDefault(request.model(), "gpt-4o-mini"),
                        "temperature", 0.2,
                        "response_format", Map.of("type", "json_object"),
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt()),
                                Map.of("role", "user", "content", userPrompt(request))
                        )
                ),
                Map.of("Authorization", "Bearer " + request.apiKey().trim())
        );
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        return content.isTextual() ? content.asText() : "";
    }

    private String callClaude(AiModelGateway.GenerationRequest request) {
        JsonNode response = postJson(
                endpoint(request.baseUrl(), "https://api.anthropic.com", "/v1/messages"),
                Map.of(
                        "model", blankToDefault(request.model(), "claude-3-5-sonnet-latest"),
                        "max_tokens", 4096,
                        "temperature", 0.2,
                        "system", systemPrompt(),
                        "messages", List.of(Map.of("role", "user", "content", userPrompt(request)))
                ),
                Map.of(
                        "x-api-key", request.apiKey().trim(),
                        "anthropic-version", "2023-06-01"
                )
        );
        JsonNode content = response.path("content");
        if (content.isArray()) {
            for (JsonNode item : content) {
                JsonNode text = item.path("text");
                if (text.isTextual() && !text.asText().isBlank()) {
                    return text.asText();
                }
            }
        }
        return "";
    }

    private JsonNode postJson(String url, Map<String, Object> body, Map<String, String> headers) {
        validateEndpoint(url);
        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON);
            headers.forEach(spec::header);
            return spec.body(body).retrieve().body(JsonNode.class);
        } catch (RuntimeException ex) {
            throw new ApiException(502, "AI 模型接口调用失败");
        }
    }

    private Map<String, Object> parseJsonObject(String content) {
        String json = stripMarkdownFence(content.trim());
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new ApiException(502, "AI 模型返回不是合法 JSON 对象");
        }
    }

    private String stripMarkdownFence(String value) {
        if (!value.startsWith("```")) {
            return value;
        }
        String withoutStart = value.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutStart.replaceFirst("\\s*```$", "").trim();
    }

    private String endpoint(String requestBaseUrl, String defaultBaseUrl, String path) {
        String baseUrl = blankToDefault(requestBaseUrl, defaultBaseUrl).replaceAll("/+$", "");
        if (baseUrl.endsWith("/v1") && path.startsWith("/v1/")) {
            return baseUrl + path.substring(3);
        }
        return baseUrl + path;
    }

    private void validateEndpoint(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "AI Base URL 不合法");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ApiException(400, "AI Base URL 不合法");
        }
        if ("https".equalsIgnoreCase(scheme)) {
            validateAllowedHost(host);
            validatePublicHost(host);
            return;
        }
        boolean localHttp = "http".equalsIgnoreCase(scheme)
                && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
        if (!localHttp) {
            throw new ApiException(400, "AI Base URL 必须使用 HTTPS， localhost 调试除外");
        }
    }

    private void validateAllowedHost(String host) {
        Set<String> allowedHosts = Arrays.stream(allowedBaseUrlHosts == null ? new String[0] : allowedBaseUrlHosts)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> item.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowedHosts.isEmpty() && !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new ApiException(400, "AI Base URL 域名不在允许列表内");
        }
    }

    private void validatePublicHost(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException ex) {
            throw new ApiException(400, "AI Base URL 域名无法解析");
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new ApiException(400, "AI Base URL 不允许指向内网或本机地址");
            }
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return !(first == 100 && second >= 64 && second <= 127);
        }
        return bytes.length != 16 || (bytes[0] & 0xfe) != 0xfc;
    }

    private String systemPrompt() {
        return """
                你是企业后台管理系统架构师。你只能输出一个 JSON 对象，不能输出 Markdown、解释文字或代码块。
                目标：把用户的业务描述转换为可校验的后台模块 schema。
                字段约束：
                - module.key 必须使用调用方提供的模块标识。
                - module.name 使用中文业务名称，module.nameEn 使用英文业务名称。
                - entity.tableName 必须使用 biz_ + 模块标识。
                - fields[].label 使用中文显示名，fields[].labelEn 使用英文显示名。
                - fields[].type 只能是 String、Integer、Long、BigDecimal、LocalDate、LocalDateTime。
                - fields[].formType 只能是 input、textarea、select、number、date、datetime。
                - select 字段必须提供 options，options 元素包含 label、labelEn 和 value。
                - 必须包含 status 字段，除非业务明确不需要状态。
                - 不要设计支付、优惠券、资金结算等交易逻辑。
                - 字段数量控制在高频后台工作流需要的范围内，默认 6-12 个，不要为了显得完整而堆字段。
                - 优先设计能帮助管理员判断和行动的字段：状态、负责人、时间、类型、备注、联系方式、归属对象。
                输出结构：
                {
                  "businessType": "crud-workflow",
                  "module": {"key": "...", "name": "...", "nameEn": "...", "permissions": ["module:view"]},
                  "entity": {
                    "className": "...Entity",
                    "tableName": "biz_...",
                    "fields": [
                      {
                        "name": "lowerCamelCase",
                        "label": "中文显示名",
                        "labelEn": "English Label",
                        "type": "String",
                        "formType": "input",
                        "required": true,
                        "table": true,
                        "searchable": true,
                        "filterable": false
                      }
                    ]
                  },
                  "workflow": [],
                  "warnings": []
                }
                """;
    }

    private String userPrompt(AiModelGateway.GenerationRequest request) {
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

    private String normalizeProvider(String value) {
        String provider = blankToDefault(value, "openai").toLowerCase(Locale.ROOT);
        return "anthropic".equals(provider) ? "claude" : provider;
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
