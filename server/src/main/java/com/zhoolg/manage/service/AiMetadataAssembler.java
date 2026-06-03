package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiMetadataAssembler {
    public Map<String, Object> previewMetadata(
            AiGenerationTaskDO entity,
            Map<String, Object> schema,
            AiValidationReport validation
    ) {
        if (!validation.passed()) {
            return null;
        }
        return toFrontendModuleMetadata(entity, schema);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> toFrontendModuleMetadata(AiGenerationTaskDO entity, Map<String, Object> schema) {
        Map<String, Object> module = (Map<String, Object>) schema.get("module");
        Map<String, Object> model = (Map<String, Object>) schema.get("entity");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) model.get("fields");
        String moduleKey = entity.getModuleKey();

        List<Map<String, Object>> columns = fields.stream()
                .filter(field -> !Boolean.FALSE.equals(field.get("table")))
                .map(this::frontendColumn)
                .toList();
        List<Map<String, Object>> search = fields.stream()
                .filter(field -> Boolean.TRUE.equals(field.get("searchable")) || Boolean.TRUE.equals(field.get("filterable")))
                .map(this::frontendSearch)
                .toList();
        List<Map<String, Object>> form = fields.stream()
                .filter(field -> !"createTime".equals(field.get("name")))
                .filter(field -> !Boolean.TRUE.equals(field.get("readonly")))
                .map(this::frontendForm)
                .toList();
        String moduleLabel = firstText(text(module.get("name")), entity.getModuleName(), moduleKey);
        String moduleEnglishLabel = firstText(
                text(module.get("nameEn")),
                text(module.get("labelEn")),
                text(module.get("englishName")),
                humanizeIdentifier(moduleKey)
        );
        Object permissions = module.get("permissions");
        if (!(permissions instanceof List<?> list) || list.isEmpty()) {
            permissions = List.of(
                    moduleKey + ":view",
                    moduleKey + ":add",
                    moduleKey + ":edit",
                    moduleKey + ":delete",
                    moduleKey + ":export"
            );
        }

        Map<String, Object> crud = new LinkedHashMap<>();
        crud.put("title", entity.getModuleName());
        crud.put("titleI18n", labelI18n(entity.getModuleName(), moduleEnglishLabel));
        crud.put("apiBase", "/" + moduleKey);
        crud.put("restBase", "/rest/" + moduleKey);
        crud.put("columns", columns);
        crud.put("search", search);
        crud.put("form", form);
        crud.put("workflow", workflowMetadata(schema));
        Map<String, Object> workflowDefinition = workflowDefinitionMetadata(schema);
        if (!workflowDefinition.isEmpty()) {
            crud.put("workflowDefinition", workflowDefinition);
        }
        crud.put("showAdd", true);
        crud.put("showExport", true);
        crud.put("showSelection", true);
        crud.put("actionPermissions", Map.of(
                "add", moduleKey + ":add",
                "edit", moduleKey + ":edit",
                "delete", moduleKey + ":delete",
                "export", moduleKey + ":export",
                "view", moduleKey + ":view"
        ));

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", moduleKey);
        metadata.put("label", moduleLabel);
        metadata.put("labelI18n", labelI18n(moduleLabel, moduleEnglishLabel));
        metadata.put("icon", "sparkles");
        metadata.put("path", "/ai/" + moduleKey);
        metadata.put("permissions", permissions);
        metadata.put("source", "ai-applied");
        metadata.put("taskNo", entity.getTaskNo());
        metadata.put("crud", crud);
        return metadata;
    }

    private List<Map<String, Object>> workflowMetadata(Map<String, Object> schema) {
        List<Map<String, Object>> transitions = new ArrayList<>();
        Object workflowObject = schema.get("workflow");
        if (workflowObject instanceof List<?> list) {
            transitions.addAll(workflowTransitions(schema, list));
        }
        Object workflowDefinitionObject = schema.get("workflowDefinition");
        if (workflowDefinitionObject instanceof Map<?, ?> workflowDefinition) {
            Object transitionsObject = workflowDefinition.get("transitions");
            if (transitionsObject instanceof List<?> list) {
                transitions.addAll(workflowTransitions(schema, list));
            }
        }
        return transitions;
    }

    private Map<String, Object> workflowDefinitionMetadata(Map<String, Object> schema) {
        Object workflowDefinitionObject = schema.get("workflowDefinition");
        if (!(workflowDefinitionObject instanceof Map<?, ?> workflowDefinition)) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("mode", firstText(text(workflowDefinition.get("mode")), "advanced"));
        copyIfPresent(workflowDefinition, normalized, "slaHours");
        copyIfPresent(workflowDefinition, normalized, "parallelStrategy");
        normalized.put("nodes", workflowNodes(workflowDefinition.get("nodes")));
        Object transitionsObject = workflowDefinition.get("transitions");
        if (transitionsObject instanceof List<?> list) {
            normalized.put("transitions", workflowTransitions(schema, list));
        } else {
            normalized.put("transitions", List.of());
        }
        return normalized;
    }

    private List<Map<String, Object>> workflowNodes(Object nodesObject) {
        if (!(nodesObject instanceof List<?> nodes)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : nodes) {
            if (!(item instanceof Map<?, ?> node)) {
                continue;
            }
            String id = text(node.get("id"));
            if (id.isBlank()) {
                continue;
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("id", id);
            normalized.put("type", firstText(text(node.get("type")), text(node.get("nodeType")), "task"));
            String label = firstText(text(node.get("label")), id);
            normalized.put("label", label);
            normalized.put("labelI18n", labelI18n(label, firstText(
                    text(node.get("labelEn")),
                    text(node.get("nameEn")),
                    humanizeIdentifier(id)
            )));
            copyIfPresent(node, normalized, "approverRole");
            copyIfPresent(node, normalized, "approverField");
            copyIfPresent(node, normalized, "timeoutHours");
            copyIfPresent(node, normalized, "timeoutAction");
            copyIfPresent(node, normalized, "parallelGroup");
            result.add(normalized);
        }
        return result;
    }

    private List<Map<String, Object>> workflowTransitions(Map<String, Object> schema, List<?> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> transition)) {
                continue;
            }
            String action = text(transition.get("action"));
            String from = text(transition.get("from"));
            String to = text(transition.get("to"));
            if (action.isBlank() || from.isBlank() || to.isBlank()) {
                continue;
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            String label = workflowLabel(action);
            String englishLabel = workflowEnglishLabel(action);
            normalized.put("action", action);
            normalized.put("from", from);
            normalized.put("to", to);
            normalized.put("label", label);
            normalized.put("labelI18n", labelI18n(label, englishLabel));
            normalized.put("permission", firstText(text(transition.get("permission")), entityPermission(schema, "edit")));
            normalized.put("statusField", firstText(text(transition.get("statusField")), "status"));
            normalized.put("confirm", "确认执行“" + label + "”操作？");
            normalized.put("confirmI18n", labelI18n(
                    "确认执行“" + label + "”操作？",
                    "Confirm " + englishLabel + "?"
            ));
            copyIfPresent(transition, normalized, "condition");
            copyIfPresent(transition, normalized, "approverRole");
            copyIfPresent(transition, normalized, "approverField");
            copyIfPresent(transition, normalized, "timeoutHours");
            copyIfPresent(transition, normalized, "timeoutAction");
            copyIfPresent(transition, normalized, "parallelGroup");
            copyIfPresent(transition, normalized, "nodeType");
            result.add(normalized);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private String entityPermission(Map<String, Object> schema, String action) {
        Map<String, Object> module = (Map<String, Object>) schema.get("module");
        String moduleKey = text(module.get("key"));
        return moduleKey + ":" + action;
    }

    private String workflowLabel(String action) {
        return switch (action) {
            case "assign" -> "派单";
            case "start" -> "开始处理";
            case "complete" -> "完成";
            case "close" -> "关闭";
            case "activate" -> "生效";
            case "expire" -> "到期";
            case "terminate" -> "终止";
            case "followUp" -> "跟进";
            case "enable" -> "启用";
            case "disable" -> "停用";
            default -> action;
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> frontendColumn(Map<String, Object> field) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("field", field.get("name"));
        column.put("label", field.get("label"));
        column.put("labelI18n", fieldLabelI18n(field));
        column.put("minWidth", 140);
        String formType = String.valueOf(field.get("formType"));
        List<Map<String, Object>> options = (List<Map<String, Object>>) field.get("options");
        if ("select".equals(formType) && options != null && !options.isEmpty()) {
            column.put("format", "status");
            column.put("statusMap", statusMap(options));
        } else {
            String format = frontendFormat(formType);
            if (!format.isBlank()) {
                column.put("format", format);
            }
        }
        return column;
    }

    private Map<String, Object> frontendSearch(Map<String, Object> field) {
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("field", field.get("name"));
        search.put("label", field.get("label"));
        search.put("labelI18n", fieldLabelI18n(field));
        search.put("type", Boolean.TRUE.equals(field.get("filterable")) ? frontendFormType(String.valueOf(field.get("formType"))) : "input");
        putPlaceholder(search, field);
        putOptions(search, field);
        return search;
    }

    private Map<String, Object> frontendForm(Map<String, Object> field) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("field", field.get("name"));
        form.put("label", field.get("label"));
        form.put("labelI18n", fieldLabelI18n(field));
        form.put("type", frontendFormType(String.valueOf(field.get("formType"))));
        form.put("required", Boolean.TRUE.equals(field.get("required")));
        putPlaceholder(form, field);
        putOptions(form, field);
        copyIfPresent(field, form, "defaultValue");
        return form;
    }

    private void copyIfPresent(Map<?, ?> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    private Map<Object, Object> statusMap(List<Map<String, Object>> options) {
        Map<Object, Object> map = new LinkedHashMap<>();
        List<String> colors = List.of("info", "warning", "success", "danger", "info");
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = options.get(i);
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("label", option.get("label"));
            status.put("labelI18n", optionLabelI18n(option));
            status.put("color", option.getOrDefault("color", colors.get(Math.min(i, colors.size() - 1))));
            map.put(option.get("value"), status);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private void putOptions(Map<String, Object> target, Map<String, Object> field) {
        Object optionsObject = field.get("options");
        if (!(optionsObject instanceof List<?> options) || options.isEmpty()) {
            return;
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Object item : options) {
            if (!(item instanceof Map<?, ?> option)) {
                continue;
            }
            Map<String, Object> normalizedOption = new LinkedHashMap<>();
            normalizedOption.put("label", option.get("label"));
            normalizedOption.put("labelI18n", optionLabelI18n((Map<String, Object>) option));
            normalizedOption.put("value", option.get("value"));
            copyIfPresent(option, normalizedOption, "color");
            normalized.add(normalizedOption);
        }
        if (!normalized.isEmpty()) {
            target.put("options", normalized);
        }
    }

    private void putPlaceholder(Map<String, Object> target, Map<String, Object> field) {
        String label = text(field.get("label"));
        if (label.isBlank()) {
            return;
        }
        String englishLabel = fieldEnglishLabel(field);
        target.put("placeholder", "请输入" + label);
        target.put("placeholderI18n", labelI18n("请输入" + label, "Enter " + englishLabel));
    }

    private Map<String, String> fieldLabelI18n(Map<String, Object> field) {
        String label = firstText(text(field.get("label")), text(field.get("name")));
        return labelI18n(label, fieldEnglishLabel(field));
    }

    private Map<String, String> optionLabelI18n(Map<String, Object> option) {
        String label = firstText(text(option.get("label")), text(option.get("value")));
        return labelI18n(label, firstText(
                text(option.get("labelEn")),
                text(option.get("nameEn")),
                humanizeIdentifier(text(option.get("value"))),
                label
        ));
    }

    private String fieldEnglishLabel(Map<String, Object> field) {
        return firstText(
                text(field.get("labelEn")),
                text(field.get("nameEn")),
                text(field.get("englishLabel")),
                text(field.get("englishName")),
                humanizeIdentifier(text(field.get("name"))),
                text(field.get("label"))
        );
    }

    private Map<String, String> labelI18n(String zhText, String enText) {
        Map<String, String> i18n = new LinkedHashMap<>();
        i18n.put("zh-CN", firstText(zhText, enText));
        i18n.put("en-US", firstText(enText, zhText));
        return i18n;
    }

    private String workflowEnglishLabel(String action) {
        return switch (action) {
            case "assign" -> "Assign";
            case "start" -> "Start Processing";
            case "complete" -> "Complete";
            case "close" -> "Close";
            case "activate" -> "Activate";
            case "expire" -> "Expire";
            case "terminate" -> "Terminate";
            case "followUp" -> "Follow Up";
            case "enable" -> "Enable";
            case "disable" -> "Disable";
            case "approve" -> "Approve";
            case "reject" -> "Reject";
            default -> humanizeIdentifier(action);
        };
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
        String[] words = normalized.split("\\s+");
        List<String> result = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            result.add(word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase());
        }
        return String.join(" ", result);
    }

    private String frontendFormat(String formType) {
        return switch (formType) {
            case "datetime" -> "datetime";
            case "date" -> "date";
            default -> "";
        };
    }

    private String frontendFormType(String formType) {
        return switch (formType) {
            case "textarea" -> "textarea";
            case "datetime" -> "datetime";
            case "select" -> "select";
            default -> "input";
        };
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
