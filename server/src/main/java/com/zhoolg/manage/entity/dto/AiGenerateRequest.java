package com.zhoolg.manage.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 模块生成请求。
 * 同时兼容项目本地格式、OpenAI Chat/Responses 格式和 Claude Messages 格式。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiGenerateRequest {
    private String description;
    private String moduleKey;
    private String moduleName;
    private String businessType;
    private String model;
    private String provider;
    private String apiKey;
    private String baseUrl;
    private JsonNode prompt;
    private JsonNode input;
    private JsonNode system;
    private JsonNode messages;
    private JsonNode metadata;
    private JsonNode module;

    public String description() {
        return firstText(
                description,
                nodeText(prompt),
                nodeText(input),
                messagesText(),
                nodeText(system)
        );
    }

    public String moduleKey() {
        return firstText(
                moduleKey,
                objectText(metadata, "moduleKey"),
                objectText(metadata, "module_key"),
                objectText(module, "key"),
                objectText(module, "moduleKey"),
                objectText(module, "module_key")
        );
    }

    public String moduleName() {
        return firstText(
                moduleName,
                objectText(metadata, "moduleName"),
                objectText(metadata, "module_name"),
                objectText(module, "name"),
                objectText(module, "moduleName"),
                objectText(module, "module_name")
        );
    }

    public String businessType() {
        return firstText(
                businessType,
                objectText(metadata, "businessType"),
                objectText(metadata, "business_type")
        );
    }

    public String model() {
        return model;
    }

    public String provider() {
        return provider;
    }

    public String apiKey() {
        return apiKey;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    @JsonAlias({"module_key"})
    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public String getModuleName() {
        return moduleName;
    }

    @JsonAlias({"module_name"})
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getBusinessType() {
        return businessType;
    }

    @JsonAlias({"business_type"})
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    @JsonAlias({"api_key"})
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    @JsonAlias({"base_url"})
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode getPrompt() {
        return prompt;
    }

    public void setPrompt(JsonNode prompt) {
        this.prompt = prompt;
    }

    public JsonNode getInput() {
        return input;
    }

    public void setInput(JsonNode input) {
        this.input = input;
    }

    public JsonNode getSystem() {
        return system;
    }

    public void setSystem(JsonNode system) {
        this.system = system;
    }

    public JsonNode getMessages() {
        return messages;
    }

    public void setMessages(JsonNode messages) {
        this.messages = messages;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public JsonNode getModule() {
        return module;
    }

    public void setModule(JsonNode module) {
        this.module = module;
    }

    private String messagesText() {
        if (messages == null || !messages.isArray()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (JsonNode message : messages) {
            String role = objectText(message, "role");
            if (role.isBlank() || "user".equals(role) || "system".equals(role)) {
                String content = nodeText(message.get("content"));
                if (!content.isBlank()) {
                    parts.add(content);
                }
            }
        }
        return String.join("\n", parts);
    }

    private String nodeText(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : node) {
                String text = nodeText(item);
                if (!text.isBlank()) {
                    parts.add(text);
                }
            }
            return String.join("\n", parts);
        }
        if (node.isObject()) {
            String text = objectText(node, "text");
            if (!text.isBlank()) {
                return text;
            }
            return nodeText(node.get("content"));
        }
        return "";
    }

    private String objectText(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return "";
        }
        JsonNode value = node.get(fieldName);
        return value != null && value.isTextual() ? value.asText() : "";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
