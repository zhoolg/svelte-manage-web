package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AiRuntimeCrudSmokeTestService {
    private final ICrudService crudService;

    public AiRuntimeCrudSmokeTestService(ICrudService crudService) {
        this.crudService = crudService;
    }

    public List<AiSmokeTestResult.Check> run(ResourceDefinition resource) {
        List<AiSmokeTestResult.Check> checks = new ArrayList<>();
        Object createdId = null;
        try {
            Map<String, Object> payload = createPayload(resource);
            Map<String, Object> created = crudService.create(resource, payload);
            createdId = created.getOrDefault(resource.primaryKey(), created.get("id"));
            if (createdId == null) {
                checks.add(failed("runtime_create", resource.key(), "运行态新增未返回主键"));
                return checks;
            }
            checks.add(passed("runtime_create", resource.key(), "运行态新增成功"));

            PageResult page = crudService.page(resource, Map.of("pageNum", "1", "pageSize", "5"));
            if (page == null || page.list() == null) {
                checks.add(failed("runtime_page", resource.key(), "运行态分页未返回列表"));
            } else {
                checks.add(passed("runtime_page", resource.key(), "运行态分页成功"));
            }

            Map<String, Object> updatePayload = updatePayload(resource);
            if (updatePayload.isEmpty()) {
                checks.add(passed("runtime_update", resource.key(), "运行态更新跳过：无可更新业务字段"));
            } else {
                crudService.update(resource, createdId, updatePayload);
                checks.add(passed("runtime_update", resource.key(), "运行态更新成功"));
            }

            if (resource.workflow().isEmpty()) {
                checks.add(passed("runtime_workflow", resource.key(), "运行态工作流跳过：未定义流转"));
            } else {
                String action = text(resource.workflow().get(0).get("action"));
                crudService.transitionWorkflow(resource, createdId, action);
                checks.add(passed("runtime_workflow", resource.key(), "运行态工作流流转成功"));
            }
        } catch (RuntimeException ex) {
            checks.add(failed("runtime_crud", resource.key(), ex.getMessage()));
        } finally {
            if (createdId != null) {
                try {
                    crudService.delete(resource, createdId);
                    checks.add(passed("runtime_delete", resource.key(), "运行态测试数据清理成功"));
                } catch (RuntimeException ex) {
                    checks.add(failed("runtime_delete", resource.key(), "运行态测试数据清理失败：" + ex.getMessage()));
                }
            }
        }
        return checks;
    }

    private Map<String, Object> createPayload(ResourceDefinition resource) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> workflowDefaults = workflowDefaults(resource);
        for (Map<String, Object> field : resource.form()) {
            String name = text(field.get("field"));
            if (name.isBlank() || !resource.allowedCreateFields().contains(name)) {
                continue;
            }
            payload.put(name, workflowDefaults.getOrDefault(name, sampleValue(field)));
        }
        workflowDefaults.forEach((field, value) -> {
            if (resource.allowedCreateFields().contains(field)) {
                payload.put(field, value);
            }
        });
        return payload;
    }

    private Map<String, Object> updatePayload(ResourceDefinition resource) {
        Map<String, Object> workflowDefaults = workflowDefaults(resource);
        for (Map<String, Object> field : resource.form()) {
            String name = text(field.get("field"));
            if (name.isBlank()
                    || !resource.allowedUpdateFields().contains(name)
                    || workflowDefaults.containsKey(name)
                    || List.of("id", "createTime", "updateTime").contains(name)) {
                continue;
            }
            return Map.of(name, updatedSampleValue(field));
        }
        return Map.of();
    }

    private Map<String, Object> workflowDefaults(ResourceDefinition resource) {
        if (resource.workflow().isEmpty()) {
            return Map.of();
        }
        Map<String, Object> transition = resource.workflow().get(0);
        String statusField = text(transition.getOrDefault("statusField", "status"));
        Object from = transition.get("from");
        if (statusField.isBlank() || from == null) {
            return Map.of();
        }
        return Map.of(statusField, from);
    }

    @SuppressWarnings("unchecked")
    private Object sampleValue(Map<String, Object> field) {
        if (field.containsKey("defaultValue")) {
            return field.get("defaultValue");
        }
        String type = text(field.get("type"));
        if (List.of("select", "radio", "checkbox").contains(type)) {
            Object options = field.get("options");
            if (options instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> option) {
                return Objects.toString(((Map<String, Object>) option).get("value"), "");
            }
        }
        return switch (type) {
            case "number" -> 1;
            case "switch" -> true;
            case "date" -> "2026-06-02";
            case "datetime" -> "2026-06-02 09:30:00";
            default -> "AI Smoke Test";
        };
    }

    private Object updatedSampleValue(Map<String, Object> field) {
        String type = text(field.get("type"));
        return switch (type) {
            case "number" -> 2;
            case "switch" -> false;
            case "date" -> "2026-06-03";
            case "datetime" -> "2026-06-03 10:30:00";
            default -> "AI Smoke Test Updated";
        };
    }

    private AiSmokeTestResult.Check passed(String code, String target, String message) {
        return new AiSmokeTestResult.Check(code, target, "passed", message);
    }

    private AiSmokeTestResult.Check failed(String code, String target, String message) {
        return new AiSmokeTestResult.Check(code, target, "failed", message);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
