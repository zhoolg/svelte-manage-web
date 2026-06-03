package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.FaqDO;
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
public class FaqMapper {
    private static final Map<String, String> QUERY_COLUMNS = columns();

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<FaqDO> rowMapper = (rs, rowNum) -> {
        FaqDO entity = new FaqDO();
        entity.setId(rs.getLong("id"));
        entity.setQuestion(rs.getString("question"));
        entity.setAnswer(rs.getString("answer"));
        entity.setSortOrder(rs.getInt("sort_order"));
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

    public FaqMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FaqDO> selectAll() {
        return jdbcTemplate.query(
                "SELECT id, question, answer, sort_order, create_time, update_time FROM sys_faq ORDER BY sort_order ASC, id DESC",
                rowMapper
        );
    }

    public FaqDO selectById(Long id) {
        List<FaqDO> rows = jdbcTemplate.query(
                "SELECT id, question, answer, sort_order, create_time, update_time FROM sys_faq WHERE id = ?",
                rowMapper,
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<FaqDO> selectPage(Map<String, String> params, List<String> searchableFields, int pageNum, int pageSize) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        return jdbcTemplate.query(
                "SELECT id, question, answer, sort_order, create_time, update_time FROM sys_faq"
                        + filter.whereClause() + " ORDER BY sort_order ASC, id DESC LIMIT ? OFFSET ?",
                rowMapper,
                filter.withPage(pageNum, pageSize)
        );
    }

    public long count(Map<String, String> params, List<String> searchableFields) {
        SqlFilterBuilder.Filter filter = SqlFilterBuilder.build(params, QUERY_COLUMNS, searchableFields);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_faq" + filter.whereClause(),
                Long.class,
                filter.argsArray()
        );
        return total == null ? 0L : total;
    }

    public int insert(FaqDO entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO sys_faq(question, answer, sort_order) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, entity.getQuestion());
            ps.setString(2, entity.getAnswer());
            ps.setInt(3, entity.getSortOrder() == null ? 0 : entity.getSortOrder());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
        return updated;
    }

    public int update(FaqDO entity) {
        return jdbcTemplate.update(
                "UPDATE sys_faq SET question = ?, answer = ?, sort_order = ?, update_time = CURRENT_TIMESTAMP WHERE id = ?",
                entity.getQuestion(),
                entity.getAnswer(),
                entity.getSortOrder() == null ? 0 : entity.getSortOrder(),
                entity.getId()
        );
    }

    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM sys_faq WHERE id = ?", id);
    }

    private static Map<String, String> columns() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "id");
        columns.put("question", "question");
        columns.put("answer", "answer");
        columns.put("sortOrder", "sort_order");
        columns.put("createTime", "create_time");
        columns.put("updateTime", "update_time");
        return columns;
    }
}
