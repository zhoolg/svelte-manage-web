package com.zhoolg.manage.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiModuleTemplateService {
    private static final String VERSION = "2026.06.01";

    public TemplateDefinition selectTemplate(String moduleKey, String description) {
        if (description.contains("报修") || moduleKey.contains("repair")) {
            return new TemplateDefinition("work-order.repair", "报修工单模板", VERSION, repairFields(), repairWorkflow(moduleKey));
        }
        if (description.contains("合同") || moduleKey.contains("contract")) {
            return new TemplateDefinition("rental.contract", "租赁合同模板", VERSION, contractFields(), contractWorkflow(moduleKey));
        }
        if (description.contains("回访") || moduleKey.contains("revisit")) {
            return new TemplateDefinition("crm.revisit", "客户回访模板", VERSION, revisitFields(), revisitWorkflow(moduleKey));
        }
        if (description.contains("库存") || moduleKey.contains("inventory")) {
            return new TemplateDefinition("inventory.stock", "库存管理模板", VERSION, inventoryFields(), defaultWorkflow(moduleKey));
        }
        return new TemplateDefinition("generic.crud", "通用 CRUD 模板", VERSION, genericFields(), defaultWorkflow(moduleKey));
    }

    private List<Map<String, Object>> repairFields() {
        return List.of(
                field("reporter", "报修人", "String", "input", true, true, true, true),
                field("phone", "联系电话", "String", "input", true, true, true, true),
                field("property", "房源", "String", "input", true, true, true, true),
                field("issueType", "问题类型", "String", "select", true, true, true, true, options("水电", "utility", "门窗", "door_window", "家电", "appliance", "其他", "other")),
                field("description", "问题描述", "String", "textarea", true, true, false, false),
                field("assignee", "维修人员", "String", "input", false, true, true, true),
                field("status", "状态", "String", "select", true, true, false, true, options("待派单", "submitted", "已派单", "assigned", "处理中", "processing", "已完成", "completed", "已关闭", "closed")),
                field("finishedTime", "完工时间", "LocalDateTime", "datetime", false, true, false, false),
                readonlyField("createTime", "创建时间", "LocalDateTime", "datetime")
        );
    }

    private List<Map<String, Object>> contractFields() {
        return List.of(
                field("contractNo", "合同编号", "String", "input", true, true, true, true),
                field("tenantName", "租客姓名", "String", "input", true, true, true, true),
                field("property", "房源", "String", "input", true, true, true, true),
                field("startDate", "开始日期", "LocalDate", "date", true, true, false, false),
                field("endDate", "结束日期", "LocalDate", "date", true, true, false, false),
                field("rentAmount", "租金", "BigDecimal", "number", true, true, false, false),
                field("status", "状态", "String", "select", true, true, false, true, options("草稿", "draft", "生效中", "active", "已到期", "expired", "已终止", "terminated")),
                readonlyField("createTime", "创建时间", "LocalDateTime", "datetime")
        );
    }

    private List<Map<String, Object>> revisitFields() {
        return List.of(
                field("customerName", "客户姓名", "String", "input", true, true, true, true),
                field("phone", "联系电话", "String", "input", false, true, true, true),
                field("revisitTime", "回访时间", "LocalDateTime", "datetime", true, true, false, false),
                field("result", "回访结果", "String", "textarea", true, true, false, false),
                field("nextAction", "后续动作", "String", "input", false, true, true, false),
                field("status", "状态", "String", "select", true, true, false, true, options("待回访", "pending", "已回访", "completed", "需跟进", "follow_up", "已关闭", "closed")),
                readonlyField("createTime", "创建时间", "LocalDateTime", "datetime")
        );
    }

    private List<Map<String, Object>> inventoryFields() {
        return List.of(
                field("sku", "库存编码", "String", "input", true, true, true, true),
                field("itemName", "物品名称", "String", "input", true, true, true, true),
                field("category", "分类", "String", "input", false, true, true, true),
                field("quantity", "库存数量", "Integer", "number", true, true, false, false),
                field("location", "存放位置", "String", "input", false, true, true, true),
                field("status", "状态", "String", "select", true, true, false, true, options("正常", "normal", "低库存", "low", "停用", "disabled")),
                readonlyField("createTime", "创建时间", "LocalDateTime", "datetime")
        );
    }

    private List<Map<String, Object>> genericFields() {
        return List.of(
                field("title", "标题", "String", "input", true, true, true, true),
                field("description", "描述", "String", "textarea", false, true, true, false),
                field("status", "状态", "String", "select", true, true, false, true, options("草稿", "draft", "启用", "enabled", "停用", "disabled")),
                readonlyField("createTime", "创建时间", "LocalDateTime", "datetime")
        );
    }

    private List<Map<String, Object>> repairWorkflow(String moduleKey) {
        return List.of(
                transition("submitted", "assigned", "assign", moduleKey + ":edit"),
                transition("assigned", "processing", "start", moduleKey + ":edit"),
                transition("processing", "completed", "complete", moduleKey + ":edit"),
                transition("completed", "closed", "close", moduleKey + ":edit")
        );
    }

    private List<Map<String, Object>> contractWorkflow(String moduleKey) {
        return List.of(
                transition("draft", "active", "activate", moduleKey + ":edit"),
                transition("active", "expired", "expire", moduleKey + ":edit"),
                transition("active", "terminated", "terminate", moduleKey + ":edit")
        );
    }

    private List<Map<String, Object>> revisitWorkflow(String moduleKey) {
        return List.of(
                transition("pending", "completed", "complete", moduleKey + ":edit"),
                transition("completed", "follow_up", "followUp", moduleKey + ":edit"),
                transition("follow_up", "closed", "close", moduleKey + ":edit")
        );
    }

    private List<Map<String, Object>> defaultWorkflow(String moduleKey) {
        return List.of(
                transition("draft", "enabled", "enable", moduleKey + ":edit"),
                transition("enabled", "disabled", "disable", moduleKey + ":edit")
        );
    }

    private Map<String, Object> readonlyField(String name, String label, String type, String formType) {
        Map<String, Object> field = field(name, label, type, formType, false, true, false, false);
        field.put("readonly", true);
        return field;
    }

    private Map<String, Object> field(
            String name,
            String label,
            String type,
            String formType,
            boolean required,
            boolean table,
            boolean searchable,
            boolean filterable
    ) {
        return field(name, label, type, formType, required, table, searchable, filterable, List.of());
    }

    private Map<String, Object> field(
            String name,
            String label,
            String type,
            String formType,
            boolean required,
            boolean table,
            boolean searchable,
            boolean filterable,
            List<Map<String, Object>> options
    ) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("name", name);
        field.put("label", label);
        field.put("labelEn", humanizeIdentifier(name));
        field.put("type", type);
        field.put("formType", formType);
        field.put("required", required);
        field.put("table", table);
        field.put("searchable", searchable);
        field.put("filterable", filterable);
        if (!options.isEmpty()) {
            field.put("options", options);
            field.put("defaultValue", options.get(0).get("value"));
        }
        return field;
    }

    private List<Map<String, Object>> options(Object... pairs) {
        java.util.ArrayList<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("label", pairs[i]);
            option.put("value", pairs[i + 1]);
            option.put("labelEn", humanizeIdentifier(String.valueOf(pairs[i + 1])));
            result.add(option);
        }
        return result;
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
        StringBuilder result = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
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

    private Map<String, Object> transition(String from, String to, String action, String permission) {
        return Map.of("from", from, "to", to, "action", action, "permission", permission);
    }

    public record TemplateDefinition(
            String key,
            String name,
            String version,
            List<Map<String, Object>> fields,
            List<Map<String, Object>> workflow
    ) {
        public Map<String, Object> metadata() {
            return Map.of(
                    "key", key,
                    "name", name,
                    "version", version
            );
        }
    }
}
