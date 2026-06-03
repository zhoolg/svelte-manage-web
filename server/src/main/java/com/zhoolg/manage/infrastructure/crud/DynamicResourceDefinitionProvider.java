package com.zhoolg.manage.infrastructure.crud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.pojo.DynamicModuleDO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.DynamicModuleMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DynamicResourceDefinitionProvider {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> MAP_LIST_TYPE = new TypeReference<>() {
    };

    private final DynamicModuleMapper mapper;
    private final ObjectMapper objectMapper;

    public DynamicResourceDefinitionProvider(DynamicModuleMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public Optional<ResourceDefinition> find(String key) {
        DynamicModuleDO entity = mapper.selectByModuleKey(key);
        if (entity == null || !Boolean.TRUE.equals(entity.getEnabled())) {
            return Optional.empty();
        }

        Map<String, Object> metadata = readJson(entity.getMetadataJson());
        Map<String, Object> crud = objectMap(metadata.get("crud"));
        if (crud.isEmpty()) {
            throw new ApiException(500, "动态模块缺少 CRUD 元数据");
        }

        List<Map<String, Object>> columns = objectList(crud.get("columns"));
        List<Map<String, Object>> search = objectList(crud.get("search"));
        List<Map<String, Object>> form = objectList(crud.get("form"));
        List<Map<String, Object>> workflow = objectList(crud.get("workflow"));
        if (columns.isEmpty() || form.isEmpty()) {
            throw new ApiException(500, "动态模块字段元数据不完整");
        }

        List<String> allowedFields = form.stream()
                .filter(field -> !Boolean.TRUE.equals(field.get("disabled")))
                .map(field -> text(field.get("field")))
                .filter(field -> !field.isBlank())
                .filter(field -> !List.of("id", "createTime", "updateTime").contains(field))
                .toList();
        List<String> searchableFields = search.stream()
                .filter(field -> List.of("input", "textarea", "tags").contains(text(field.get("type"))))
                .map(field -> text(field.get("field")))
                .filter(field -> !field.isBlank())
                .toList();
        List<String> filterFields = search.stream()
                .filter(field -> !List.of("input", "textarea", "tags").contains(text(field.get("type"))))
                .map(field -> text(field.get("field")))
                .filter(field -> !field.isBlank())
                .toList();

        String moduleId = text(metadata.getOrDefault("id", key));
        String title = firstText(text(crud.get("title")), text(metadata.get("label")), entity.getModuleName(), key);
        String path = firstText(text(crud.get("restBase")), "/" + moduleId);

        return Optional.of(new ResourceDefinition(
                moduleId,
                path,
                title,
                permissionPrefix(metadata, moduleId),
                "id",
                searchableFields,
                filterFields,
                allowedFields,
                allowedFields,
                columns,
                search,
                form,
                workflow
        ));
    }

    public boolean exists(String key) {
        DynamicModuleDO entity = mapper.selectByModuleKey(key);
        return entity != null && Boolean.TRUE.equals(entity.getEnabled());
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            throw new ApiException(500, "动态模块元数据解析失败");
        }
    }

    private Map<String, Object> objectMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        return objectMapper.convertValue(value, MAP_TYPE);
    }

    private List<Map<String, Object>> objectList(Object value) {
        if (value == null) {
            return List.of();
        }
        return objectMapper.convertValue(value, MAP_LIST_TYPE);
    }

    @SuppressWarnings("unchecked")
    private String permissionPrefix(Map<String, Object> metadata, String fallback) {
        Object permissions = metadata.get("permissions");
        if (permissions instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(permission -> permission.endsWith(":view"))
                    .map(permission -> permission.substring(0, permission.indexOf(':')))
                    .findFirst()
                    .orElse(fallback);
        }
        return fallback;
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
