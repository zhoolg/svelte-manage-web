package com.zhoolg.manage.infrastructure.crud;

import java.util.List;
import java.util.Map;

public record ResourceDefinition(
        String key,
        String path,
        String title,
        String permissionPrefix,
        String primaryKey,
        List<String> searchableFields,
        List<String> filterFields,
        List<String> allowedCreateFields,
        List<String> allowedUpdateFields,
        List<Map<String, Object>> columns,
        List<Map<String, Object>> search,
        List<Map<String, Object>> form,
        List<Map<String, Object>> workflow
) {
    public ResourceDefinition(
            String key,
            String path,
            String title,
            String permissionPrefix,
            String primaryKey,
            List<String> searchableFields,
            List<String> filterFields,
            List<String> allowedCreateFields,
            List<String> allowedUpdateFields,
            List<Map<String, Object>> columns,
            List<Map<String, Object>> search,
            List<Map<String, Object>> form
    ) {
        this(key, path, title, permissionPrefix, primaryKey, searchableFields, filterFields,
                allowedCreateFields, allowedUpdateFields, columns, search, form, List.of());
    }

    public String permission(String action) {
        return permissionPrefix + ":" + action;
    }
}
