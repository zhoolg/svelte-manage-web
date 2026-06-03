package com.zhoolg.manage.mapper.struct;

import com.zhoolg.manage.entity.pojo.ApplicationDO;
import com.zhoolg.manage.entity.pojo.AuditLogDO;
import com.zhoolg.manage.entity.pojo.DictDO;
import com.zhoolg.manage.entity.pojo.FaqDO;
import com.zhoolg.manage.entity.pojo.MenuDO;
import com.zhoolg.manage.entity.pojo.SystemSettingDO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ResourceStructMapper {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> toApplicationRow(ApplicationDO entity) {
        Map<String, Object> row = baseRow(entity.getId(), entity.getCreateTime(), entity.getUpdateTime());
        row.put("applicant", entity.getApplicant());
        row.put("phone", entity.getPhone());
        row.put("property", entity.getProperty());
        row.put("moveInDate", entity.getMoveInDate());
        row.put("leasePeriod", entity.getLeasePeriod());
        row.put("status", entity.getStatus());
        return stripNulls(row);
    }

    public Map<String, Object> toFaqRow(FaqDO entity) {
        Map<String, Object> row = baseRow(entity.getId(), entity.getCreateTime(), entity.getUpdateTime());
        row.put("question", entity.getQuestion());
        row.put("answer", entity.getAnswer());
        row.put("sortOrder", entity.getSortOrder());
        return stripNulls(row);
    }

    public Map<String, Object> toDictRow(DictDO entity) {
        Map<String, Object> row = baseRow(entity.getId(), entity.getCreateTime(), entity.getUpdateTime());
        row.put("name", entity.getName());
        row.put("code", entity.getCode());
        row.put("value", entity.getValue());
        row.put("status", entity.getStatus());
        return stripNulls(row);
    }

    public Map<String, Object> toAuditLogRow(AuditLogDO entity) {
        Map<String, Object> row = baseRow(entity.getId(), entity.getCreateTime(), null);
        row.put("username", entity.getUsername());
        row.put("operator", entity.getUsername());
        row.put("module", entity.getModule());
        row.put("action", entity.getAction());
        row.put("description", entity.getDescription());
        row.put("ip", entity.getIp() == null ? "" : entity.getIp());
        row.put("status", entity.getResult());
        row.put("message", entity.getDescription());
        return stripNulls(row);
    }

    public Map<String, Object> toSettingRow(SystemSettingDO entity) {
        Map<String, Object> row = baseRow(entity.getId(), entity.getCreateTime(), entity.getUpdateTime());
        row.put("name", entity.getSettingName());
        row.put("key", entity.getSettingKey());
        row.put("value", entity.getSettingValue());
        row.put("description", entity.getDescription());
        row.put("systemBuiltin", entity.isSystemBuiltin());
        return stripNulls(row);
    }

    public Map<String, Object> toMenuRow(MenuDO entity) {
        Map<String, Object> row = baseRow(entity.getId(), entity.getCreateTime(), entity.getUpdateTime());
        row.put("menuKey", entity.getMenuKey());
        row.put("parentKey", entity.getParentKey());
        row.put("label", entity.getLabel());
        row.put("icon", entity.getIcon());
        row.put("path", entity.getPath());
        row.put("moduleId", entity.getModuleId());
        row.put("permissionCode", entity.getPermissionCode());
        row.put("sortOrder", entity.getSortOrder());
        row.put("hidden", Boolean.TRUE.equals(entity.getHidden()));
        row.put("enabled", !Boolean.FALSE.equals(entity.getEnabled()));
        row.put("systemBuiltin", Boolean.TRUE.equals(entity.getSystemBuiltin()));
        return stripNulls(row);
    }

    private Map<String, Object> baseRow(Long id, LocalDateTime createTime, LocalDateTime updateTime) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        if (createTime != null) {
            row.put("createTime", format(createTime));
        }
        if (updateTime != null) {
            row.put("updateTime", format(updateTime));
        }
        return row;
    }

    private Map<String, Object> stripNulls(Map<String, Object> row) {
        row.entrySet().removeIf(entry -> entry.getValue() == null);
        return row;
    }

    private String format(LocalDateTime time) {
        return DATE_TIME_FORMATTER.format(time);
    }
}
