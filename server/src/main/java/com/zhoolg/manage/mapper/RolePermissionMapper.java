package com.zhoolg.manage.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RolePermissionMapper {
    private final JdbcTemplate jdbcTemplate;

    public RolePermissionMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RoleRow> selectEnabledRoles() {
        return jdbcTemplate.query(
                "SELECT role_code, role_name, enabled, system_builtin FROM sys_role WHERE enabled = 1 ORDER BY id ASC",
                (rs, rowNum) -> new RoleRow(
                        rs.getString("role_code"),
                        rs.getString("role_name"),
                        rs.getBoolean("enabled"),
                        rs.getBoolean("system_builtin")
                )
        );
    }

    public List<String> selectPermissionsByRole(String roleCode) {
        return jdbcTemplate.queryForList(
                "SELECT permission FROM sys_role_permission WHERE role_code = ? ORDER BY id ASC",
                String.class,
                roleCode
        );
    }

    public void upsertRolePermission(String roleCode, String permission) {
        jdbcTemplate.update(
                "INSERT IGNORE INTO sys_role_permission(role_code, permission) VALUES (?, ?)",
                roleCode,
                permission
        );
    }

    public record RoleRow(String code, String name, boolean enabled, boolean systemBuiltin) {
    }
}
