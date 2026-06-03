package com.zhoolg.manage.service.impl;

import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.PermissionMapper;
import com.zhoolg.manage.mapper.RolePermissionMapper;
import com.zhoolg.manage.service.IPermissionService;
import com.zhoolg.manage.entity.base.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限服务
 * 职责：权限校验、角色权限映射
 */
@Service
public class PermissionServiceImpl implements IPermissionService {
    private static final Map<String, List<String>> FALLBACK_PERMISSIONS = Map.of(
            "super_admin", List.of("*"),
            "admin", List.of("*"),
            "operator", List.of("dashboard:view", "application:*", "faq:*", "admin:view", "log:view", "profile:*"),
            "viewer", List.of("dashboard:view", "admin:view", "application:view", "faq:view", "log:view", "dict:view", "settings:view")
    );

    private static final List<Map<String, Object>> FALLBACK_ROLES = List.of(
            Map.of("code", "super_admin", "name", "超级管理员", "permissions", FALLBACK_PERMISSIONS.get("super_admin")),
            Map.of("code", "admin", "name", "管理员", "permissions", FALLBACK_PERMISSIONS.get("admin")),
            Map.of("code", "operator", "name", "运营人员", "permissions", FALLBACK_PERMISSIONS.get("operator")),
            Map.of("code", "viewer", "name", "查看者", "permissions", FALLBACK_PERMISSIONS.get("viewer"))
    );

    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(RolePermissionMapper rolePermissionMapper, PermissionMapper permissionMapper) {
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionMapper = permissionMapper;
    }

    /**
     * 校验用户是否拥有指定权限
     * @param user 当前用户
     * @param requiredPermission 所需权限（如 "admin:view"、"*"）
     * @throws ApiException 403 无权限
     */
    public void requirePermission(CurrentUser user, String requiredPermission) {
        if (!hasPermission(user, requiredPermission)) {
            throw new ApiException(403, "无权限访问");
        }
    }

    /**
     * 判断用户是否拥有指定权限
     */
    public boolean hasPermission(CurrentUser user, String requiredPermission) {
        if (user == null || requiredPermission == null || requiredPermission.isBlank()) {
            return false;
        }

        List<String> userPermissions = user.permissions();
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (userPermissions.contains("*")) {
            return true;
        }

        // 精确匹配
        if (userPermissions.contains(requiredPermission)) {
            return true;
        }

        // 通配符匹配（如 "admin:*" 匹配 "admin:view"）
        String prefix = requiredPermission.contains(":")
                ? requiredPermission.substring(0, requiredPermission.indexOf(":")) + ":*"
                : null;
        if (prefix != null && userPermissions.contains(prefix)) {
            return true;
        }

        return false;
    }

    /**
     * 角色权限以数据库为准；数据库尚未初始化或角色无权限时回落到内置基线。
     */
    public List<String> permissionsForRole(String roleCode) {
        String normalizedRole = normalizeRole(roleCode);
        try {
            List<String> permissions = rolePermissionMapper.selectPermissionsByRole(normalizedRole);
            if (permissions != null && !permissions.isEmpty()) {
                return List.copyOf(permissions);
            }
        } catch (RuntimeException ignored) {
            // 初始化阶段或数据库不可用时，使用内置权限保证本地开发可登录。
        }
        return FALLBACK_PERMISSIONS.getOrDefault(normalizedRole, FALLBACK_PERMISSIONS.get("viewer"));
    }

    public List<Map<String, Object>> roleMetadata() {
        try {
            List<Map<String, Object>> roles = rolePermissionMapper.selectEnabledRoles().stream()
                    .map(role -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("code", role.code());
                        row.put("name", role.name());
                        row.put("permissions", permissionsForRole(role.code()));
                        row.put("systemBuiltin", role.systemBuiltin());
                        return row;
                    })
                    .toList();
            if (!roles.isEmpty()) {
                return roles;
            }
        } catch (RuntimeException ignored) {
            // 初始化阶段或数据库不可用时，使用内置角色元数据。
        }
        return FALLBACK_ROLES;
    }

    public List<Map<String, Object>> permissionCatalog() {
        try {
            return permissionMapper.selectEnabled().stream()
                    .map(permission -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("permission", permission.getPermissionCode());
                        row.put("moduleKey", permission.getModuleKey());
                        row.put("moduleName", permission.getModuleName());
                        row.put("action", permission.getActionCode());
                        row.put("actionName", permission.getActionName());
                        row.put("source", permission.getSource());
                        return row;
                    })
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return "viewer";
        }
        String normalized = roleCode.trim().toLowerCase();
        return FALLBACK_PERMISSIONS.containsKey(normalized) ? normalized : normalized;
    }
}
