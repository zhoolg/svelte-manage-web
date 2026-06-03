package com.zhoolg.manage.infrastructure.crud;

import com.zhoolg.manage.entity.pojo.ApplicationDO;
import com.zhoolg.manage.entity.pojo.AuditLogDO;
import com.zhoolg.manage.entity.pojo.DictDO;
import com.zhoolg.manage.entity.pojo.FaqDO;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.entity.pojo.SystemSettingDO;
import com.zhoolg.manage.entity.base.PageResult;
import com.zhoolg.manage.entity.dto.AdminUserDTO;
import com.zhoolg.manage.entity.dto.CreateAdminDTO;
import com.zhoolg.manage.entity.dto.UpdateAdminDTO;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.mapper.ApplicationMapper;
import com.zhoolg.manage.mapper.AuditLogMapper;
import com.zhoolg.manage.mapper.DictMapper;
import com.zhoolg.manage.mapper.FaqMapper;
import com.zhoolg.manage.mapper.MenuMapper;
import com.zhoolg.manage.mapper.SystemSettingMapper;
import com.zhoolg.manage.mapper.struct.ResourceStructMapper;
import com.zhoolg.manage.service.IAdminUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Repository
public class DatabaseResourceRepository implements ResourceRepository {
    private static final List<String> STRUCTURED_RESOURCE_KEYS = List.of(
            "admins", "applications", "faq", "dict", "logs", "settings", "menus"
    );

    private final IAdminUserService adminUserService;
    private final ApplicationMapper applicationMapper;
    private final FaqMapper faqMapper;
    private final DictMapper dictMapper;
    private final AuditLogMapper auditLogMapper;
    private final SystemSettingMapper systemSettingMapper;
    private final MenuMapper menuMapper;
    private final ResourceStructMapper structMapper;
    private final DynamicTableService dynamicTableService;

    public DatabaseResourceRepository(IAdminUserService adminUserService,
                                      ApplicationMapper applicationMapper, FaqMapper faqMapper, DictMapper dictMapper,
                                      AuditLogMapper auditLogMapper, SystemSettingMapper systemSettingMapper,
                                      MenuMapper menuMapper, ResourceStructMapper structMapper,
                                      DynamicTableService dynamicTableService) {
        this.adminUserService = adminUserService;
        this.applicationMapper = applicationMapper;
        this.faqMapper = faqMapper;
        this.dictMapper = dictMapper;
        this.auditLogMapper = auditLogMapper;
        this.systemSettingMapper = systemSettingMapper;
        this.menuMapper = menuMapper;
        this.structMapper = structMapper;
        this.dynamicTableService = dynamicTableService;
    }

    @Override
    public List<Map<String, Object>> findAll(String key) {
        return switch (key) {
            case "admins" -> adminUserService.listAdmins(Map.of("pageNum", "1", "pageSize", String.valueOf(Integer.MAX_VALUE))).list();
            case "applications" -> applicationMapper.selectAll().stream().map(structMapper::toApplicationRow).toList();
            case "faq" -> faqMapper.selectAll().stream().map(structMapper::toFaqRow).toList();
            case "dict" -> dictMapper.selectAll().stream().map(structMapper::toDictRow).toList();
            case "logs" -> auditLogMapper.selectAll().stream().map(structMapper::toAuditLogRow).toList();
            case "settings" -> systemSettingMapper.selectAll().stream().map(structMapper::toSettingRow).toList();
            case "menus" -> menuMapper.selectAll().stream().map(structMapper::toMenuRow).toList();
            default -> throw new ApiException(400, "动态资源请使用分页接口读取");
        };
    }

    @Override
    public PageResult page(ResourceDefinition resource, Map<String, String> params) {
        int pageNum = parseInt(params.getOrDefault("pageNum", params.getOrDefault("page", "1")), 1);
        int pageSize = parseInt(params.getOrDefault("pageSize", params.getOrDefault("size", "10")), 10);
        String key = resource.key();
        if (!STRUCTURED_RESOURCE_KEYS.contains(key)) {
            return dynamicTableService.page(resource, params);
        }
        List<Map<String, Object>> list = switch (key) {
            case "admins" -> adminUserService.listAdmins(params).list();
            case "applications" -> applicationMapper.selectPage(params, resource.searchableFields(), pageNum, pageSize)
                    .stream().map(structMapper::toApplicationRow).toList();
            case "faq" -> faqMapper.selectPage(params, resource.searchableFields(), pageNum, pageSize)
                    .stream().map(structMapper::toFaqRow).toList();
            case "dict" -> dictMapper.selectPage(params, resource.searchableFields(), pageNum, pageSize)
                    .stream().map(structMapper::toDictRow).toList();
            case "logs" -> auditLogMapper.selectPage(params, resource.searchableFields(), pageNum, pageSize)
                    .stream().map(structMapper::toAuditLogRow).toList();
            case "settings" -> systemSettingMapper.selectPage(params, resource.searchableFields(), pageNum, pageSize)
                    .stream().map(structMapper::toSettingRow).toList();
            case "menus" -> menuMapper.selectPage(params, resource.searchableFields(), pageNum, pageSize)
                    .stream().map(structMapper::toMenuRow).toList();
            default -> throw new ApiException(404, "资源不存在");
        };
        long total = switch (key) {
            case "admins" -> adminUserService.listAdmins(params).total();
            case "applications" -> applicationMapper.count(params, resource.searchableFields());
            case "faq" -> faqMapper.count(params, resource.searchableFields());
            case "dict" -> dictMapper.count(params, resource.searchableFields());
            case "logs" -> auditLogMapper.count(params, resource.searchableFields());
            case "settings" -> systemSettingMapper.count(params, resource.searchableFields());
            case "menus" -> menuMapper.count(params, resource.searchableFields());
            default -> throw new ApiException(404, "资源不存在");
        };
        return new PageResult(list, total);
    }

    @Override
    public Map<String, Object> create(ResourceDefinition resource, Map<String, Object> payload) {
        return switch (resource.key()) {
            case "admins" -> createAdmin(payload);
            case "applications" -> createApplication(payload);
            case "faq" -> createFaq(payload);
            case "dict" -> createDict(payload);
            case "settings" -> createSetting(payload);
            case "menus" -> createMenu(payload);
            case "logs" -> throw new ApiException(400, "审计日志不可手工创建");
            default -> dynamicTableService.create(resource, payload);
        };
    }

    @Override
    public Map<String, Object> update(ResourceDefinition resource, Object id, Map<String, Object> payload) {
        return switch (resource.key()) {
            case "admins" -> updateAdmin(id, payload);
            case "applications" -> updateApplication(id, payload);
            case "faq" -> updateFaq(id, payload);
            case "dict" -> updateDict(id, payload);
            case "settings" -> updateSetting(id, payload);
            case "menus" -> updateMenu(id, payload);
            case "logs" -> throw new ApiException(400, "审计日志不可修改");
            default -> dynamicTableService.update(resource, id, payload);
        };
    }

    @Override
    public void delete(ResourceDefinition resource, Object id) {
        switch (resource.key()) {
            case "admins" -> adminUserService.deleteAdmin(parseId(id));
            case "applications" -> applicationMapper.deleteById(requireApplication(id).getId());
            case "faq" -> faqMapper.deleteById(requireFaq(id).getId());
            case "dict" -> dictMapper.deleteById(requireDict(id).getId());
            case "settings" -> deleteSetting(id);
            case "menus" -> deleteMenu(id);
            case "logs" -> throw new ApiException(400, "审计日志不可删除");
            default -> dynamicTableService.delete(resource, id);
        }
    }

    @Override
    public Map<String, Object> transitionWorkflow(
            ResourceDefinition resource,
            Object id,
            Map<String, Object> transition
    ) {
        if (STRUCTURED_RESOURCE_KEYS.contains(resource.key())) {
            throw new ApiException(400, "内置资源暂未启用动态工作流");
        }
        return dynamicTableService.transitionWorkflow(resource, id, transition);
    }

    private Map<String, Object> createAdmin(Map<String, Object> payload) {
        return dtoToRow(adminUserService.createAdmin(new CreateAdminDTO(
                requiredString(payload.get("username"), "用户名不能为空"),
                requiredString(payload.get("password"), "密码不能为空"),
                requiredString(payload.get("name"), "姓名不能为空"),
                requiredRole(payload)
        )));
    }

    private Map<String, Object> updateAdmin(Object id, Map<String, Object> payload) {
        String name = payload.containsKey("name") ? stringValue(payload.get("name")) : null;
        String roleCode = optionalRole(payload);
        Boolean enabled = optionalEnabled(payload);
        return dtoToRow(adminUserService.updateAdmin(parseId(id), new UpdateAdminDTO(name, roleCode, enabled)));
    }

    private Map<String, Object> dtoToRow(AdminUserDTO dto) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", dto.id());
        row.put("username", dto.username());
        row.put("name", dto.name());
        row.put("role", dto.roleCode());
        row.put("status", Boolean.TRUE.equals(dto.enabled()) ? "enabled" : "disabled");
        return stripNulls(row);
    }

    private Map<String, Object> createApplication(Map<String, Object> payload) {
        ApplicationDO entity = new ApplicationDO();
        entity.setApplicant(requiredString(payload.get("applicant"), "申请人不能为空"));
        entity.setPhone(requiredString(payload.get("phone"), "联系电话不能为空"));
        entity.setProperty(requiredString(payload.get("property"), "意向房源不能为空"));
        entity.setMoveInDate(blankToNull(stringValue(payload.get("moveInDate"))));
        entity.setLeasePeriod(blankToNull(stringValue(payload.get("leasePeriod"))));
        entity.setStatus(blankToDefault(payload.get("status"), "pending"));
        applicationMapper.insert(entity);
        return structMapper.toApplicationRow(applicationMapper.selectById(entity.getId()));
    }

    private Map<String, Object> updateApplication(Object id, Map<String, Object> payload) {
        ApplicationDO entity = requireApplication(id);
        if (payload.containsKey("applicant")) {
            entity.setApplicant(requiredString(payload.get("applicant"), "申请人不能为空"));
        }
        if (payload.containsKey("phone")) {
            entity.setPhone(requiredString(payload.get("phone"), "联系电话不能为空"));
        }
        if (payload.containsKey("property")) {
            entity.setProperty(requiredString(payload.get("property"), "意向房源不能为空"));
        }
        if (payload.containsKey("moveInDate")) {
            entity.setMoveInDate(blankToNull(stringValue(payload.get("moveInDate"))));
        }
        if (payload.containsKey("leasePeriod")) {
            entity.setLeasePeriod(blankToNull(stringValue(payload.get("leasePeriod"))));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(blankToDefault(payload.get("status"), "pending"));
        }
        applicationMapper.update(entity);
        return structMapper.toApplicationRow(applicationMapper.selectById(entity.getId()));
    }

    private Map<String, Object> createFaq(Map<String, Object> payload) {
        FaqDO entity = new FaqDO();
        entity.setQuestion(requiredString(payload.get("question"), "问题不能为空"));
        entity.setAnswer(requiredString(payload.get("answer"), "答案不能为空"));
        entity.setSortOrder(intValue(payload.get("sortOrder"), 0));
        faqMapper.insert(entity);
        return structMapper.toFaqRow(faqMapper.selectById(entity.getId()));
    }

    private Map<String, Object> updateFaq(Object id, Map<String, Object> payload) {
        FaqDO entity = requireFaq(id);
        if (payload.containsKey("question")) {
            entity.setQuestion(requiredString(payload.get("question"), "问题不能为空"));
        }
        if (payload.containsKey("answer")) {
            entity.setAnswer(requiredString(payload.get("answer"), "答案不能为空"));
        }
        if (payload.containsKey("sortOrder")) {
            entity.setSortOrder(intValue(payload.get("sortOrder"), 0));
        }
        faqMapper.update(entity);
        return structMapper.toFaqRow(faqMapper.selectById(entity.getId()));
    }

    private Map<String, Object> createDict(Map<String, Object> payload) {
        DictDO entity = new DictDO();
        entity.setName(requiredString(payload.get("name"), "字典名称不能为空"));
        entity.setCode(requiredString(payload.get("code"), "字典编码不能为空"));
        entity.setValue(requiredString(payload.get("value"), "字典值不能为空"));
        entity.setStatus(intValue(payload.get("status"), 1));
        try {
            dictMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(400, "字典编码已存在");
        }
        return structMapper.toDictRow(dictMapper.selectById(entity.getId()));
    }

    private Map<String, Object> updateDict(Object id, Map<String, Object> payload) {
        DictDO entity = requireDict(id);
        if (payload.containsKey("name")) {
            entity.setName(requiredString(payload.get("name"), "字典名称不能为空"));
        }
        if (payload.containsKey("code")) {
            entity.setCode(requiredString(payload.get("code"), "字典编码不能为空"));
        }
        if (payload.containsKey("value")) {
            entity.setValue(requiredString(payload.get("value"), "字典值不能为空"));
        }
        if (payload.containsKey("status")) {
            entity.setStatus(intValue(payload.get("status"), 1));
        }
        try {
            dictMapper.update(entity);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(400, "字典编码已存在");
        }
        return structMapper.toDictRow(dictMapper.selectById(entity.getId()));
    }

    private Map<String, Object> createSetting(Map<String, Object> payload) {
        SystemSettingDO entity = new SystemSettingDO();
        entity.setSettingName(requiredString(payload.get("name"), "配置名称不能为空"));
        entity.setSettingKey(requiredString(payload.get("key"), "配置键不能为空"));
        entity.setSettingValue(stringValue(payload.get("value")));
        entity.setDescription(blankToNull(stringValue(payload.get("description"))));
        entity.setSystemBuiltin(false);
        try {
            systemSettingMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(400, "配置键已存在");
        }
        return structMapper.toSettingRow(systemSettingMapper.selectById(entity.getId()));
    }

    private Map<String, Object> createMenu(Map<String, Object> payload) {
        MenuDO entity = new MenuDO();
        entity.setMenuKey(requiredString(payload.get("menuKey"), "菜单标识不能为空"));
        entity.setParentKey(blankToNull(stringValue(payload.get("parentKey"))));
        entity.setLabel(requiredString(payload.get("label"), "菜单名称不能为空"));
        entity.setIcon(blankToDefault(payload.get("icon"), "circle"));
        entity.setPath(blankToNull(stringValue(payload.get("path"))));
        entity.setModuleId(blankToNull(stringValue(payload.get("moduleId"))));
        entity.setPermissionCode(permissionOrNull(payload.get("permissionCode")));
        entity.setSortOrder(intValue(payload.get("sortOrder"), 0));
        entity.setHidden(boolValue(payload.get("hidden"), false));
        entity.setEnabled(boolValue(payload.get("enabled"), true));
        entity.setSystemBuiltin(false);
        try {
            menuMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(400, "菜单标识已存在");
        }
        return structMapper.toMenuRow(menuMapper.selectById(entity.getId()));
    }

    private Map<String, Object> updateSetting(Object id, Map<String, Object> payload) {
        SystemSettingDO entity = requireSetting(id);
        if (payload.containsKey("name")) {
            entity.setSettingName(requiredString(payload.get("name"), "配置名称不能为空"));
        }
        if (payload.containsKey("key")) {
            entity.setSettingKey(requiredString(payload.get("key"), "配置键不能为空"));
        }
        if (payload.containsKey("value")) {
            entity.setSettingValue(stringValue(payload.get("value")));
        }
        if (payload.containsKey("description")) {
            entity.setDescription(blankToNull(stringValue(payload.get("description"))));
        }
        try {
            systemSettingMapper.update(entity);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(400, "配置键已存在");
        }
        return structMapper.toSettingRow(systemSettingMapper.selectById(entity.getId()));
    }

    private Map<String, Object> updateMenu(Object id, Map<String, Object> payload) {
        MenuDO entity = requireMenu(id);
        if (payload.containsKey("menuKey")) {
            entity.setMenuKey(requiredString(payload.get("menuKey"), "菜单标识不能为空"));
        }
        if (payload.containsKey("parentKey")) {
            entity.setParentKey(blankToNull(stringValue(payload.get("parentKey"))));
        }
        if (payload.containsKey("label")) {
            entity.setLabel(requiredString(payload.get("label"), "菜单名称不能为空"));
        }
        if (payload.containsKey("icon")) {
            entity.setIcon(blankToDefault(payload.get("icon"), "circle"));
        }
        if (payload.containsKey("path")) {
            entity.setPath(blankToNull(stringValue(payload.get("path"))));
        }
        if (payload.containsKey("moduleId")) {
            entity.setModuleId(blankToNull(stringValue(payload.get("moduleId"))));
        }
        if (payload.containsKey("permissionCode")) {
            entity.setPermissionCode(permissionOrNull(payload.get("permissionCode")));
        }
        if (payload.containsKey("sortOrder")) {
            entity.setSortOrder(intValue(payload.get("sortOrder"), 0));
        }
        if (payload.containsKey("hidden")) {
            entity.setHidden(boolValue(payload.get("hidden"), false));
        }
        if (payload.containsKey("enabled")) {
            entity.setEnabled(boolValue(payload.get("enabled"), true));
        }
        try {
            menuMapper.update(entity);
        } catch (DuplicateKeyException ex) {
            throw new ApiException(400, "菜单标识已存在");
        }
        return structMapper.toMenuRow(menuMapper.selectById(entity.getId()));
    }

    private void deleteSetting(Object id) {
        SystemSettingDO entity = requireSetting(id);
        if (entity.isSystemBuiltin()) {
            throw new ApiException(400, "系统内置配置不可删除");
        }
        systemSettingMapper.deleteById(entity.getId());
    }

    private void deleteMenu(Object id) {
        MenuDO entity = requireMenu(id);
        if (Boolean.TRUE.equals(entity.getSystemBuiltin())) {
            throw new ApiException(400, "系统内置菜单不可删除");
        }
        menuMapper.deleteById(entity.getId());
    }

    private ApplicationDO requireApplication(Object id) {
        ApplicationDO entity = applicationMapper.selectById(parseId(id));
        if (entity == null) {
            throw new ApiException(404, "申请不存在");
        }
        return entity;
    }

    private FaqDO requireFaq(Object id) {
        FaqDO entity = faqMapper.selectById(parseId(id));
        if (entity == null) {
            throw new ApiException(404, "问答不存在");
        }
        return entity;
    }

    private DictDO requireDict(Object id) {
        DictDO entity = dictMapper.selectById(parseId(id));
        if (entity == null) {
            throw new ApiException(404, "字典不存在");
        }
        return entity;
    }

    private SystemSettingDO requireSetting(Object id) {
        SystemSettingDO entity = systemSettingMapper.selectById(parseId(id));
        if (entity == null) {
            throw new ApiException(404, "配置不存在");
        }
        return entity;
    }

    private MenuDO requireMenu(Object id) {
        MenuDO entity = menuMapper.selectById(parseId(id));
        if (entity == null) {
            throw new ApiException(404, "菜单不存在");
        }
        return entity;
    }

    private Long parseId(Object id) {
        try {
            return Long.valueOf(String.valueOf(id));
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "无效 ID");
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Math.max(Integer.parseInt(value), 1);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String requiredString(Object value, String message) {
        String text = stringValue(value);
        if (text.isBlank()) {
            throw new ApiException(400, message);
        }
        return text;
    }

    private String requiredRole(Map<String, Object> payload) {
        String role = optionalRole(payload);
        if (role == null || role.isBlank()) {
            throw new ApiException(400, "角色不能为空");
        }
        return role;
    }

    private String optionalRole(Map<String, Object> payload) {
        Object role = payload.containsKey("roleCode") ? payload.get("roleCode") : payload.get("role");
        String text = stringValue(role);
        return text.isBlank() ? null : text;
    }

    private Boolean optionalEnabled(Map<String, Object> payload) {
        if (payload.containsKey("enabled")) {
            return boolValue(payload.get("enabled"), true);
        }
        if (!payload.containsKey("status")) {
            return null;
        }
        Object status = payload.get("status");
        if (status instanceof Boolean bool) {
            return bool;
        }
        String text = stringValue(status);
        if (text.isBlank()) {
            return null;
        }
        if ("enabled".equalsIgnoreCase(text) || "1".equals(text) || "true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("disabled".equalsIgnoreCase(text) || "0".equals(text) || "false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new ApiException(400, "管理员状态格式不正确");
    }

    private String blankToDefault(Object value, String fallback) {
        String text = stringValue(value);
        return text.isBlank() ? fallback : text;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String permissionOrNull(Object value) {
        if (value instanceof Collection<?> values) {
            String joined = values.stream()
                    .map(item -> item == null ? "" : String.valueOf(item).trim())
                    .filter(item -> !item.isBlank())
                    .filter(item -> !"__none__".equals(item))
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(","));
            return joined.isBlank() ? null : joined;
        }
        String permission = stringValue(value);
        return permission.isBlank() || "__none__".equals(permission) ? null : permission;
    }

    private Integer intValue(Object value, int fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "数字格式不正确");
        }
    }

    private Boolean boolValue(Object value, boolean fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        if ("1".equals(text)) {
            return true;
        }
        if ("0".equals(text)) {
            return false;
        }
        return Boolean.parseBoolean(text);
    }

    private Map<String, Object> stripNulls(Map<String, Object> row) {
        row.entrySet().removeIf(entry -> entry.getValue() == null);
        return row;
    }

}
