package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiGenerateResponse;
import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiValidationService {
    @SuppressWarnings("unchecked")
    public AiValidationReport validate(
            AiGenerationTaskDO entity,
            Map<String, Object> schema,
            List<AiGenerateResponse.GeneratedFile> files
    ) {
        List<AiValidationReport.Issue> issues = new ArrayList<>();
        Object moduleObject = schema.get("module");
        Object entityObject = schema.get("entity");
        if (!(moduleObject instanceof Map<?, ?> module)) {
            issues.add(error("schema.module.missing", "缺少模块定义"));
        } else {
            Object key = module.get("key");
            if (!entity.getModuleKey().equals(key)) {
                issues.add(error("module.key.mismatch", "模块标识与任务记录不一致"));
            }
            Object permissions = module.get("permissions");
            if (!(permissions instanceof List<?> list) || list.isEmpty()) {
                issues.add(error("module.permissions.missing", "缺少权限定义"));
            }
        }
        if (!(entityObject instanceof Map<?, ?> model)) {
            issues.add(error("schema.entity.missing", "缺少实体定义"));
        } else {
            Object tableName = model.get("tableName");
            if (!("biz_" + entity.getModuleKey()).equals(tableName)) {
                issues.add(error("entity.table.invalid", "表名必须使用 biz_ + 模块标识"));
            }
            Object fields = model.get("fields");
            if (!(fields instanceof List<?> fieldList) || fieldList.isEmpty()) {
                issues.add(error("entity.fields.missing", "至少需要一个业务字段"));
            } else {
                List<Map<String, Object>> normalizedFields = fieldList.stream()
                        .filter(Map.class::isInstance)
                        .map(this::objectMap)
                        .toList();
                if (normalizedFields.stream().noneMatch(field -> "status".equals(field.get("name")))) {
                    issues.add(warn("entity.status.missing", "建议为后台业务模块保留状态字段"));
                }
                validateFieldQuality(normalizedFields, issues);
                validateWorkflowConsistency(schema, normalizedFields, entity.getModuleKey(), issues);
                validateIndexSuggestion(normalizedFields, files, issues);
            }
        }
        if (files == null || files.isEmpty()) {
            issues.add(error("files.missing", "缺少生成文件预览"));
        } else {
            boolean hasLegacyModulePath = files.stream().anyMatch(file -> file.path().contains("/modules/"));
            if (hasLegacyModulePath) {
                issues.add(error("files.legacy.path", "生成文件不能回到旧 modules 包结构"));
            }
            boolean hasSql = files.stream().anyMatch(file -> "sql".equals(file.type()));
            if (!hasSql) {
                issues.add(warn("files.sql.missing", "建议提供 MySQL 建表草稿"));
            }
        }

        long errorCount = issues.stream().filter(issue -> "error".equals(issue.level())).count();
        long warningCount = issues.stream().filter(issue -> "warning".equals(issue.level())).count();
        int score = (int) Math.max(0, 100 - errorCount * 40 - warningCount * 10);
        return new AiValidationReport(errorCount == 0, score, issues);
    }

    private void validateFieldQuality(
            List<Map<String, Object>> fields,
            List<AiValidationReport.Issue> issues
    ) {
        if (fields.size() > 30) {
            issues.add(warn("entity.fields.too_many", "字段数量超过 30 个，建议拆分为多个业务模块或分组表单"));
        }

        Set<String> fieldNames = new HashSet<>();
        int searchableCount = 0;
        for (Map<String, Object> field : fields) {
            String name = text(field.get("name"));
            String formType = text(field.get("formType"));
            if (name.isBlank()) {
                issues.add(error("entity.field.name.blank", "字段名不能为空"));
                continue;
            }
            if (!fieldNames.add(name)) {
                issues.add(error("entity.field.name.duplicate", "字段名重复：" + name));
            }
            if (!name.matches("[a-z][A-Za-z0-9]*")) {
                issues.add(warn("entity.field.name.style", "字段名建议使用 lowerCamelCase：" + name));
            }
            if (Boolean.TRUE.equals(field.get("searchable")) || Boolean.TRUE.equals(field.get("filterable"))) {
                searchableCount++;
            }
            if (Boolean.TRUE.equals(field.get("searchable")) && "textarea".equals(formType)) {
                issues.add(warn("entity.search.long_text", "长文本字段不建议直接参与模糊搜索：" + name));
            }
            if ("select".equals(formType) && objectList(field.get("options")).isEmpty()) {
                issues.add(warn("entity.enum.options.missing", "枚举字段缺少选项：" + name));
            }
            if (isSensitiveField(name)) {
                issues.add(warn("entity.field.sensitive", "字段可能包含敏感信息，需要确认脱敏、导出和权限策略：" + name));
            }
        }

        if (searchableCount > 6) {
            issues.add(warn("entity.search.too_many", "搜索/筛选字段超过 6 个，建议收敛高频查询并补充索引"));
        }
    }

    private void validateWorkflowConsistency(
            Map<String, Object> schema,
            List<Map<String, Object>> fields,
            String moduleKey,
            List<AiValidationReport.Issue> issues
    ) {
        List<Map<String, Object>> workflow = objectList(schema.get("workflow"));
        Object workflowDefinitionObject = schema.get("workflowDefinition");
        if (workflowDefinitionObject instanceof Map<?, ?> workflowDefinition) {
            workflow = new ArrayList<>(workflow);
            workflow.addAll(objectList(workflowDefinition.get("transitions")));
            validateAdvancedWorkflowDefinition(workflowDefinition, fields, issues);
        }
        if (workflow.isEmpty()) {
            return;
        }

        Set<String> fieldNames = new HashSet<>();
        fields.forEach(field -> fieldNames.add(text(field.get("name"))));
        Map<String, Object> statusField = fields.stream()
                .filter(field -> "status".equals(field.get("name")))
                .findFirst()
                .orElse(Map.of());
        List<Map<String, Object>> statusOptions = objectList(statusField.get("options"));
        if (statusOptions.isEmpty()) {
            issues.add(error("workflow.status.options.missing", "workflow 已定义，但 status 字段缺少枚举选项"));
            return;
        }

        Set<String> allowedStatus = new HashSet<>();
        statusOptions.forEach(option -> allowedStatus.add(text(option.get("value"))));
        for (Map<String, Object> transition : workflow) {
            String from = text(transition.get("from"));
            String to = text(transition.get("to"));
            String action = text(transition.get("action"));
            String permission = text(transition.get("permission"));
            if (from.isBlank() || to.isBlank() || action.isBlank()) {
                issues.add(error("workflow.transition.incomplete", "workflow 流转必须包含 from/to/action"));
                continue;
            }
            if (!allowedStatus.contains(from) || !allowedStatus.contains(to)) {
                issues.add(error("workflow.status.mismatch", "workflow 状态必须存在于 status 枚举：" + from + " -> " + to));
            }
            if (permission.isBlank() || !permission.startsWith(moduleKey + ":")) {
                issues.add(warn("workflow.permission.invalid", "workflow 权限建议使用当前模块权限前缀：" + action));
            }
            validateWorkflowCondition(transition, fieldNames, issues);
        }
    }

    private void validateAdvancedWorkflowDefinition(
            Map<?, ?> workflowDefinition,
            List<Map<String, Object>> fields,
            List<AiValidationReport.Issue> issues
    ) {
        String mode = text(workflowDefinition.get("mode"));
        if (!mode.isBlank() && !"advanced".equals(mode)) {
            issues.add(warn("workflow.mode.unknown", "workflowDefinition.mode 建议使用 advanced"));
        }
        for (Map<String, Object> node : objectList(workflowDefinition.get("nodes"))) {
            String type = text(node.get("type"));
            if ("approval".equals(type) && text(node.get("approverRole")).isBlank()) {
                issues.add(warn("workflow.approver.missing", "审批节点建议声明 approverRole：" + text(node.get("id"))));
            }
            Object timeoutHours = node.get("timeoutHours");
            if (timeoutHours != null && !isPositiveNumber(timeoutHours)) {
                issues.add(warn("workflow.timeout.invalid", "超时配置必须为正数：" + text(node.get("id"))));
            }
        }
        for (Map<String, Object> transition : objectList(workflowDefinition.get("transitions"))) {
            Object timeoutHours = transition.get("timeoutHours");
            if (timeoutHours != null && !isPositiveNumber(timeoutHours)) {
                issues.add(warn("workflow.timeout.invalid", "流转超时配置必须为正数：" + text(transition.get("action"))));
            }
        }
    }

    private void validateWorkflowCondition(
            Map<String, Object> transition,
            Set<String> fieldNames,
            List<AiValidationReport.Issue> issues
    ) {
        Object conditionObject = transition.get("condition");
        if (!(conditionObject instanceof Map<?, ?> condition) || condition.isEmpty()) {
            return;
        }
        String field = text(condition.get("field"));
        String operator = text(condition.get("operator"));
        if (field.isBlank() || operator.isBlank()) {
            issues.add(error("workflow.condition.incomplete", "工作流条件必须包含 field/operator"));
            return;
        }
        if (!fieldNames.contains(field)) {
            issues.add(error("workflow.condition.field_missing", "工作流条件字段不存在：" + field));
        }
        Set<String> operators = Set.of("eq", "ne", "gt", "gte", "lt", "lte", "notBlank");
        if (!operators.contains(operator)) {
            issues.add(error("workflow.condition.operator_invalid", "工作流条件操作符不支持：" + operator));
        }
    }

    private boolean isPositiveNumber(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value)) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void validateIndexSuggestion(
            List<Map<String, Object>> fields,
            List<AiGenerateResponse.GeneratedFile> files,
            List<AiValidationReport.Issue> issues
    ) {
        long indexedQueryFields = fields.stream()
                .filter(field -> Boolean.TRUE.equals(field.get("searchable")) || Boolean.TRUE.equals(field.get("filterable")))
                .filter(field -> !"textarea".equals(text(field.get("formType"))))
                .count();
        if (indexedQueryFields < 3) {
            return;
        }

        boolean sqlHasIndex = files != null && files.stream()
                .filter(file -> "sql".equals(file.type()))
                .map(AiGenerateResponse.GeneratedFile::content)
                .anyMatch(content -> content.toLowerCase(Locale.ROOT).contains(" key ")
                        || content.toLowerCase(Locale.ROOT).contains(" index "));
        if (!sqlHasIndex) {
            issues.add(warn("entity.index.suggestion", "搜索/筛选字段较多，建议为高频查询字段补充 MySQL 索引"));
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String lowerName = fieldName.toLowerCase(Locale.ROOT);
        return lowerName.contains("password")
                || lowerName.contains("token")
                || lowerName.contains("secret")
                || lowerName.contains("idcard")
                || lowerName.contains("identity")
                || lowerName.contains("bankcard")
                || lowerName.contains("bankaccount");
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

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private AiValidationReport.Issue error(String code, String message) {
        return new AiValidationReport.Issue("error", code, message);
    }

    private AiValidationReport.Issue warn(String code, String message) {
        return new AiValidationReport.Issue("warning", code, message);
    }
}
