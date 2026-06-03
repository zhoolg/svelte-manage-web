package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.DynamicModuleVersionDO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DynamicModuleVersionMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<DynamicModuleVersionDO> rowMapper = (rs, rowNum) -> {
        DynamicModuleVersionDO entity = new DynamicModuleVersionDO();
        entity.setId(rs.getLong("id"));
        entity.setModuleKey(rs.getString("module_key"));
        entity.setModuleName(rs.getString("module_name"));
        entity.setVersionNo(rs.getInt("version_no"));
        entity.setTaskNo(rs.getString("task_no"));
        entity.setSchemaHash(rs.getString("schema_hash"));
        entity.setMetadataJson(rs.getString("metadata_json"));
        entity.setSchemaJson(rs.getString("schema_json"));
        entity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return entity;
    };

    public DynamicModuleVersionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int nextVersionNo(String moduleKey) {
        Integer current = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version_no), 0) FROM sys_dynamic_module_version WHERE module_key = ?",
                Integer.class,
                moduleKey
        );
        return (current == null ? 0 : current) + 1;
    }

    public void insert(DynamicModuleVersionDO entity) {
        jdbcTemplate.update(
                "INSERT INTO sys_dynamic_module_version(module_key, module_name, version_no, task_no, "
                        + "schema_hash, metadata_json, schema_json) VALUES (?, ?, ?, ?, ?, ?, ?)",
                entity.getModuleKey(),
                entity.getModuleName(),
                entity.getVersionNo(),
                entity.getTaskNo(),
                entity.getSchemaHash(),
                entity.getMetadataJson(),
                entity.getSchemaJson()
        );
    }

    public List<DynamicModuleVersionDO> selectByModuleKey(String moduleKey) {
        return jdbcTemplate.query(
                "SELECT id, module_key, module_name, version_no, task_no, schema_hash, metadata_json, "
                        + "schema_json, create_time FROM sys_dynamic_module_version "
                        + "WHERE module_key = ? ORDER BY version_no DESC",
                rowMapper,
                moduleKey
        );
    }

    public DynamicModuleVersionDO selectByModuleKeyAndVersionNo(String moduleKey, int versionNo) {
        List<DynamicModuleVersionDO> rows = jdbcTemplate.query(
                "SELECT id, module_key, module_name, version_no, task_no, schema_hash, metadata_json, "
                        + "schema_json, create_time FROM sys_dynamic_module_version "
                        + "WHERE module_key = ? AND version_no = ?",
                rowMapper,
                moduleKey,
                versionNo
        );
        return rows.isEmpty() ? null : rows.get(0);
    }
}
