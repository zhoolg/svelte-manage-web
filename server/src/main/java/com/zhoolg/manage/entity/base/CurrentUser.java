package com.zhoolg.manage.entity.base;

import java.util.List;

public record CurrentUser(
        long id,
        String username,
        String name,
        String roleCode,
        List<String> permissions,
        boolean admin
) {
    public boolean hasPermission(String permission) {
        if (admin || permissions.contains("*")) {
            return true;
        }
        if (permissions.contains(permission)) {
            return true;
        }
        int colonIndex = permission.indexOf(':');
        if (colonIndex > 0) {
            return permissions.contains(permission.substring(0, colonIndex) + ":*");
        }
        return false;
    }
}
