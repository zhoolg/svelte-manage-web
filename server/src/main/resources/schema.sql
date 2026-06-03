DROP TABLE IF EXISTS `sys_resource_data`;

CREATE TABLE IF NOT EXISTS `sys_admin_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `name` VARCHAR(128) NOT NULL,
    `avatar` VARCHAR(512) NULL,
    `role_code` VARCHAR(32) NOT NULL DEFAULT 'viewer',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_user_username` (`username`),
    KEY `idx_admin_user_role` (`role_code`),
    KEY `idx_admin_user_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_ai_user_provider_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `provider` VARCHAR(32) NOT NULL DEFAULT 'template',
    `model` VARCHAR(128) NULL,
    `base_url` VARCHAR(255) NULL,
    `api_key_ciphertext` LONGTEXT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_provider_config_user` (`user_id`),
    KEY `idx_ai_provider_config_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_code` VARCHAR(64) NOT NULL,
    `role_name` VARCHAR(128) NOT NULL,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `system_builtin` TINYINT(1) NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_role_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `role_code` VARCHAR(64) NOT NULL,
    `permission` VARCHAR(128) NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_code`, `permission`),
    KEY `idx_role_permission_role` (`role_code`),
    KEY `idx_role_permission_value` (`permission`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `permission_code` VARCHAR(128) NOT NULL,
    `module_key` VARCHAR(64) NOT NULL,
    `module_name` VARCHAR(128) NOT NULL,
    `action_code` VARCHAR(64) NOT NULL,
    `action_name` VARCHAR(64) NOT NULL,
    `source` VARCHAR(32) NOT NULL DEFAULT 'system',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_permission_module` (`module_key`),
    KEY `idx_permission_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `applicant` VARCHAR(128) NOT NULL,
    `phone` VARCHAR(32) NOT NULL,
    `property` VARCHAR(255) NOT NULL,
    `move_in_date` VARCHAR(32) NULL,
    `lease_period` VARCHAR(64) NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'pending',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_application_phone` (`phone`),
    KEY `idx_application_status` (`status`),
    KEY `idx_application_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_faq` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question` VARCHAR(512) NOT NULL,
    `answer` TEXT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_faq_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_dict` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(128) NOT NULL,
    `code` VARCHAR(128) NOT NULL,
    `value` VARCHAR(512) NOT NULL,
    `status` TINYINT(1) NOT NULL DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_code` (`code`),
    KEY `idx_dict_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_system_setting` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `setting_key` VARCHAR(128) NOT NULL,
    `setting_name` VARCHAR(128) NOT NULL,
    `setting_value` LONGTEXT NOT NULL,
    `description` VARCHAR(512) NULL,
    `system_builtin` TINYINT(1) NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_setting_key` (`setting_key`),
    KEY `idx_system_setting_builtin` (`system_builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NULL,
    `username` VARCHAR(64) NOT NULL,
    `module` VARCHAR(64) NOT NULL,
    `action` VARCHAR(64) NOT NULL,
    `target_type` VARCHAR(64) NULL,
    `target_id` VARCHAR(128) NULL,
    `result` VARCHAR(16) NOT NULL,
    `description` VARCHAR(512) NOT NULL,
    `ip` VARCHAR(64) NULL,
    `user_agent` VARCHAR(512) NULL,
    `details_json` LONGTEXT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_audit_user` (`username`),
    KEY `idx_audit_module_action` (`module`, `action`),
    KEY `idx_audit_target` (`target_type`, `target_id`),
    KEY `idx_audit_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_generation_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_no` VARCHAR(64) NOT NULL,
    `prompt` LONGTEXT NOT NULL,
    `module_key` VARCHAR(64) NOT NULL,
    `module_name` VARCHAR(128) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `generated_schema` LONGTEXT NOT NULL,
    `generated_files` LONGTEXT NOT NULL,
    `smoke_test_status` VARCHAR(32) NULL,
    `smoke_test_result` LONGTEXT NULL,
    `smoke_test_time` DATETIME NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_generation_task_no` (`task_no`),
    KEY `idx_ai_task_module` (`module_key`),
    KEY `idx_ai_task_status` (`status`),
    KEY `idx_ai_task_smoke_status` (`smoke_test_status`),
    KEY `idx_ai_task_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_dynamic_module` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `module_key` VARCHAR(64) NOT NULL,
    `module_name` VARCHAR(128) NOT NULL,
    `task_no` VARCHAR(64) NOT NULL,
    `current_version_no` INT NOT NULL DEFAULT 1,
    `schema_hash` VARCHAR(64) NOT NULL DEFAULT '',
    `metadata_json` LONGTEXT NOT NULL,
    `enabled` TINYINT(1) DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dynamic_module_key` (`module_key`),
    KEY `idx_dynamic_module_enabled` (`enabled`),
    KEY `idx_dynamic_module_task` (`task_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_dynamic_module_version` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `module_key` VARCHAR(64) NOT NULL,
    `module_name` VARCHAR(128) NOT NULL,
    `version_no` INT NOT NULL,
    `task_no` VARCHAR(64) NOT NULL,
    `schema_hash` VARCHAR(64) NOT NULL,
    `metadata_json` LONGTEXT NOT NULL,
    `schema_json` LONGTEXT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dynamic_module_version` (`module_key`, `version_no`),
    KEY `idx_dynamic_module_version_task` (`task_no`),
    KEY `idx_dynamic_module_version_hash` (`schema_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_ai_schema_migration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `migration_key` VARCHAR(191) NOT NULL,
    `module_key` VARCHAR(64) NOT NULL,
    `table_name` VARCHAR(128) NOT NULL,
    `column_name` VARCHAR(128) NULL,
    `operation_type` VARCHAR(32) NOT NULL,
    `ddl_sql` LONGTEXT NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `error_message` VARCHAR(1000) NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_schema_migration_key` (`migration_key`),
    KEY `idx_ai_schema_migration_module` (`module_key`),
    KEY `idx_ai_schema_migration_status` (`status`),
    KEY `idx_ai_schema_migration_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `menu_key` VARCHAR(128) NOT NULL,
    `parent_key` VARCHAR(128) NULL,
    `label` VARCHAR(128) NOT NULL,
    `icon` VARCHAR(64) NOT NULL DEFAULT 'circle',
    `path` VARCHAR(255) NULL,
    `module_id` VARCHAR(128) NULL,
    `permission_code` VARCHAR(255) NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `hidden` TINYINT(1) NOT NULL DEFAULT 0,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `system_builtin` TINYINT(1) NOT NULL DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_key` (`menu_key`),
    KEY `idx_menu_parent` (`parent_key`),
    KEY `idx_menu_module` (`module_id`),
    KEY `idx_menu_enabled_hidden` (`enabled`, `hidden`),
    KEY `idx_menu_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
