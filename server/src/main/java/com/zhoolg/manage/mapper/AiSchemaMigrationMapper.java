package com.zhoolg.manage.mapper;

import com.zhoolg.manage.entity.pojo.AiSchemaMigrationDO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class AiSchemaMigrationMapper {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AiSchemaMigrationDO> rowMapper = (rs, rowNum) -> {
        AiSchemaMigrationDO entity = new AiSchemaMigrationDO();
        entity.setId(rs.getLong("id"));
        entity.setMigrationKey(rs.getString("migration_key"));
        entity.setModuleKey(rs.getString("module_key"));
        entity.setTableName(rs.getString("table_name"));
        entity.setColumnName(rs.getString("column_name"));
        entity.setOperationType(rs.getString("operation_type"));
        entity.setDdlSql(rs.getString("ddl_sql"));
        entity.setStatus(rs.getString("status"));
        entity.setErrorMessage(rs.getString("error_message"));
        entity.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            entity.setUpdateTime(updateTime.toLocalDateTime());
        }
        return entity;
    };

    public AiSchemaMigrationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiSchemaMigrationDO selectByMigrationKey(String migrationKey) {
        List<AiSchemaMigrationDO> rows = jdbcTemplate.query(
                "SELECT id, migration_key, module_key, table_name, column_name, operation_type, ddl_sql, "
                        + "status, error_message, create_time, update_time FROM sys_ai_schema_migration "
                        + "WHERE migration_key = ?",
                rowMapper,
                migrationKey
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public boolean insertApplying(
            String migrationKey,
            String moduleKey,
            String tableName,
            String columnName,
            String operationType,
            String ddlSql
    ) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO sys_ai_schema_migration(migration_key, module_key, table_name, column_name, "
                            + "operation_type, ddl_sql, status) VALUES (?, ?, ?, ?, ?, ?, 'APPLYING')",
                    migrationKey,
                    moduleKey,
                    tableName,
                    columnName,
                    operationType,
                    ddlSql
            );
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    public int markApplying(String migrationKey) {
        return jdbcTemplate.update(
                "UPDATE sys_ai_schema_migration SET status = 'APPLYING', error_message = NULL, "
                        + "update_time = CURRENT_TIMESTAMP WHERE migration_key = ? AND status <> 'APPLIED'",
                migrationKey
        );
    }

    public void markApplied(String migrationKey) {
        jdbcTemplate.update(
                "UPDATE sys_ai_schema_migration SET status = 'APPLIED', error_message = NULL, "
                        + "update_time = CURRENT_TIMESTAMP WHERE migration_key = ?",
                migrationKey
        );
    }

    public void markFailed(String migrationKey, String errorMessage) {
        jdbcTemplate.update(
                "UPDATE sys_ai_schema_migration SET status = 'FAILED', error_message = ?, "
                        + "update_time = CURRENT_TIMESTAMP WHERE migration_key = ?",
                truncate(errorMessage),
                migrationKey
        );
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
