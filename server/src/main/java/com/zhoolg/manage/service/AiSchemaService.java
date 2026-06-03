package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiGenerateRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiSchemaService {
    private final AiModuleTemplateService templateService;
    private final AiModelGateway aiModelGateway;

    public AiSchemaService(AiModuleTemplateService templateService, AiModelGateway aiModelGateway) {
        this.templateService = templateService;
        this.aiModelGateway = aiModelGateway;
    }

    public Map<String, Object> buildSchema(
            AiRequirementService.Requirement requirement,
            AiGenerateRequest request
    ) {
        var generationRequest = new AiModelGateway.GenerationRequest(
                requirement.description(),
                requirement.moduleKey(),
                requirement.moduleName(),
                request.businessType(),
                request.provider(),
                request.model(),
                request.apiKey(),
                request.baseUrl()
        );
        var modelSchema = aiModelGateway.generateSchema(generationRequest);
        if (modelSchema.isPresent()) {
            return normalizeModelSchema(modelSchema.get(), requirement, request);
        }
        return buildTemplateSchema(requirement, request);
    }

    private Map<String, Object> buildTemplateSchema(
            AiRequirementService.Requirement requirement,
            AiGenerateRequest request
    ) {
        String moduleKey = requirement.moduleKey();
        AiModuleTemplateService.TemplateDefinition template = templateService.selectTemplate(
                moduleKey,
                requirement.description()
        );
        String businessType = request.businessType();

        return Map.of(
                "source", "ai-draft",
                "template", template.metadata(),
                "businessType", businessType == null || businessType.isBlank() ? "crud-workflow" : businessType,
                "module", Map.of(
                        "key", moduleKey,
                        "name", requirement.moduleName(),
                        "nameEn", humanizeIdentifier(moduleKey),
                        "permissions", defaultPermissions(moduleKey)
                ),
                "entity", Map.of(
                        "className", upperCamel(moduleKey) + "Entity",
                        "tableName", "biz_" + moduleKey,
                        "fields", template.fields()
                ),
                "workflow", template.workflow(),
                "originalPrompt", requirement.description()
        );
    }

    private Map<String, Object> normalizeModelSchema(
            Map<String, Object> rawSchema,
            AiRequirementService.Requirement requirement,
            AiGenerateRequest request
    ) {
        String moduleKey = requirement.moduleKey();
        AiModuleTemplateService.TemplateDefinition template = templateService.selectTemplate(
                moduleKey,
                requirement.description()
        );
        Map<String, Object> entity = objectMap(rawSchema.get("entity"));
        List<Map<String, Object>> rawFields = objectList(entity.get("fields"));
        List<Map<String, Object>> fields = rawFields.isEmpty()
                ? template.fields()
                : rawFields.stream().map(this::normalizeField).toList();
        List<Map<String, Object>> workflow = objectList(rawSchema.get("workflow"));
        if (workflow.isEmpty()) {
            workflow = template.workflow();
        }

        String businessType = firstText(
                text(rawSchema.get("businessType")),
                request.businessType(),
                "crud-workflow"
        );
        Map<String, Object> rawModule = objectMap(rawSchema.get("module"));

        return Map.of(
                "source", "spring-ai",
                "template", template.metadata(),
                "businessType", businessType,
                "module", Map.of(
                        "key", moduleKey,
                        "name", requirement.moduleName(),
                        "nameEn", firstText(text(rawModule.get("nameEn")), text(rawModule.get("labelEn")), humanizeIdentifier(moduleKey)),
                        "permissions", defaultPermissions(moduleKey)
                ),
                "entity", Map.of(
                        "className", upperCamel(moduleKey) + "Entity",
                        "tableName", "biz_" + moduleKey,
                        "fields", fields
                ),
                "workflow", workflow,
                "warnings", objectList(rawSchema.get("warnings")),
                "originalPrompt", requirement.description()
        );
    }

    private Map<String, Object> normalizeField(Map<String, Object> rawField) {
        Map<String, Object> field = new LinkedHashMap<>();
        String name = normalizeFieldName(firstText(text(rawField.get("name")), "title"));
        String label = firstText(text(rawField.get("label")), name);
        String type = normalizeFieldType(text(rawField.get("type")));
        String formType = normalizeFormType(text(rawField.get("formType")), type);
        field.put("name", name);
        field.put("label", label);
        field.put("labelEn", firstText(text(rawField.get("labelEn")), text(rawField.get("nameEn")), humanizeIdentifier(name)));
        field.put("type", type);
        field.put("formType", formType);
        field.put("required", boolValue(rawField.get("required"), false));
        field.put("table", boolValue(rawField.get("table"), true));
        field.put("searchable", boolValue(rawField.get("searchable"), false));
        field.put("filterable", boolValue(rawField.get("filterable"), "select".equals(formType)));
        if (boolValue(rawField.get("readonly"), false)) {
            field.put("readonly", true);
        }
        List<Map<String, Object>> options = normalizeOptions(rawField.get("options"));
        if (!options.isEmpty()) {
            field.put("options", options);
            field.put("defaultValue", firstText(text(rawField.get("defaultValue")), text(options.get(0).get("value"))));
        }
        return field;
    }

    private List<String> defaultPermissions(String moduleKey) {
        return List.of(
                moduleKey + ":view",
                moduleKey + ":add",
                moduleKey + ":edit",
                moduleKey + ":delete",
                moduleKey + ":export"
        );
    }

    private String upperCamel(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("_")) {
            if (part.isBlank()) {
                continue;
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                result.append(part.substring(1));
            }
        }
        return result.toString();
    }

    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> map = objectMap(item);
            if (!map.isEmpty()) {
                result.add(map);
            }
        }
        return result;
    }

    private List<Map<String, Object>> normalizeOptions(Object value) {
        List<Map<String, Object>> options = objectList(value);
        if (options.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = options.get(i);
            String label = firstText(text(option.get("label")), text(option.get("name")));
            String optionValue = firstText(text(option.get("value")), "option_" + (i + 1));
            if (!label.isBlank()) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                normalized.put("label", label);
                normalized.put("labelEn", firstText(text(option.get("labelEn")), text(option.get("nameEn")), humanizeIdentifier(optionValue)));
                normalized.put("value", optionValue);
                result.add(normalized);
            }
        }
        return result;
    }

    private String normalizeFieldName(String value) {
        String normalized = value == null ? "" : value.trim()
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (normalized.isBlank()) {
            return "title";
        }
        String[] parts = normalized.split("_");
        StringBuilder result = new StringBuilder(parts[0].substring(0, 1).toLowerCase(Locale.ROOT) + parts[0].substring(1));
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                result.append(parts[i].substring(0, 1).toUpperCase(Locale.ROOT));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1));
                }
            }
        }
        return result.toString();
    }

    private String humanizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[_\\-]+", " ")
                .trim();
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
    }

    private String normalizeFieldType(String value) {
        return switch (value) {
            case "Integer", "Long", "BigDecimal", "LocalDate", "LocalDateTime" -> value;
            default -> "String";
        };
    }

    private String normalizeFormType(String value, String fieldType) {
        return switch (value) {
            case "textarea", "select", "number", "date", "datetime" -> value;
            default -> switch (fieldType) {
                case "Integer", "Long", "BigDecimal" -> "number";
                case "LocalDate" -> "date";
                case "LocalDateTime" -> "datetime";
                default -> "input";
            };
        };
    }

    private boolean boolValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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
