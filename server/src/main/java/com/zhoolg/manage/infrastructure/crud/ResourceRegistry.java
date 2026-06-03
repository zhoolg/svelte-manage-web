package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zhoolg.manage.infrastructure.crud.FieldMaps.*;

@Component
public class ResourceRegistry {
    private final Map<String, ResourceDefinition> resources = new LinkedHashMap<>();
    private final DynamicResourceDefinitionProvider dynamicResourceDefinitionProvider;

    public ResourceRegistry(DynamicResourceDefinitionProvider dynamicResourceDefinitionProvider) {
        this.dynamicResourceDefinitionProvider = dynamicResourceDefinitionProvider;
        register(admins());
        register(applications());
        register(faq());
        register(logs());
        register(dict());
        register(settings());
        register(menus());
    }

    public List<ResourceDefinition> all() {
        return List.copyOf(resources.values());
    }

    public ResourceDefinition require(String key) {
        String normalizedKey = normalizeKey(key);
        ResourceDefinition definition = resources.get(normalizedKey);
        if (definition == null) {
            return dynamicResourceDefinitionProvider.find(normalizedKey)
                    .orElseThrow(() -> new ApiException(404, "资源不存在"));
        }
        return definition;
    }

    public boolean exists(String key) {
        String normalizedKey = normalizeKey(key);
        return resources.containsKey(normalizedKey) || dynamicResourceDefinitionProvider.exists(normalizedKey);
    }

    public String normalizeKey(String key) {
        return switch (key) {
            case "admin" -> "admins";
            case "application" -> "applications";
            case "interlocution" -> "faq";
            case "log" -> "logs";
            case "menu" -> "menus";
            default -> key;
        };
    }

    private void register(ResourceDefinition definition) {
        resources.put(definition.key(), definition);
    }

    private ResourceDefinition admins() {
        List<Map<String, Object>> statusOptions = options("启用", "enabled", "禁用", "disabled");
        return new ResourceDefinition(
                "admins",
                "/admins",
                "管理员",
                "admin",
                "id",
                List.of("username", "name"),
                List.of("status", "role"),
                List.of("username", "password", "name", "role", "status"),
                List.of("id", "name", "role", "status"),
                List.of(
                        column("id", "ID", 80, null),
                        column("username", "用户名", 120, null),
                        column("name", "姓名", 120, null),
                        column("role", "角色", 120, null),
                        statusColumn("status", "状态", Map.of("enabled", Map.of("label", "启用", "color", "success"), "disabled", Map.of("label", "禁用", "color", "danger"))),
                        column("createTime", "创建时间", 180, "datetime")
                ),
                List.of(search("username", "用户名", "input"), search("name", "姓名", "input")),
                List.of(
                        form("username", "用户名", "input", true),
                        form("name", "姓名", "input", true),
                        form("password", "密码", "input", true),
                        selectForm("role", "角色", true, "admin", options("超级管理员", "super_admin", "管理员", "admin", "运营人员", "operator", "查看者", "viewer")),
                        selectForm("status", "状态", false, "enabled", statusOptions)
                )
        );
    }

    private ResourceDefinition applications() {
        return new ResourceDefinition(
                "applications",
                "/applications",
                "租房申请",
                "application",
                "id",
                List.of("applicant", "phone", "property"),
                List.of("status"),
                List.of("applicant", "phone", "property", "moveInDate", "leasePeriod", "status"),
                List.of("id", "applicant", "phone", "property", "moveInDate", "leasePeriod", "status"),
                List.of(
                        column("id", "ID", 80, null),
                        column("applicant", "申请人", 120, null),
                        column("phone", "联系电话", 140, null),
                        column("property", "意向房源", 180, null),
                        column("moveInDate", "入住时间", 120, "date"),
                        column("leasePeriod", "租赁期限", 120, null),
                        statusColumn("status", "状态", Map.of("pending", Map.of("label", "待审核", "color", "warning"), "approved", Map.of("label", "已通过", "color", "success"), "rejected", Map.of("label", "已拒绝", "color", "danger"))),
                        column("createTime", "申请时间", 180, "datetime")
                ),
                List.of(search("applicant", "申请人", "input"), selectSearch("status", "状态", options("待审核", "pending", "已通过", "approved", "已拒绝", "rejected"))),
                List.of()
        );
    }

    private ResourceDefinition faq() {
        return new ResourceDefinition(
                "faq",
                "/faq",
                "问答管理",
                "faq",
                "id",
                List.of("question", "answer"),
                List.of(),
                List.of("question", "answer", "sortOrder"),
                List.of("id", "question", "answer", "sortOrder"),
                List.of(column("id", "ID", 80, null), column("question", "问题", 200, null), column("answer", "答案", 300, null), column("sortOrder", "排序", 80, null), column("createTime", "创建时间", 180, "datetime")),
                List.of(search("question", "问题", "input")),
                List.of(form("question", "问题", "input", true), form("answer", "答案", "textarea", true), form("sortOrder", "排序", "number", false))
        );
    }

    private ResourceDefinition logs() {
        return new ResourceDefinition("logs", "/logs", "系统日志", "log", "id", List.of("operator", "action", "module"), List.of("level"), List.of(), List.of(), List.of(column("id", "ID", 80, null), column("operator", "操作人", 120, null), column("module", "模块", 120, null), column("action", "操作", 120, null), column("ip", "IP", 140, null), column("createTime", "时间", 180, "datetime")), List.of(search("operator", "操作人", "input")), List.of());
    }

    private ResourceDefinition dict() {
        return new ResourceDefinition("dict", "/dict", "字典管理", "dict", "id", List.of("name", "code"), List.of("status"), List.of("name", "code", "value", "status"), List.of("id", "name", "code", "value", "status"), List.of(column("id", "ID", 80, null), column("name", "字典名称", 160, null), column("code", "字典编码", 160, null), column("value", "字典值", 200, null), statusColumn("status", "状态", Map.of(1, Map.of("label", "启用", "color", "success"), 0, Map.of("label", "禁用", "color", "danger")))), List.of(search("name", "字典名称", "input")), List.of(form("name", "字典名称", "input", true), form("code", "字典编码", "input", true), form("value", "字典值", "input", true), selectForm("status", "状态", false, 1, options("启用", 1, "禁用", 0))));
    }

    private ResourceDefinition settings() {
        return new ResourceDefinition("settings", "/settings", "系统设置", "settings", "id", List.of("name", "key", "description"), List.of(), List.of("name", "key", "value", "description"), List.of("id", "name", "key", "value", "description"), List.of(column("id", "ID", 80, null), column("name", "配置名称", 160, null), column("key", "配置键", 180, null), column("value", "配置值", 240, null), column("description", "说明", 240, null), column("systemBuiltin", "内置", 80, null)), List.of(search("name", "配置名称", "input"), search("key", "配置键", "input")), List.of(form("name", "配置名称", "input", true), form("key", "配置键", "input", true), form("value", "配置值", "input", true), form("description", "说明", "textarea", false)));
    }

    private ResourceDefinition menus() {
        List<Map<String, Object>> yesNoOptions = options("是", true, "否", false);
        List<Map<String, Object>> iconOptions = options(
                "圆点", "circle",
                "首页", "home",
                "用户", "users",
                "用户头像", "user-circle",
                "系统", "cog",
                "设置", "settings",
                "列表树", "list-tree",
                "日志", "file-text",
                "字典", "book-open",
                "申请单", "clipboard-check",
                "客服", "message-circle",
                "AI", "sparkles"
        );
        List<Map<String, Object>> permissionOptions = options(
                "控制台：查看", "dashboard:view",
                "管理员：查看", "admin:view",
                "管理员：新增", "admin:add",
                "管理员：编辑", "admin:edit",
                "管理员：删除", "admin:delete",
                "租房申请：查看", "application:view",
                "租房申请：新增", "application:add",
                "租房申请：编辑", "application:edit",
                "租房申请：删除", "application:delete",
                "常见问题：查看", "faq:view",
                "常见问题：新增", "faq:add",
                "常见问题：编辑", "faq:edit",
                "常见问题：删除", "faq:delete",
                "数据字典：查看", "dict:view",
                "数据字典：新增", "dict:add",
                "数据字典：编辑", "dict:edit",
                "数据字典：删除", "dict:delete",
                "系统设置：查看", "settings:view",
                "系统设置：编辑", "settings:edit",
                "菜单管理：查看", "menu:view",
                "菜单管理：新增", "menu:add",
                "菜单管理：编辑", "menu:edit",
                "菜单管理：删除", "menu:delete",
                "操作日志：查看", "log:view",
                "个人中心：查看", "profile:view"
        );
        return new ResourceDefinition(
                "menus",
                "/menus",
                "菜单管理",
                "menu",
                "id",
                List.of("menuKey", "label", "path"),
                List.of("enabled", "hidden"),
                List.of("menuKey", "parentKey", "label", "icon", "path", "moduleId", "permissionCode", "sortOrder", "hidden", "enabled"),
                List.of("id", "menuKey", "parentKey", "label", "icon", "path", "moduleId", "sortOrder", "hidden", "enabled"),
                List.of(
                        column("id", "ID", 80, null),
                        column("menuKey", "内部ID", 150, null),
                        column("parentKey", "上级菜单", 140, null),
                        column("label", "显示名称", 180, null),
                        column("icon", "图标", 120, null),
                        column("path", "点击路径", 180, null),
                        column("moduleId", "页面模块", 160, null),
                        column("sortOrder", "排序", 80, null),
                        statusColumn("hidden", "侧边栏", Map.of(true, Map.of("label", "隐藏", "color", "warning"), false, Map.of("label", "显示", "color", "success"))),
                        statusColumn("enabled", "状态", Map.of(true, Map.of("label", "启用", "color", "success"), false, Map.of("label", "禁用", "color", "danger"))),
                        statusColumn("systemBuiltin", "来源", Map.of(true, Map.of("label", "系统", "color", "info"), false, Map.of("label", "自定义", "color", "success")))
                ),
                List.of(
                        search("menuKey", "内部ID", "input"),
                        search("label", "显示名称", "input"),
                        search("path", "点击路径", "input")
                ),
                List.of(
                        withTip(form("menuKey", "内部ID", "input", true), "例如 menus、ai-orders，只能作为唯一标识使用"),
                        withTip(form("parentKey", "上级菜单", "input", false), "留空就是一级菜单；填 system、tenant、service 可挂到对应分组下"),
                        withTip(form("label", "显示名称", "input", true), "可以直接填中文，也可以填 menu.xxx 多语言 key"),
                        withTip(selectForm("icon", "图标", false, "circle", iconOptions), "选择侧边栏显示的图标"),
                        withTip(form("path", "点击路径", "input", false), "有页面时填写 /xxx；纯分组菜单可填写分组路径或留空"),
                        withTip(form("moduleId", "页面模块", "input", false), "绑定要打开的页面模块，例如 admins、logs、settings；纯分组留空"),
                        withTip(checkboxForm("permissionCode", "可见权限", permissionOptions), "选择访问该菜单需要的权限；不选表示登录后可见"),
                        withTip(form("sortOrder", "显示顺序", "number", false), "数字越小越靠前"),
                        withTip(selectForm("hidden", "是否隐藏", false, false, yesNoOptions), "隐藏后不会出现在侧边栏，但保留配置"),
                        withTip(selectForm("enabled", "是否启用", false, true, yesNoOptions), "禁用后该菜单不会参与前端菜单生成")
                )
        );
    }

    private Map<String, Object> withTip(Map<String, Object> field, String tip) {
        field.put("tip", tip);
        return field;
    }

    private Map<String, Object> checkboxForm(String field, String label, List<Map<String, Object>> options) {
        Map<String, Object> map = form(field, label, "checkbox", false);
        map.put("options", options);
        return map;
    }
}
