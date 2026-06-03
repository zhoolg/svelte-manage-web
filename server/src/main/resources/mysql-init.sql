-- svelte-manage-web 完整默认 MySQL 初始化脚本
-- 适用：全新空库初始化。会删除并重建本项目默认业务表。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_resource_data`;
DROP TABLE IF EXISTS `sys_audit_log`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_ai_schema_migration`;
DROP TABLE IF EXISTS `sys_ai_user_provider_config`;
DROP TABLE IF EXISTS `sys_passkey_credential`;
DROP TABLE IF EXISTS `sys_dynamic_module_version`;
DROP TABLE IF EXISTS `sys_dynamic_module`;
DROP TABLE IF EXISTS `ai_generation_task`;
DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_permission`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_system_setting`;
DROP TABLE IF EXISTS `sys_dict`;
DROP TABLE IF EXISTS `sys_faq`;
DROP TABLE IF EXISTS `sys_application`;
DROP TABLE IF EXISTS `sys_admin_user`;

SET FOREIGN_KEY_CHECKS = 1;

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

INSERT INTO `sys_admin_user` (`username`, `password_hash`, `name`, `avatar`, `role_code`, `enabled`) VALUES
('admin', '', '超级管理员', NULL, 'super_admin', 1),
('operator', '', '运营人员', NULL, 'operator', 1),
('viewer', '', '查看者', NULL, 'viewer', 1);

INSERT INTO `sys_role` (`role_code`, `role_name`, `enabled`, `system_builtin`) VALUES
('super_admin', '超级管理员', 1, 1),
('admin', '管理员', 1, 1),
('operator', '运营人员', 1, 1),
('viewer', '查看者', 1, 1);

INSERT INTO `sys_role_permission` (`role_code`, `permission`) VALUES
('super_admin', '*'),
('admin', '*'),
('operator', 'dashboard:view'),
('operator', 'admin:view'),
('operator', 'application:*'),
('operator', 'faq:*'),
('operator', 'dict:*'),
('operator', 'settings:view'),
('operator', 'settings:edit'),
('operator', 'menu:view'),
('operator', 'menu:add'),
('operator', 'menu:edit'),
('operator', 'menu:delete'),
('operator', 'log:view'),
('operator', 'profile:*'),
('viewer', 'dashboard:view'),
('viewer', 'admin:view'),
('viewer', 'application:view'),
('viewer', 'faq:view'),
('viewer', 'dict:view'),
('viewer', 'settings:view'),
('viewer', 'menu:view'),
('viewer', 'log:view');

INSERT INTO `sys_permission` (`permission_code`, `module_key`, `module_name`, `action_code`, `action_name`, `source`) VALUES
('dashboard:view', 'dashboard', '控制台', 'view', '查看', 'system'),
('application:view', 'application', '租房申请', 'view', '查看', 'system'),
('application:add', 'application', '租房申请', 'add', '新增', 'system'),
('application:edit', 'application', '租房申请', 'edit', '编辑', 'system'),
('application:delete', 'application', '租房申请', 'delete', '删除', 'system'),
('faq:view', 'faq', '常见问题', 'view', '查看', 'system'),
('faq:add', 'faq', '常见问题', 'add', '新增', 'system'),
('faq:edit', 'faq', '常见问题', 'edit', '编辑', 'system'),
('faq:delete', 'faq', '常见问题', 'delete', '删除', 'system'),
('admin:view', 'admin', '管理员', 'view', '查看', 'system'),
('admin:add', 'admin', '管理员', 'add', '新增', 'system'),
('admin:edit', 'admin', '管理员', 'edit', '编辑', 'system'),
('admin:delete', 'admin', '管理员', 'delete', '删除', 'system'),
('log:view', 'log', '系统日志', 'view', '查看', 'system'),
('dict:view', 'dict', '字典管理', 'view', '查看', 'system'),
('dict:add', 'dict', '字典管理', 'add', '新增', 'system'),
('dict:edit', 'dict', '字典管理', 'edit', '编辑', 'system'),
('dict:delete', 'dict', '字典管理', 'delete', '删除', 'system'),
('settings:view', 'settings', '系统设置', 'view', '查看', 'system'),
('settings:edit', 'settings', '系统设置', 'edit', '编辑', 'system'),
('menu:view', 'menu', '菜单管理', 'view', '查看', 'system'),
('menu:add', 'menu', '菜单管理', 'add', '新增', 'system'),
('menu:edit', 'menu', '菜单管理', 'edit', '编辑', 'system'),
('menu:delete', 'menu', '菜单管理', 'delete', '删除', 'system'),
('profile:view', 'profile', '个人中心', 'view', '查看', 'system');

INSERT INTO `sys_menu` (`menu_key`, `parent_key`, `label`, `icon`, `path`, `module_id`, `permission_code`, `sort_order`, `hidden`, `enabled`, `system_builtin`) VALUES
('home', NULL, 'menu.home', 'home', '/', 'home', NULL, 10, 0, 1, 1),
('tenant', NULL, 'menu.tenant', 'users', '/tenant', NULL, NULL, 20, 0, 1, 1),
('service', NULL, 'menu.service', 'message-circle', '/service', NULL, NULL, 30, 0, 1, 1),
('system', NULL, 'menu.system', 'cog', '/system', NULL, NULL, 40, 0, 1, 1),
('applications', 'tenant', 'menu.applications', 'clipboard-check', '/applications', 'applications', 'application:view', 10, 0, 1, 1),
('faq', 'service', 'menu.faq', 'question-circle', '/faq', 'faq', 'faq:view', 10, 0, 1, 1),
('admins', 'system', 'menu.admins', 'user-circle', '/admins', 'admins', 'admin:view', 10, 0, 1, 1),
('logs', 'system', 'menu.logs', 'file-text', '/logs', 'logs', 'log:view', 20, 0, 1, 1),
('dict', 'system', 'menu.dict', 'book-open', '/dict', 'dict', 'dict:view', 30, 0, 1, 1),
('settings', 'system', 'menu.settings', 'settings', '/settings', 'settings', 'settings:view', 40, 0, 1, 1),
('menus', 'system', 'menu.menus', 'list-tree', '/menus', 'menus', 'menu:view', 50, 0, 1, 1),
('system-status', 'system', 'menu.systemStatus', 'activity', '/system-status', 'system-status', 'settings:view', 60, 0, 1, 1),
('ai-modules', 'system', 'menu.aiModules', 'sparkles', '/ai-modules', 'ai-modules', 'settings:edit', 70, 0, 1, 1),
('profile', NULL, 'menu.profile', 'user', '/profile', 'profile', 'profile:view', 999, 1, 1, 1);

INSERT INTO `sys_system_setting` (`setting_key`, `setting_name`, `setting_value`, `description`, `system_builtin`) VALUES
('site.title', '站点标题', '管理平台', '前端与后台展示的默认站点标题', 1),
('upload.max-size-mb', '上传大小上限', '20', '文件上传接口允许的单文件最大 MB 数', 1),
('ai.enabled', 'AI 生成模块开关', 'true', '控制后台 AI 模块生成入口是否启用', 1),
('ai.provider', 'AI 默认提供商', 'none', '可配置为 none、openai 或 anthropic', 1),
('security.session-ttl-minutes', '会话有效期', '120', '后台登录会话默认有效分钟数', 1),
('web.wechat-login-enabled', '微信登录开关', 'false', 'Web 端微信登录能力开关', 1);

INSERT INTO `sys_dict` (`name`, `code`, `value`, `status`) VALUES
('房源状态', 'property_status', 'available=可租,rented=已租,offline=下架', 1),
('申请状态', 'application_status', 'pending=待审核,approved=已通过,rejected=已拒绝', 1),
('菜单显示状态', 'menu_enabled_status', 'true=启用,false=禁用', 1),
('通用启用状态', 'common_enabled_status', '1=启用,0=禁用', 1);

INSERT INTO `sys_application` (`applicant`, `phone`, `property`, `move_in_date`, `lease_period`, `status`) VALUES
('张三', '13900000001', '阳光花园 2 室 1 厅', '2026-06-15', '12个月', 'pending'),
('李四', '13900000002', '城市公寓 1 室', '2026-07-01', '6个月', 'approved'),
('王五', '13900000003', '滨江雅苑 3 室 2 厅', '2026-07-20', '24个月', 'rejected');

INSERT INTO `sys_faq` (`question`, `answer`, `sort_order`) VALUES
('如何提交租房申请？', '在房源详情页点击申请按钮并填写姓名、手机号、意向房源和入住时间。', 1),
('押金如何退还？', '合同到期并完成验房后，押金按合同约定原路退还。', 2),
('可以在线查看申请状态吗？', '可以。后台租房申请模块会显示待审核、已通过、已拒绝等状态。', 3);

INSERT INTO `sys_audit_log` (`username`, `module`, `action`, `result`, `description`, `ip`) VALUES
('system', 'database', 'init', 'success', '完整默认数据库初始化完成', '127.0.0.1');
