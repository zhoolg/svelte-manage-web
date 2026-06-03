package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.AuditLogDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AuditLogMapper {
    private static final Map<String, String> QUERY_COLUMNS = columns();

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AuditLogDO> rowMapper = (rs, rowNum) -> {
        AuditLogDO entity = new AuditLogDO();
        entity.setId(rs.getLong("id"));
        entity.setUserId(rs.getObject("user_id") == null ? null : rs.getLong("user_id"));
        entity.setUsername(rs.getString("username"));
        entity.setModule(rs.getString("module"));
        entity.setAction(rs.getString("action"));
        entity.setTargetType(rs.getString("target_type"));
        entity.setTargetId(rs.getString("target_id"));
        entity.setResult(rs.getString("result"));
        entity.setDescription(rs.getString("description"));
        entity.setIp(rs.getString("ip"));
        entity.setUserAgent(rs.getString("user_agent"));
        entity.setDetailsJson(rs.getString("details_json"));
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            entity.setCreateTime(createTime.toLocalDateTime());
        }
        return entity;
    };

    public AuditLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AuditLogDO> selectAll() {
        return jdbcTemplate.query(
                "SELECT id, user_id, username, module, action, target_type, target_id, result, description, "
                        + "ip, user_agent, details_json, create_time FROM sys_audit_log ORDER BY id DESC",
                rowMapper
        );
    }

    public List<AuditLogDO> selectPage(Map<String, String> params, List<String> searchableFields, int pageNum, int pageSize) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        return jdbcTemplate.query(
                "SELECT id, user_id, username, module, action, target_type, target_id, result, description, "
                        + "ip, user_agent, details_json, create_time FROM sys_audit_log"
                        + filter.whereClause() + " ORDER BY id DESC LIMIT ? OFFSET ?",
                rowMapper,
                filter.withPage(pageNum, pageSize)
        );
    }

    public long count(Map<String, String> params, List<String> searchableFields) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log" + filter.whereClause(),
                Long.class,
                filter.argsArray()
        );
        return total == null ? 0L : total;
    }

    public long countLoginFailuresSince(LocalDateTime since) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE module = 'auth' AND action = 'login' "
                        + "AND result = 'error' AND create_time >= ?",
                Long.class,
                Timestamp.valueOf(since)
        );
        return total == null ? 0L : total;
    }

    public LocalDateTime latestAdminLoginTime() {
        List<LocalDateTime> rows = jdbcTemplate.query(
                "SELECT create_time FROM sys_audit_log WHERE module = 'auth' AND action IN ('login', 'passkey_login') "
                        + "AND result = 'success' ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> {
                    Timestamp createTime = rs.getTimestamp("create_time");
                    return createTime == null ? null : createTime.toLocalDateTime();
                }
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int insert(AuditLogDO entity) {
        return jdbcTemplate.update(
                "INSERT INTO sys_audit_log(user_id, username, module, action, target_type, target_id, "
                        + "result, description, ip, user_agent, details_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                entity.getUserId(),
                entity.getUsername(),
                entity.getModule(),
                entity.getAction(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getResult(),
                entity.getDescription(),
                entity.getIp(),
                entity.getUserAgent(),
                entity.getDetailsJson()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM sys_audit_log WHERE id = ?", id);
    }

    private static Map<String, String> columns() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "id");
        columns.put("username", "username");
        columns.put("operator", "username");
        columns.put("module", "module");
        columns.put("action", "action");
        columns.put("targetType", "target_type");
        columns.put("targetId", "target_id");
        columns.put("status", "result");
        columns.put("level", "result");
        columns.put("message", "description");
        columns.put("description", "description");
        columns.put("ip", "ip");
        columns.put("createTime", "create_time");
        return columns;
    }
}
