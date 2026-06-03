package com.zhoolg.manage.controller.admin;

import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.infrastructure.crud.ResourceDefinition;
import com.zhoolg.manage.infrastructure.crud.ResourceRegistry;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.service.DynamicModuleService;
import com.zhoolg.manage.service.IAuthService;
import com.zhoolg.manage.service.IPermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/meta")
public class MetadataController {
    private final ResourceRegistry registry;
    private final DynamicModuleService dynamicModuleService;
    private final IAuthService authService;
    private final IPermissionService permissionService;
    private final MenuMapper menuMapper;

    public MetadataController(ResourceRegistry registry, DynamicModuleService dynamicModuleService, IAuthService authService, IPermissionService permissionService, MenuMapper menuMapper) {
        this.registry = registry;
        this.dynamicModuleService = dynamicModuleService;
        this.authService = authService;
        this.permissionService = permissionService;
        this.menuMapper = menuMapper;
    }

    @GetMapping("/modules")
    public ApiResponse<List<Map<String, Object>>> modules() {
        authService.requireUser();
        List<Map<String, Object>> modules = new java.util.ArrayList<>();
        modules.add(customModule("home", "menu.home", "home", "/", "Dashboard", List.of("dashboard:view")));
        modules.add(customModule("preferences", "menu.preferences", "palette", "/preferences", "Settings", List.of()));
        modules.addAll(registry.all().stream()
                .map(this::toModule)
                .toList());
        modules.add(customModule("system-status", "menu.systemStatus", "activity", "/system-status", "SystemStatus", List.of("settings:view")));
        modules.add(customModule("ai-modules", "menu.aiModules", "sparkles", "/ai-modules", "AiModules", List.of("settings:edit")));
        modules.addAll(dynamicModuleService.enabledModules());
        return ApiResponse.ok(modules);
    }

    @GetMapping("/menu")
    public ApiResponse<List<Map<String, Object>>> menu() {
        authService.requireUser();
        List<MenuDO> rows = menuMapper.selectEnabled();
        if (!rows.isEmpty()) {
            return ApiResponse.ok(menuTree(rows));
        }
        return ApiResponse.ok(defaultMenuTree());
    }

    private List<Map<String, Object>> menuTree(List<MenuDO> rows) {
        Map<String, Map<String, Object>> nodeMap = new LinkedHashMap<>();
        rows.forEach(row -> nodeMap.put(row.getMenuKey(), toMenuNode(row)));

        List<Map<String, Object>> roots = new ArrayList<>();
        rows.forEach(row -> {
            Map<String, Object> node = nodeMap.get(row.getMenuKey());
            String parentKey = row.getParentKey();
            if (parentKey == null || parentKey.isBlank() || !nodeMap.containsKey(parentKey)) {
                roots.add(node);
                return;
            }
            childrenOf(nodeMap.get(parentKey)).add(node);
        });

        appendMissingBuiltinMenus(roots, rows);
        appendUnmanagedDynamicModules(roots, rows);
        return roots;
    }

    private void appendMissingBuiltinMenus(List<Map<String, Object>> roots, List<MenuDO> rows) {
        Set<String> menuKeys = rows.stream()
                .map(MenuDO::getMenuKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> moduleIds = rows.stream()
                .map(MenuDO::getModuleId)
                .filter(Objects::nonNull)
                .filter(moduleId -> !moduleId.isBlank())
                .collect(Collectors.toSet());

        if (!menuKeys.contains("home") && !moduleIds.contains("home")) {
            roots.add(customMenuNode("home", "menu.home", "home", "/", "home", List.of("dashboard:view")));
        }

        Map<String, Object> tenant = ensureRootGroup(roots, menuKeys, "tenant", "menu.tenant", "users", "/tenant");
        appendMissingChild(tenant, menuKeys, moduleIds, "applications", "menu.applications", "clipboard-check", "/applications", "applications", List.of("application:view"));

        Map<String, Object> service = ensureRootGroup(roots, menuKeys, "service", "menu.service", "message-circle", "/service");
        appendMissingChild(service, menuKeys, moduleIds, "faq", "menu.faq", "question-circle", "/faq", "faq", List.of("faq:view"));

        Map<String, Object> system = ensureRootGroup(roots, menuKeys, "system", "menu.system", "cog", "/system");
        appendMissingChild(system, menuKeys, moduleIds, "admins", "menu.admins", "user-circle", "/admins", "admins", List.of("admin:view"));
        appendMissingChild(system, menuKeys, moduleIds, "logs", "menu.logs", "file-text", "/logs", "logs", List.of("log:view"));
        appendMissingChild(system, menuKeys, moduleIds, "dict", "menu.dict", "book-open", "/dict", "dict", List.of("dict:view"));
        appendMissingChild(system, menuKeys, moduleIds, "settings", "menu.settings", "settings", "/settings", "settings", List.of("settings:view"));
        appendMissingChild(system, menuKeys, moduleIds, "menus", "menu.menus", "list-tree", "/menus", "menus", List.of("menu:view"));
        appendMissingChild(system, menuKeys, moduleIds, "ai-modules", "menu.aiModules", "sparkles", "/ai-modules", "ai-modules", List.of("settings:edit"));
    }

    private Map<String, Object> ensureRootGroup(
            List<Map<String, Object>> roots,
            Set<String> menuKeys,
            String id,
            String label,
            String icon,
            String path
    ) {
        return roots.stream()
                .filter(root -> id.equals(root.get("id")))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> group = groupNode(id, label, icon, path);
                    roots.add(group);
                    menuKeys.add(id);
                    return group;
                });
    }

    private void appendMissingChild(
            Map<String, Object> parent,
            Set<String> menuKeys,
            Set<String> moduleIds,
            String id,
            String label,
            String icon,
            String path,
            String module,
            List<String> permissions
    ) {
        if (menuKeys.contains(id) || moduleIds.contains(module)) {
            return;
        }
        childrenOf(parent).add(customMenuNode(id, label, icon, path, module, permissions));
        menuKeys.add(id);
        moduleIds.add(module);
    }

    private Map<String, Object> customMenuNode(
            String id,
            String label,
            String icon,
            String path,
            String module,
            List<String> permissions
    ) {
        Map<String, Object> node = groupNode(id, label, icon, path);
        node.remove("children");
        node.put("module", module);
        if (!permissions.isEmpty()) {
            node.put("permissions", permissions);
        }
        return node;
    }

    private void appendUnmanagedDynamicModules(List<Map<String, Object>> roots, List<MenuDO> rows) {
        Set<String> managedModules = rows.stream()
                .map(MenuDO::getModuleId)
                .filter(Objects::nonNull)
                .filter(moduleId -> !moduleId.isBlank())
                .collect(Collectors.toSet());
        List<Map<String, Object>> unmanaged = dynamicModuleService.enabledModules().stream()
                .filter(module -> !managedModules.contains(Objects.toString(module.get("id"), "")))
                .map(this::dynamicModuleMenuNode)
                .toList();
        if (unmanaged.isEmpty()) {
            return;
        }

        Map<String, Object> system = roots.stream()
                .filter(root -> "system".equals(root.get("id")))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> fallback = groupNode("system", "menu.system", "cog", "/system");
                    roots.add(fallback);
                    return fallback;
                });
        childrenOf(system).addAll(unmanaged);
    }

    private Map<String, Object> toMenuNode(MenuDO row) {
        Map<String, Object> node = groupNode(row.getMenuKey(), row.getLabel(), row.getIcon(), row.getPath());
        if (row.getModuleId() != null && !row.getModuleId().isBlank()) {
            node.put("module", row.getModuleId());
        }
        if (Boolean.TRUE.equals(row.getHidden())) {
            node.put("hidden", true);
        }
        List<String> permissions = splitPermissions(row.getPermissionCode());
        if (!permissions.isEmpty()) {
            node.put("permissions", permissions);
        }
        return node;
    }

    private Map<String, Object> dynamicModuleMenuNode(Map<String, Object> module) {
        Map<String, Object> node = new LinkedHashMap<>();
        String id = Objects.toString(module.get("id"), "");
        node.put("id", "ai-" + id);
        node.put("label", Objects.toString(module.get("label"), id));
        node.put("icon", Objects.toString(module.get("icon"), "sparkles"));
        node.put("path", Objects.toString(module.get("path"), "/ai/" + id));
        node.put("module", id);
        Object permissions = module.get("permissions");
        if (permissions instanceof List<?> list && !list.isEmpty()) {
            node.put("permissions", list);
        }
        return node;
    }

    private List<String> splitPermissions(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(permissionCode.split(","))
                .map(String::trim)
                .filter(permission -> !permission.isBlank())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> childrenOf(Map<String, Object> node) {
        Object children = node.get("children");
        if (children instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        List<Map<String, Object>> created = new ArrayList<>();
        node.put("children", created);
        return created;
    }

    private Map<String, Object> groupNode(String id, String label, String icon, String path) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("label", label);
        node.put("icon", icon == null || icon.isBlank() ? "circle" : icon);
        if (path != null && !path.isBlank()) {
            node.put("path", path);
        }
        node.put("children", new ArrayList<Map<String, Object>>());
        return node;
    }

    private List<Map<String, Object>> defaultMenuTree() {
        List<String> systemChildren = new ArrayList<>(List.of("admins", "logs", "dict", "settings", "menus", "ai-modules"));
        systemChildren.addAll(dynamicModuleService.enabledModules().stream()
                .map(module -> Objects.toString(module.get("id"), ""))
                .filter(id -> !id.isBlank())
                .toList());

        return List.of(
                group("home", "menu.home", "home", "/", List.of()),
                group("tenant", "menu.tenant", "users", "/tenant", List.of("applications")),
                group("service", "menu.service", "message-circle", "/service", List.of("faq")),
                group("system", "menu.system", "cog", "/system", systemChildren)
        );
    }

    @GetMapping("/permissions")
    public ApiResponse<Map<String, Object>> permissions() {
        authService.requireUser();
        return ApiResponse.ok(Map.of(
                "roles", permissionService.roleMetadata(),
                "permissions", permissionService.permissionCatalog(),
                "actions", List.of("view", "add", "edit", "delete", "export", "approve", "reject", "reset_password")
        ));
    }

    private Map<String, Object> toModule(ResourceDefinition resource) {
        return Map.of(
                "id", resource.key(),
                "label", "menu." + resource.key(),
                "icon", icon(resource.key()),
                "path", resource.path(),
                "permissions", List.of(resource.permission("view")),
                "crud", Map.of(
                        "title", resource.title(),
                        "apiBase", "/" + singular(resource.key()),
                        "restBase", resource.path(),
                        "columns", resource.columns(),
                        "search", resource.search(),
                        "form", resource.form(),
                        "showAdd", !resource.allowedCreateFields().isEmpty(),
                        "showExport", true,
                        "showSelection", !resource.allowedCreateFields().isEmpty(),
                        "actionPermissions", Map.of(
                                "add", resource.permission("add"),
                                "edit", resource.permission("edit"),
                                "delete", resource.permission("delete"),
                                "export", resource.permission("export"),
                                "view", resource.permission("view")
                        )
                )
        );
    }

    private Map<String, Object> group(String id, String label, String icon, String path, List<String> children) {
        return Map.of("id", id, "label", label, "icon", icon, "path", path, "children", children);
    }

    private Map<String, Object> customModule(
            String id,
            String label,
            String icon,
            String path,
            String customPage,
            List<String> permissions
    ) {
        return Map.of(
                "id", id,
                "label", label,
                "icon", icon,
                "path", path,
                "customPage", customPage,
                "permissions", permissions
        );
    }

    private String icon(String key) {
        return switch (key) {
            case "admins" -> "user-circle";
            case "applications" -> "clipboard-check";
            case "faq" -> "question-circle";
            case "logs" -> "file-text";
            case "dict" -> "book-open";
            case "settings" -> "settings";
            case "menus" -> "list-tree";
            case "system-status" -> "activity";
            default -> "circle";
        };
    }

    private String singular(String key) {
        return switch (key) {
            case "admins" -> "admin";
            case "applications" -> "application";
            case "logs" -> "log";
            default -> key;
        };
    }
}
