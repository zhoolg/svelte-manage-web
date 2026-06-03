package com.zhoolg.manage.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhoolg.manage.entity.dto.AiApplyPlan;
import com.zhoolg.manage.entity.dto.AiModuleVersionSummary;
import com.zhoolg.manage.entity.dto.AiValidationReport;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.entity.pojo.DynamicModuleDO;
import com.zhoolg.manage.entity.pojo.DynamicModuleVersionDO;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.entity.pojo.PermissionDO;
import com.zhoolg.manage.infrastructure.crud.DynamicTableService;
import com.zhoolg.manage.mapper.DynamicModuleMapper;
import com.zhoolg.manage.mapper.DynamicModuleVersionMapper;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.mapper.PermissionMapper;
import com.zhoolg.manage.mapper.RolePermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DynamicModuleService {
    private final DynamicModuleMapper mapper;
    private final DynamicModuleVersionMapper versionMapper;
    private final MenuMapper menuMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final ObjectMapper objectMapper;
    private final DynamicTableService dynamicTableService;

    public DynamicModuleService(
            DynamicModuleMapper mapper,
            DynamicModuleVersionMapper versionMapper,
            MenuMapper menuMapper,
            PermissionMapper permissionMapper,
            RolePermissionMapper rolePermissionMapper,
            ObjectMapper objectMapper,
            DynamicTableService dynamicTableService
    ) {
        this.mapper = mapper;
        this.versionMapper = versionMapper;
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.objectMapper = objectMapper;
        this.dynamicTableService = dynamicTableService;
    }

    public List<Map<String, Object>> enabledModules() {
        return mapper.selectEnabled().stream()
                .map(entity -> readJson(entity.getMetadataJson()))
                .toList();
    }

    public AiApplyPlan buildApplyPlan(
            String taskNo,
            String moduleKey,
            String moduleName,
            Map<String, Object> metadata,
            Map<String, Object> schema,
            AiValidationReport validation
    ) {
        List<AiApplyPlan.Operation> operations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!validation.passed()) {
            validation.issues().forEach(issue -> operations.add(operation(
                    "validation",
                    issue.level(),
                    issue.code(),
                    issue.message(),
                    "error".equals(issue.level()) ? "high" : "medium"
            )));
        }

        if (validation.passed()) {
            operations.addAll(dynamicTableService.tablePlan(moduleKey, schema));
            DynamicModuleDO existingModule = mapper.selectByModuleKey(moduleKey);
            int nextVersionNo = versionMapper.nextVersionNo(moduleKey);
            operations.add(operation(
                    "metadata",
                    existingModule == null ? "create_module" : "update_module",
                    "sys_dynamic_module." + moduleKey,
                    existingModule == null
                            ? "注册动态模块元数据 " + moduleName
                            : "更新动态模块元数据并启用 " + moduleName,
                    existingModule == null ? "low" : "medium"
            ));
            operations.add(operation(
                    "version",
                    "create_version",
                    "sys_dynamic_module_version." + moduleKey + ".v" + nextVersionNo,
                    "保存第 " + nextVersionNo + " 个动态模块版本快照，包含 schema、元数据和任务号",
                    "low"
            ));

            MenuDO existingMenu = menuMapper.selectByModuleId(moduleKey);
            operations.add(operation(
                    "menu",
                    existingMenu == null ? "create_menu" : "update_menu",
                    "sys_menu.ai-" + moduleKey,
                    existingMenu == null
                            ? "在系统菜单下创建 AI 模块入口"
                            : "更新并启用 AI 模块菜单入口",
                    "low"
            ));

            operations.add(operation(
                    "api",
                    "expose_crud",
                    "/" + moduleKey,
                    "通过兼容 CRUD 接口开放 list/add/update/delete/status",
                    "low"
            ));
            operations.add(operation(
                    "permission",
                    "register_permission",
                    moduleKey + ":view/add/edit/delete/export",
                    "注册动态权限目录，并默认绑定 super_admin/admin 管理角色",
                    "low"
            ));
            operations.add(operation(
                    "permission",
                    "bind_admin_roles",
                    "super_admin,admin",
                    "把动态模块权限绑定到超级管理员和管理员角色；普通角色需后续在权限管理中授权",
                    "low"
            ));
            warnings.add("动态模块默认只授权 super_admin/admin，operator/viewer 不会自动获得新模块访问权。");
            warnings.add("当前版本只执行建表和追加列，不执行删除列、改类型等破坏性迁移。");
        }

        int riskScore = riskScore(operations, warnings, validation.score());
        return new AiApplyPlan(
                taskNo,
                moduleKey,
                moduleName,
                validation.passed(),
                riskScore,
                riskLevel(operations, riskScore),
                operations,
                warnings
        );
    }

    @Transactional
    public void applyModule(
            String moduleKey,
            String moduleName,
            String taskNo,
            Map<String, Object> metadata,
            Map<String, Object> schema
    ) {
        dynamicTableService.ensureTable(moduleKey, schema);
        String metadataJson = writeJson(metadata);
        String schemaJson = writeJson(schema);
        int versionNo = versionMapper.nextVersionNo(moduleKey);
        String schemaHash = sha256(schemaJson);

        DynamicModuleVersionDO version = new DynamicModuleVersionDO();
        version.setModuleKey(moduleKey);
        version.setModuleName(moduleName);
        version.setVersionNo(versionNo);
        version.setTaskNo(taskNo);
        version.setSchemaHash(schemaHash);
        version.setMetadataJson(metadataJson);
        version.setSchemaJson(schemaJson);
        versionMapper.insert(version);

        DynamicModuleDO entity = new DynamicModuleDO();
        entity.setModuleKey(moduleKey);
        entity.setModuleName(moduleName);
        entity.setTaskNo(taskNo);
        entity.setCurrentVersionNo(versionNo);
        entity.setSchemaHash(schemaHash);
        entity.setMetadataJson(metadataJson);
        mapper.upsert(entity);
        registerPermissions(moduleKey, moduleName, metadata);
        upsertMenu(metadata, moduleKey, moduleName);
    }

    @Transactional
    public void rollbackModule(String moduleKey, String taskNo) {
        int updated = mapper.disableByModuleKeyAndTaskNo(moduleKey, taskNo);
        if (updated == 0) {
            throw new ApiException(400, "动态模块不存在、已回滚或已被新版本替换");
        }
        menuMapper.disableByModuleId(moduleKey);
    }

    public List<AiModuleVersionSummary> versions(String moduleKey) {
        DynamicModuleDO current = mapper.selectByModuleKey(moduleKey);
        Integer currentVersionNo = current == null ? null : current.getCurrentVersionNo();
        return versionMapper.selectByModuleKey(moduleKey).stream()
                .map(version -> new AiModuleVersionSummary(
                        version.getModuleKey(),
                        version.getModuleName(),
                        version.getVersionNo(),
                        version.getTaskNo(),
                        version.getSchemaHash(),
                        Objects.equals(version.getVersionNo(), currentVersionNo),
                        version.getCreateTime()
                ))
                .toList();
    }

    public Map<String, Object> designerMetadata(String moduleKey) {
        DynamicModuleDO current = requireCurrentModule(moduleKey);
        return readJson(current.getMetadataJson());
    }

    @Transactional
    public Map<String, Object> saveDesignerMetadata(String moduleKey, Map<String, Object> metadata) {
        DynamicModuleDO current = requireCurrentModule(moduleKey);
        DynamicModuleVersionDO version = versionMapper.selectByModuleKeyAndVersionNo(
                moduleKey,
                current.getCurrentVersionNo()
        );
        if (version == null) {
            throw new ApiException(404, "动态模块当前版本不存在");
        }
        validateDesignerMetadata(moduleKey, metadata);
        Map<String, Object> schema = readJson(version.getSchemaJson());
        String moduleName = firstText(
                Objects.toString(metadata.get("label"), ""),
                Objects.toString(metadata.get("id"), ""),
                current.getModuleName(),
                moduleKey
        );
        String taskNo = firstText(current.getTaskNo(), version.getTaskNo());
        if (taskNo.isBlank()) {
            throw new ApiException(400, "动态模块缺少可关联的生成任务号");
        }
        applyModule(moduleKey, moduleName, taskNo, metadata, schema);
        return Map.of(
                "moduleKey", moduleKey,
                "moduleName", moduleName,
                "taskNo", taskNo,
                "metadata", metadata
        );
    }

    @Transactional
    public Map<String, Object> restoreVersion(String moduleKey, int versionNo) {
        DynamicModuleVersionDO version = versionMapper.selectByModuleKeyAndVersionNo(moduleKey, versionNo);
        if (version == null) {
            throw new ApiException(404, "动态模块版本不存在");
        }
        Map<String, Object> metadata = readJson(version.getMetadataJson());
        Map<String, Object> schema = readJson(version.getSchemaJson());
        dynamicTableService.ensureTable(moduleKey, schema);

        DynamicModuleDO entity = new DynamicModuleDO();
        entity.setModuleKey(version.getModuleKey());
        entity.setModuleName(version.getModuleName());
        entity.setTaskNo(version.getTaskNo());
        entity.setCurrentVersionNo(version.getVersionNo());
        entity.setSchemaHash(version.getSchemaHash());
        entity.setMetadataJson(version.getMetadataJson());
        mapper.upsert(entity);
        registerPermissions(moduleKey, version.getModuleName(), metadata);
        upsertMenu(metadata, moduleKey, version.getModuleName());

        return Map.of(
                "moduleKey", moduleKey,
                "versionNo", versionNo,
                "taskNo", version.getTaskNo(),
                "metadata", metadata
        );
    }

    private void upsertMenu(Map<String, Object> metadata, String moduleKey, String moduleName) {
        MenuDO existing = menuMapper.selectByModuleId(moduleKey);
        if (existing != null) {
            existing.setLabel(Objects.toString(metadata.getOrDefault("label", moduleName), moduleName));
            existing.setIcon(Objects.toString(metadata.getOrDefault("icon", "sparkles"), "sparkles"));
            existing.setPath(Objects.toString(metadata.getOrDefault("path", "/ai/" + moduleKey), "/ai/" + moduleKey));
            existing.setModuleId(moduleKey);
            existing.setPermissionCode(viewPermission(metadata, moduleKey));
            existing.setEnabled(true);
            menuMapper.update(existing);
            return;
        }

        MenuDO menu = new MenuDO();
        menu.setMenuKey("ai-" + moduleKey);
        menu.setParentKey("system");
        menu.setLabel(Objects.toString(metadata.getOrDefault("label", moduleName), moduleName));
        menu.setIcon(Objects.toString(metadata.getOrDefault("icon", "sparkles"), "sparkles"));
        menu.setPath(Objects.toString(metadata.getOrDefault("path", "/ai/" + moduleKey), "/ai/" + moduleKey));
        menu.setModuleId(moduleKey);
        menu.setPermissionCode(viewPermission(metadata, moduleKey));
        menu.setSortOrder(1000);
        menu.setHidden(false);
        menu.setEnabled(true);
        menu.setSystemBuiltin(false);
        menuMapper.insert(menu);
    }

    private void validateDesignerMetadata(String moduleKey, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            throw new ApiException(400, "设计器元数据不能为空");
        }
        String id = Objects.toString(metadata.get("id"), "").trim();
        if (!moduleKey.equals(id)) {
            throw new ApiException(400, "设计器元数据模块标识不一致");
        }
        Object crudObject = metadata.get("crud");
        if (!(crudObject instanceof Map<?, ?> crud)) {
            throw new ApiException(400, "设计器元数据缺少 CRUD 配置");
        }
        Object columns = crud.get("columns");
        Object form = crud.get("form");
        if (!(columns instanceof List<?> columnList) || columnList.isEmpty()
                || !(form instanceof List<?> formList) || formList.isEmpty()) {
            throw new ApiException(400, "设计器元数据必须保留表格列和表单字段");
        }
    }

    private DynamicModuleDO requireCurrentModule(String moduleKey) {
        DynamicModuleDO current = mapper.selectByModuleKey(moduleKey);
        if (current == null || !Boolean.TRUE.equals(current.getEnabled())) {
            throw new ApiException(404, "动态模块不存在或未启用");
        }
        return current;
    }

    private String viewPermission(Map<String, Object> metadata, String moduleKey) {
        Object permissions = metadata.get("permissions");
        if (permissions instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(permission -> permission.endsWith(":view"))
                    .findFirst()
                    .orElse(moduleKey + ":view");
        }
        return moduleKey + ":view";
    }

    private void registerPermissions(String moduleKey, String moduleName, Map<String, Object> metadata) {
        List<String> permissions = permissions(metadata, moduleKey);
        for (String permission : permissions) {
            String action = permission.substring(permission.indexOf(':') + 1);
            PermissionDO entity = new PermissionDO();
            entity.setPermissionCode(permission);
            entity.setModuleKey(moduleKey);
            entity.setModuleName(moduleName);
            entity.setActionCode(action);
            entity.setActionName(actionName(action));
            entity.setSource("ai-dynamic");
            permissionMapper.upsert(entity);
            rolePermissionMapper.upsertRolePermission("super_admin", permission);
            rolePermissionMapper.upsertRolePermission("admin", permission);
        }
    }

    private List<String> permissions(Map<String, Object> metadata, String moduleKey) {
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

    private String actionName(String action) {
        return switch (action) {
            case "view" -> "查看";
            case "add" -> "新增";
            case "edit" -> "编辑";
            case "delete" -> "删除";
            case "export" -> "导出";
            default -> action;
        };
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private AiApplyPlan.Operation operation(
            String category,
            String action,
            String target,
            String description,
            String riskLevel
    ) {
        return new AiApplyPlan.Operation(category, action, target, description, riskLevel);
    }

    private int riskScore(List<AiApplyPlan.Operation> operations, List<String> warnings, int validationScore) {
        long high = operations.stream().filter(operation -> "high".equals(operation.riskLevel())).count();
        long medium = operations.stream().filter(operation -> "medium".equals(operation.riskLevel())).count();
        return (int) Math.max(0, Math.min(validationScore, 100 - high * 30 - medium * 8 - warnings.size() * 3));
    }

    private String riskLevel(List<AiApplyPlan.Operation> operations, int riskScore) {
        if (operations.stream().anyMatch(operation -> "high".equals(operation.riskLevel())) || riskScore < 60) {
            return "high";
        }
        if (operations.stream().anyMatch(operation -> "medium".equals(operation.riskLevel())) || riskScore < 85) {
            return "medium";
        }
        return "low";
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new ApiException(500, "动态模块元数据解析失败");
        }
    }

    private String writeJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
        } catch (Exception ex) {
            throw new ApiException(500, "动态模块元数据序列化失败");
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ApiException(500, "动态模块版本指纹生成失败");
        }
    }
}
