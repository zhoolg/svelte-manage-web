package com.zhoolg.manage.infrastructure.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Passkey 凭据表迁移器，保证老数据库启动后也具备 WebAuthn 凭据存储能力。
 */
@Component
public class PasskeySchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public PasskeySchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS `sys_passkey_credential` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                    `user_id` BIGINT NOT NULL,
                    `username` VARCHAR(64) NOT NULL,
                    `credential_id` VARCHAR(512) NOT NULL,
                    `public_key_cose` LONGTEXT NOT NULL,
                    `signature_count` BIGINT NOT NULL DEFAULT 0,
                    `display_name` VARCHAR(128) NULL,
                    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
                    `last_used_time` DATETIME NULL,
                    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY `uk_passkey_credential_id` (`credential_id`),
                    KEY `idx_passkey_user` (`user_id`),
                    KEY `idx_passkey_username` (`username`),
                    KEY `idx_passkey_enabled` (`enabled`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        // 插入 Passkey 相关配置
        jdbcTemplate.execute("""
                INSERT IGNORE INTO `sys_system_setting` (`setting_key`, `setting_name`, `setting_value`, `description`, `system_builtin`) VALUES
                ('auth.passkey-rp-id', 'Passkey 域名 (RP ID)', 'localhost', 'Passkey 验证域名，通常与访问域名一致', 1),
                ('auth.passkey-rp-name', 'Passkey 服务名称', 'Svelte Manage Web', 'Passkey 弹窗显示的名称，通常为系统名', 1)
                """);
    }
}
