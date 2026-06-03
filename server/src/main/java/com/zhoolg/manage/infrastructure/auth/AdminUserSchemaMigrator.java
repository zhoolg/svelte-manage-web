package com.zhoolg.manage.infrastructure.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 管理员账号表兼容迁移：为旧库补齐用户中心需要的头像字段。
 */
@Component
@Order(0)
public class AdminUserSchemaMigrator implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public AdminUserSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!hasColumn("sys_admin_user", "avatar")) {
            jdbcTemplate.update("ALTER TABLE `sys_admin_user` ADD COLUMN `avatar` VARCHAR(512) NULL AFTER `name`");
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
