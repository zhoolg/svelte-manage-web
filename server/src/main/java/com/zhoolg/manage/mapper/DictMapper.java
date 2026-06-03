package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.DictDO;
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
public class DictMapper {
    private static final Map<String, String> QUERY_COLUMNS = columns();

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<DictDO> rowMapper = (rs, rowNum) -> {
        DictDO entity = new DictDO();
        entity.setId(rs.getLong("id"));
        entity.setName(rs.getString("name"));
        entity.setCode(rs.getString("code"));
        entity.setValue(rs.getString("value"));
        entity.setStatus(rs.getInt("status"));
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

    public DictMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DictDO> selectAll() {
        return jdbcTemplate.query(
                "SELECT id, name, code, value, status, create_time, update_time FROM sys_dict ORDER BY id DESC",
                rowMapper
        );
    }

    public DictDO selectById(Long id) {
        List<DictDO> rows = jdbcTemplate.query(
                "SELECT id, name, code, value, status, create_time, update_time FROM sys_dict WHERE id = ?",
                rowMapper,
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<DictDO> selectPage(Map<String, String> params, List<String> searchableFields, int pageNum, int pageSize) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        return jdbcTemplate.query(
                "SELECT id, name, code, value, status, create_time, update_time FROM sys_dict"
                        + filter.whereClause() + " ORDER BY id DESC LIMIT ? OFFSET ?",
                rowMapper,
                filter.withPage(pageNum, pageSize)
        );
    }

    public long count(Map<String, String> params, List<String> searchableFields) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_dict" + filter.whereClause(),
                Long.class,
                filter.argsArray()
        );
        return total == null ? 0L : total;
    }

    public int insert(DictDO entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_dict(name, code, value, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, entity.getName());
            ps.setString(2, entity.getCode());
            ps.setString(3, entity.getValue());
            ps.setInt(4, entity.getStatus() == null ? 1 : entity.getStatus());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
        return updated;
    }

    public int update(DictDO entity) {
        return jdbcTemplate.update(
                "UPDATE sys_dict SET name = ?, code = ?, value = ?, status = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?",
                entity.getName(),
                entity.getCode(),
                entity.getValue(),
                entity.getStatus() == null ? 1 : entity.getStatus(),
                entity.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM sys_dict WHERE id = ?", id);
    }

    private static Map<String, String> columns() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "id");
        columns.put("name", "name");
        columns.put("code", "code");
        columns.put("value", "value");
        columns.put("status", "status");
        columns.put("createTime", "create_time");
        columns.put("updateTime", "update_time");
        return columns;
    }
}
