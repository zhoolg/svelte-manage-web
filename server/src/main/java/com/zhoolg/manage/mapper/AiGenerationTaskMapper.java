package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.AiGenerationTaskDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class AiGenerationTaskMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AiGenerationTaskDO> rowMapper = (rs, rowNum) -> {
        AiGenerationTaskDO entity = new AiGenerationTaskDO();
        entity.setId(rs.getLong("id"));
        entity.setTaskNo(rs.getString("task_no"));
        entity.setPrompt(rs.getString("prompt"));
        entity.setModuleKey(rs.getString("module_key"));
        entity.setModuleName(rs.getString("module_name"));
        entity.setStatus(rs.getString("status"));
        entity.setGeneratedSchema(rs.getString("generated_schema"));
        entity.setGeneratedFiles(rs.getString("generated_files"));
        entity.setSmokeTestStatus(rs.getString("smoke_test_status"));
        entity.setSmokeTestResult(rs.getString("smoke_test_result"));
        Timestamp smokeTestTime = rs.getTimestamp("smoke_test_time");
        if (smokeTestTime != null) {
            entity.setSmokeTestTime(smokeTestTime.toLocalDateTime());
        }
        entity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        if (rs.getTimestamp("update_time") != null) {
            entity.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        }
        return entity;
    };

    public AiGenerationTaskMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(AiGenerationTaskDO entity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ai_generation_task(task_no, prompt, module_key, module_name, status, "
                            + "generated_schema, generated_files) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, entity.getTaskNo());
            ps.setString(2, entity.getPrompt());
            ps.setString(3, entity.getModuleKey());
            ps.setString(4, entity.getModuleName());
            ps.setString(5, entity.getStatus());
            ps.setString(6, entity.getGeneratedSchema());
            ps.setString(7, entity.getGeneratedFiles());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            entity.setId(keyHolder.getKey().longValue());
        }
        return updated;
    }

    public AiGenerationTaskDO selectByTaskNo(String taskNo) {
        List<AiGenerationTaskDO> rows = jdbcTemplate.query(
                baseSelect() + " WHERE task_no = ?",
                rowMapper,
                taskNo
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<AiGenerationTaskDO> selectRecent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return jdbcTemplate.query(
                baseSelect() + " ORDER BY id DESC LIMIT ?",
                rowMapper,
                safeLimit
        );
    }

    public int updateStatus(String taskNo, String status) {
        return jdbcTemplate.update(
                "UPDATE ai_generation_task SET status = ?, update_time = CURRENT_TIMESTAMP WHERE task_no = ?",
                status,
                taskNo
        );
    }

    public int updateSmokeTestResult(String taskNo, String status, String resultJson) {
        return jdbcTemplate.update(
                "UPDATE ai_generation_task SET smoke_test_status = ?, smoke_test_result = ?, "
                        + "smoke_test_time = CURRENT_TIMESTAMP, update_time = CURRENT_TIMESTAMP WHERE task_no = ?",
                status,
                resultJson,
                taskNo
        );
    }

    private String baseSelect() {
        return "SELECT id, task_no, prompt, module_key, module_name, status, generated_schema, generated_files, "
                + "smoke_test_status, smoke_test_result, smoke_test_time, create_time, update_time "
                + "FROM ai_generation_task";
    }
}
