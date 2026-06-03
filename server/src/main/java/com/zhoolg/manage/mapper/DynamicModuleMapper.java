package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.DynamicModuleDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DynamicModuleMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<DynamicModuleDO> rowMapper = (rs, rowNum) -> {
        DynamicModuleDO entity = new DynamicModuleDO();
        entity.setId(rs.getLong("id"));
        entity.setModuleKey(rs.getString("module_key"));
        entity.setModuleName(rs.getString("module_name"));
        entity.setTaskNo(rs.getString("task_no"));
        entity.setCurrentVersionNo(rs.getInt("current_version_no"));
        entity.setSchemaHash(rs.getString("schema_hash"));
        entity.setMetadataJson(rs.getString("metadata_json"));
        entity.setEnabled(rs.getBoolean("enabled"));
        entity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        if (rs.getTimestamp("update_time") != null) {
            entity.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        }
        return entity;
    };

    public DynamicModuleMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DynamicModuleDO> selectEnabled() {
        return jdbcTemplate.query(
                "SELECT id, module_key, module_name, task_no, current_version_no, schema_hash, "
                        + "metadata_json, enabled, create_time, update_time "
                        + "FROM sys_dynamic_module WHERE enabled = TRUE ORDER BY id DESC",
                rowMapper
        );
    }

    public DynamicModuleDO selectByModuleKey(String moduleKey) {
        List<DynamicModuleDO> rows = jdbcTemplate.query(
                "SELECT id, module_key, module_name, task_no, current_version_no, schema_hash, "
                        + "metadata_json, enabled, create_time, update_time "
                        + "FROM sys_dynamic_module WHERE module_key = ?",
                rowMapper,
                moduleKey
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void upsert(DynamicModuleDO entity) {
        DynamicModuleDO existing = selectByModuleKey(entity.getModuleKey());
        if (existing == null) {
            jdbcTemplate.update(
                    "INSERT INTO sys_dynamic_module(module_key, module_name, task_no, current_version_no, "
                            + "schema_hash, metadata_json, enabled) VALUES (?, ?, ?, ?, ?, ?, TRUE)",
                    entity.getModuleKey(),
                    entity.getModuleName(),
                    entity.getTaskNo(),
                    entity.getCurrentVersionNo(),
                    entity.getSchemaHash(),
                    entity.getMetadataJson()
            );
            return;
        }
        jdbcTemplate.update(
                "UPDATE sys_dynamic_module SET module_name = ?, task_no = ?, current_version_no = ?, "
                        + "schema_hash = ?, metadata_json = ?, "
                        + "enabled = TRUE, update_time = CURRENT_TIMESTAMP WHERE module_key = ?",
                entity.getModuleName(),
                entity.getTaskNo(),
                entity.getCurrentVersionNo(),
                entity.getSchemaHash(),
                entity.getMetadataJson(),
                entity.getModuleKey()
        );
    }

    public int disableByModuleKeyAndTaskNo(String moduleKey, String taskNo) {
        return jdbcTemplate.update(
                "UPDATE sys_dynamic_module SET enabled = FALSE, update_time = CURRENT_TIMESTAMP "
                        + "WHERE module_key = ? AND task_no = ? AND enabled = TRUE",
                moduleKey,
                taskNo
        );
    }
}
