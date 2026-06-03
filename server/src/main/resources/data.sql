INSERT IGNORE INTO `sys_admin_user` (`username`, `password_hash`, `name`, `role_code`, `enabled`) VALUES
('admin', '', '超级管理员', 'super_admin', 1),
('operator', '', '运营人员', 'operator', 1),
('viewer', '', '查看者', 'viewer', 1);

INSERT IGNORE INTO `sys_role` (`role_code`, `role_name`, `enabled`, `system_builtin`) VALUES
('super_admin', '超级管理员', 1, 1),
('admin', '管理员', 1, 1),
('operator', '运营人员', 1, 1),
('viewer', '查看者', 1, 1);

INSERT IGNORE INTO `sys_role_permission` (`role_code`, `permission`) VALUES
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

INSERT IGNORE INTO `sys_permission` (`permission_code`, `module_key`, `module_name`, `action_code`, `action_name`, `source`) VALUES
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

INSERT IGNORE INTO `sys_menu` (`menu_key`, `parent_key`, `label`, `icon`, `path`, `module_id`, `permission_code`, `sort_order`, `hidden`, `enabled`, `system_builtin`) VALUES
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

INSERT IGNORE INTO `sys_system_setting` (`setting_key`, `setting_name`, `setting_value`, `description`, `system_builtin`) VALUES
('site.title', '站点标题', '管理平台', '前端与后台展示的默认站点标题', 1),
('upload.max-size-mb', '上传大小上限', '20', '文件上传接口允许的单文件最大 MB 数', 1),
('ai.enabled', 'AI 生成模块开关', 'true', '控制后台 AI 模块生成入口是否启用', 1),
('ai.provider', 'AI 默认提供商', 'none', '可配置为 none、openai 或 anthropic', 1),
('security.session-ttl-minutes', '会话有效期', '120', '后台登录会话默认有效分钟数', 1),
('web.wechat-login-enabled', '微信登录开关', 'false', 'Web 端微信登录能力开关', 1);

INSERT INTO `sys_dict` (`name`, `code`, `value`, `status`)
SELECT '房源状态', 'property_status', 'available=可租,rented=已租,offline=下架', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'property_status');

INSERT INTO `sys_dict` (`name`, `code`, `value`, `status`)
SELECT '申请状态', 'application_status', 'pending=待审核,approved=已通过,rejected=已拒绝', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'application_status');

INSERT INTO `sys_dict` (`name`, `code`, `value`, `status`)
SELECT '菜单显示状态', 'menu_enabled_status', 'true=启用,false=禁用', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'menu_enabled_status');

INSERT INTO `sys_dict` (`name`, `code`, `value`, `status`)
SELECT '通用启用状态', 'common_enabled_status', '1=启用,0=禁用', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `code` = 'common_enabled_status');

INSERT INTO `sys_application` (`applicant`, `phone`, `property`, `move_in_date`, `lease_period`, `status`)
SELECT '张三', '13900000001', '阳光花园 2 室 1 厅', '2026-06-15', '12个月', 'pending'
WHERE NOT EXISTS (SELECT 1 FROM `sys_application` WHERE `phone` = '13900000001');

INSERT INTO `sys_application` (`applicant`, `phone`, `property`, `move_in_date`, `lease_period`, `status`)
SELECT '李四', '13900000002', '城市公寓 1 室', '2026-07-01', '6个月', 'approved'
WHERE NOT EXISTS (SELECT 1 FROM `sys_application` WHERE `phone` = '13900000002');

INSERT INTO `sys_faq` (`question`, `answer`, `sort_order`)
SELECT '如何提交租房申请？', '在房源详情页点击申请按钮并填写姓名、手机号、意向房源和入住时间。', 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_faq` WHERE `question` = '如何提交租房申请？');

INSERT INTO `sys_faq` (`question`, `answer`, `sort_order`)
SELECT '押金如何退还？', '合同到期并完成验房后，押金按合同约定原路退还。', 2
WHERE NOT EXISTS (SELECT 1 FROM `sys_faq` WHERE `question` = '押金如何退还？');

INSERT INTO `sys_audit_log` (`username`, `module`, `action`, `result`, `description`, `ip`)
SELECT 'system', 'database', 'init', 'success', '完整默认数据库初始化完成', '127.0.0.1'
WHERE NOT EXISTS (SELECT 1 FROM `sys_audit_log` WHERE `username` = 'system' AND `module` = 'database' AND `action` = 'init');
