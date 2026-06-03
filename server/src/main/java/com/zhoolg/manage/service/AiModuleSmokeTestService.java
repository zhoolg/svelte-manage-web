package com.zhoolg.manage.service;

import com.zhoolg.manage.entity.dto.AiSmokeTestResult;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.infrastructure.crud.DynamicResourceDefinitionProvider;
import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.mapper.PermissionMapper;
import com.zhoolg.manage.mapper.RolePermissionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AiModuleSmokeTestService {
    private final DynamicResourceDefinitionProvider resourceDefinitionProvider;
    private final MenuMapper menuMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final AiRuntimeCrudSmokeTestService runtimeCrudSmokeTestService;

    public AiModuleSmokeTestService(
            DynamicResourceDefinitionProvider resourceDefinitionProvider,
            MenuMapper menuMapper,
            PermissionMapper permissionMapper,
            RolePermissionMapper rolePermissionMapper,
            AiRuntimeCrudSmokeTestService runtimeCrudSmokeTestService
    ) {
        this.resourceDefinitionProvider = resourceDefinitionProvider;
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.runtimeCrudSmokeTestService = runtimeCrudSmokeTestService;
    }

    public AiSmokeTestResult runAppliedModuleSmokeTest(String moduleKey, Map<String, Object> metadata) {
        List<AiSmokeTestResult.Check> checks = new ArrayList<>();
        List<String> expectedPermissions = expectedPermissions(moduleKey, metadata);
        ResourceDefinition definition = checkResourceDefinition(moduleKey, checks);
        checkMenu(moduleKey, checks);
        checkPermissionCatalog(expectedPermissions, checks);
        checkRoleBinding("super_admin", expectedPermissions, checks);
        checkRoleBinding("admin", expectedPermissions, checks);
        if (definition != null) {
            checks.addAll(runtimeCrudSmokeTestService.run(definition));
        }

        long failed = checks.stream().filter(check -> "failed".equals(check.status())).count();
        int score = (int) Math.max(0, 100 - failed * 20);
        List<AiSmokeTestResult.Diagnostic> diagnostics = diagnostics(checks);
        return new AiSmokeTestResult(
                failed == 0,
                score,
                LocalDateTime.now().toString(),
                checks,
                diagnostics,
                repairSuggestions(diagnostics)
        );
    }

    private ResourceDefinition checkResourceDefinition(String moduleKey, List<AiSmokeTestResult.Check> checks) {
        try {
            ResourceDefinition definition = resourceDefinitionProvider.find(moduleKey).orElse(null);
            if (definition == null) {
                checks.add(failed("resource_definition", moduleKey, "动态资源定义未注册或未启用"));
                return null;
            }
            if (definition.columns().isEmpty() || definition.form().isEmpty()) {
                checks.add(failed("resource_definition", moduleKey, "动态资源缺少表格列或表单字段"));
                return null;
            }
            if (!definition.permission("view").equals(moduleKey + ":view")) {
                checks.add(failed("resource_definition", moduleKey, "动态资源权限前缀与模块标识不一致"));
                return null;
            }
            checks.add(passed("resource_definition", moduleKey, "动态资源定义可加载"));
            return definition;
        } catch (RuntimeException ex) {
            checks.add(failed("resource_definition", moduleKey, ex.getMessage()));
            return null;
        }
    }

    private void checkMenu(String moduleKey, List<AiSmokeTestResult.Check> checks) {
        try {
            MenuDO menu = menuMapper.selectByModuleId(moduleKey);
            if (menu == null || !Boolean.TRUE.equals(menu.getEnabled())) {
                checks.add(failed("menu", moduleKey, "菜单未创建或未启用"));
                return;
            }
            if (!Objects.equals(menu.getPermissionCode(), moduleKey + ":view")) {
                checks.add(failed("menu", moduleKey, "菜单查看权限与模块标识不一致"));
                return;
            }
            checks.add(passed("menu", moduleKey, "菜单入口已启用"));
        } catch (RuntimeException ex) {
            checks.add(failed("menu", moduleKey, ex.getMessage()));
        }
    }

    private void checkPermissionCatalog(
            List<String> expectedPermissions,
            List<AiSmokeTestResult.Check> checks
    ) {
        try {
            Set<String> catalog = new HashSet<>();
            permissionMapper.selectEnabled().forEach(permission -> catalog.add(permission.getPermissionCode()));
            List<String> missing = expectedPermissions.stream()
                    .filter(permission -> !catalog.contains(permission))
                    .toList();
            if (!missing.isEmpty()) {
                checks.add(failed("permission_catalog", String.join(",", missing), "权限目录缺少动态权限"));
                return;
            }
            checks.add(passed("permission_catalog", String.join(",", expectedPermissions), "动态权限目录已注册"));
        } catch (RuntimeException ex) {
            checks.add(failed("permission_catalog", String.join(",", expectedPermissions), ex.getMessage()));
        }
    }

    private void checkRoleBinding(
            String roleCode,
            List<String> expectedPermissions,
            List<AiSmokeTestResult.Check> checks
    ) {
        try {
            Set<String> rolePermissions = new HashSet<>(rolePermissionMapper.selectPermissionsByRole(roleCode));
            List<String> missing = expectedPermissions.stream()
                    .filter(permission -> !rolePermissions.contains(permission))
                    .toList();
            if (!missing.isEmpty()) {
                checks.add(failed("role_binding", roleCode, "角色缺少权限：" + String.join(",", missing)));
                return;
            }
            checks.add(passed("role_binding", roleCode, "角色权限绑定完整"));
        } catch (RuntimeException ex) {
            checks.add(failed("role_binding", roleCode, ex.getMessage()));
        }
    }

    private List<String> expectedPermissions(String moduleKey, Map<String, Object> metadata) {
        Object permissions = metadata.get("permissions");
        if (permissions instanceof List<?> list) {
            List<String> result = list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(permission -> permission.startsWith(moduleKey + ":"))
                    .distinct()
                    .toList();
            if (!result.isEmpty()) {
                return result;
            }
        }
        return List.of(
                moduleKey + ":view",
                moduleKey + ":add",
                moduleKey + ":edit",
                moduleKey + ":delete",
                moduleKey + ":export"
        );
    }

    private AiSmokeTestResult.Check passed(String code, String target, String message) {
        return new AiSmokeTestResult.Check(code, target, "passed", message);
    }

    private AiSmokeTestResult.Check failed(String code, String target, String message) {
        return new AiSmokeTestResult.Check(code, target, "failed", message);
    }

    private List<AiSmokeTestResult.Diagnostic> diagnostics(List<AiSmokeTestResult.Check> checks) {
        return checks.stream()
                .filter(check -> "failed".equals(check.status()))
                .map(check -> new AiSmokeTestResult.Diagnostic(
                        check.code(),
                        check.target(),
                        severity(check.code()),
                        check.message(),
                        suggestion(check.code(), check.target())
                ))
                .toList();
    }

    private String severity(String code) {
        return switch (code) {
            case "resource_definition", "permission_catalog", "runtime_create", "runtime_page", "runtime_update",
                    "runtime_workflow", "runtime_delete", "runtime_crud" -> "high";
            case "menu", "role_binding" -> "medium";
            default -> "low";
        };
    }

    private String suggestion(String code, String target) {
        return switch (code) {
            case "resource_definition" -> "重新生成或恢复动态模块元数据，确认 crud.columns 和 crud.form 均非空。";
            case "menu" -> "重新执行 AI 模块应用或菜单 upsert，确认 sys_menu 中 module_id=" + target + " 已启用。";
            case "permission_catalog" -> "重新注册动态权限目录，确认 sys_permission 包含：" + target + "。";
            case "role_binding" -> "重新绑定管理角色权限，确认角色 " + target + " 具备模块 view/add/edit/delete/export 权限。";
            case "runtime_create", "runtime_page", "runtime_update", "runtime_workflow", "runtime_delete", "runtime_crud" ->
                    "检查动态表结构、表单必填字段、字段类型和工作流初始状态，重新执行应用后运行 CRUD 冒烟测试。";
            default -> "检查失败项并重新执行 AI 模块应用。";
        };
    }

    private List<String> repairSuggestions(List<AiSmokeTestResult.Diagnostic> diagnostics) {
        return diagnostics.stream()
                .map(AiSmokeTestResult.Diagnostic::suggestion)
                .distinct()
                .toList();
    }
}
