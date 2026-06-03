package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.ApplicationDO;
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
public class ApplicationMapper {
    private static final Map<String, String> QUERY_COLUMNS = columns();

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<ApplicationDO> rowMapper = (rs, rowNum) -> {
        ApplicationDO entity = new ApplicationDO();
        entity.setId(rs.getLong("id"));
        entity.setApplicant(rs.getString("applicant"));
        entity.setPhone(rs.getString("phone"));
        entity.setProperty(rs.getString("property"));
        entity.setMoveInDate(rs.getString("move_in_date"));
        entity.setLeasePeriod(rs.getString("lease_period"));
        entity.setStatus(rs.getString("status"));
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

    public ApplicationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ApplicationDO> selectAll() {
        return jdbcTemplate.query(
                "SELECT id, applicant, phone, property, move_in_date, lease_period, status, create_time, update_time "
                        + "FROM sys_application ORDER BY id DESC",
                rowMapper
        );
    }

    public ApplicationDO selectById(Long id) {
        List<ApplicationDO> rows = jdbcTemplate.query(
                "SELECT id, applicant, phone, property, move_in_date, lease_period, status, create_time, update_time "
                        + "FROM sys_application WHERE id = ?",
                rowMapper,
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<ApplicationDO> selectPage(Map<String, String> params, List<String> searchableFields, int pageNum, int pageSize) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        return jdbcTemplate.query(
                "SELECT id, applicant, phone, property, move_in_date, lease_period, status, create_time, update_time "
                        + "FROM sys_application" + filter.whereClause() + " ORDER BY id DESC LIMIT ? OFFSET ?",
                rowMapper,
                filter.withPage(pageNum, pageSize)
        );
    }

    public long count(Map<String, String> params, List<String> searchableFields) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_application" + filter.whereClause(),
                Long.class,
                filter.argsArray()
        );
        return total == null ? 0L : total;
    }

    public int insert(ApplicationDO entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_application(applicant, phone, property, move_in_date, lease_period, status) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, entity.getApplicant());
            ps.setString(2, entity.getPhone());
            ps.setString(3, entity.getProperty());
            ps.setString(4, entity.getMoveInDate());
            ps.setString(5, entity.getLeasePeriod());
            ps.setString(6, entity.getStatus());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
        return updated;
    }

    public int update(ApplicationDO entity) {
        return jdbcTemplate.update(
                "UPDATE sys_application SET applicant = ?, phone = ?, property = ?, move_in_date = ?, "
                        + "lease_period = ?, status = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?",
                entity.getApplicant(),
                entity.getPhone(),
                entity.getProperty(),
                entity.getMoveInDate(),
                entity.getLeasePeriod(),
                entity.getStatus(),
                entity.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM sys_application WHERE id = ?", id);
    }

    private static Map<String, String> columns() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "id");
        columns.put("applicant", "applicant");
        columns.put("phone", "phone");
        columns.put("property", "property");
        columns.put("moveInDate", "move_in_date");
        columns.put("leasePeriod", "lease_period");
        columns.put("status", "status");
        columns.put("createTime", "create_time");
        columns.put("updateTime", "update_time");
        return columns;
    }
}
