package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.PermissionDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class PermissionMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PermissionDO> rowMapper = (rs, rowNum) -> {
        PermissionDO entity = new PermissionDO();
        entity.setId(rs.getLong("id"));
        entity.setPermissionCode(rs.getString("permission_code"));
        entity.setModuleKey(rs.getString("module_key"));
        entity.setModuleName(rs.getString("module_name"));
        entity.setActionCode(rs.getString("action_code"));
        entity.setActionName(rs.getString("action_name"));
        entity.setSource(rs.getString("source"));
        entity.setEnabled(rs.getBoolean("enabled"));
        entity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            entity.setUpdateTime(updateTime.toLocalDateTime());
        }
        return entity;
    };

    public PermissionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(PermissionDO entity) {
        jdbcTemplate.update(
                "INSERT INTO sys_permission(permission_code, module_key, module_name, action_code, action_name, source, enabled) "
                        + "VALUES (?, ?, ?, ?, ?, ?, TRUE) "
                        + "ON DUPLICATE KEY UPDATE module_name = VALUES(module_name), action_name = VALUES(action_name), "
                        + "source = VALUES(source), enabled = TRUE, update_time = CURRENT_TIMESTAMP",
                entity.getPermissionCode(),
                entity.getModuleKey(),
                entity.getModuleName(),
                entity.getActionCode(),
                entity.getActionName(),
                entity.getSource()
        );
    }

    public List<PermissionDO> selectEnabled() {
        return jdbcTemplate.query(
                "SELECT id, permission_code, module_key, module_name, action_code, action_name, source, "
                        + "enabled, create_time, update_time FROM sys_permission WHERE enabled = TRUE "
                        + "ORDER BY module_key ASC, id ASC",
                rowMapper
        );
    }
}
