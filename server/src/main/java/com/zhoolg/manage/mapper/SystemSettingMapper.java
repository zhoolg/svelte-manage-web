package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.SystemSettingDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SystemSettingMapper {
    private static final Map<String, String> QUERY_COLUMNS = columns();

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<SystemSettingDO> rowMapper = (rs, rowNum) -> {
        SystemSettingDO entity = new SystemSettingDO();
        entity.setId(rs.getLong("id"));
        entity.setSettingKey(rs.getString("setting_key"));
        entity.setSettingName(rs.getString("setting_name"));
        entity.setSettingValue(rs.getString("setting_value"));
        entity.setDescription(rs.getString("description"));
        entity.setSystemBuiltin(rs.getBoolean("system_builtin"));
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            entity.setCreateTime(createTime.toLocalDateTime());
        }
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            entity.setUpdateTime(updateTime.toLocalDateTime());
        }
        return entity;
    };

    public SystemSettingMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SystemSettingDO> selectAll() {
        return jdbcTemplate.query(
                "SELECT id, setting_key, setting_name, setting_value, description, system_builtin, create_time, update_time "
                        + "FROM sys_system_setting ORDER BY id DESC",
                rowMapper
        );
    }

    public SystemSettingDO selectById(Long id) {
        List<SystemSettingDO> rows = jdbcTemplate.query(
                "SELECT id, setting_key, setting_name, setting_value, description, system_builtin, create_time, update_time "
                        + "FROM sys_system_setting WHERE id = ?",
                rowMapper,
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public SystemSettingDO selectByKey(String key) {
        List<SystemSettingDO> rows = jdbcTemplate.query(
                "SELECT id, setting_key, setting_name, setting_value, description, system_builtin, create_time, update_time "
                        + "FROM sys_system_setting WHERE setting_key = ?",
                rowMapper,
                key
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<SystemSettingDO> selectPage(Map<String, String> params, List<String> searchableFields, int pageNum, int pageSize) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        return jdbcTemplate.query(
                "SELECT id, setting_key, setting_name, setting_value, description, system_builtin, create_time, update_time "
                        + "FROM sys_system_setting" + filter.whereClause() + " ORDER BY id DESC LIMIT ? OFFSET ?",
                rowMapper,
                filter.withPage(pageNum, pageSize)
        );
    }

    public long count(Map<String, String> params, List<String> searchableFields) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_system_setting" + filter.whereClause(),
                Long.class,
                filter.argsArray()
        );
        return total == null ? 0L : total;
    }

    public int insert(SystemSettingDO entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_system_setting(setting_key, setting_name, setting_value, description, system_builtin) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, entity.getSettingKey());
            ps.setString(2, entity.getSettingName());
            ps.setString(3, entity.getSettingValue());
            ps.setString(4, entity.getDescription());
            ps.setBoolean(5, entity.isSystemBuiltin());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
        return updated;
    }

    public int update(SystemSettingDO entity) {
        return jdbcTemplate.update(
                "UPDATE sys_system_setting SET setting_key = ?, setting_name = ?, setting_value = ?, description = ?, "
                        + "update_time = CURRENT_TIMESTAMP WHERE id = ?",
                entity.getSettingKey(),
                entity.getSettingName(),
                entity.getSettingValue(),
                entity.getDescription(),
                entity.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM sys_system_setting WHERE id = ?", id);
    }

    private static Map<String, String> columns() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "id");
        columns.put("name", "setting_name");
        columns.put("key", "setting_key");
        columns.put("value", "setting_value");
        columns.put("description", "description");
        columns.put("systemBuiltin", "system_builtin");
        columns.put("createTime", "create_time");
        columns.put("updateTime", "update_time");
        return columns;
    }
}
