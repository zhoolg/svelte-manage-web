/**
 * 🚀 超级懒人权限配置 - 一个文件搞定所有权限
 * ============================================================
 *
 * 在这个文件中配置：
 * ✅ 所有权限定义
 * ✅ 所有角色定义
 * ✅ 角色权限映射
 * ✅ 模拟用户数据
 *
 * 使用方式：
 * import { PERMISSION_CONFIG, getUserPermissions } from './config/permissions';
 */

// ==================== 权限定义 ====================

/**
 * 所有权限列表
 * 格式：模块:操作
 */
export const ALL_PERMISSIONS = {
  // ==================== 首页 ====================
  DASHBOARD: {
    VIEW: 'dashboard:view',
  },

  // ==================== 用户管理 ====================
  USER: {
    VIEW: 'user:view',
    ADD: 'user:add',
    EDIT: 'user:edit',
    DELETE: 'user:delete',
    EXPORT: 'user:export',
    IMPORT: 'user:import',
    RESET_PASSWORD: 'user:reset_password',
    ALL: 'user:*',
  },

  // ==================== 代理商管理 ====================
  AGENT: {
    VIEW: 'agent:view',
    ADD: 'agent:add',
    EDIT: 'agent:edit',
    DELETE: 'agent:delete',
    APPROVE: 'agent:approve',
    REJECT: 'agent:reject',
    EXPORT: 'agent:export',
    ALL: 'agent:*',
  },

  // ==================== 问答管理 ====================
  FAQ: {
    VIEW: 'faq:view',
    ADD: 'faq:add',
    EDIT: 'faq:edit',
    DELETE: 'faq:delete',
    EXPORT: 'faq:export',
    SORT: 'faq:sort',
    ALL: 'faq:*',
  },

  // ==================== 文章管理 ====================
  ARTICLE: {
    VIEW: 'article:view',
    ADD: 'article:add',
    EDIT: 'article:edit',
    DELETE: 'article:delete',
    PUBLISH: 'article:publish',
    UNPUBLISH: 'article:unpublish',
    EXPORT: 'article:export',
    ALL: 'article:*',
  },

  // ==================== 日志管理 ====================
  LOG: {
    VIEW: 'log:view',
    EXPORT: 'log:export',
    DELETE: 'log:delete',
    ALL: 'log:*',
  },

  // ==================== 字典管理 ====================
  DICT: {
    VIEW: 'dict:view',
    ADD: 'dict:add',
    EDIT: 'dict:edit',
    DELETE: 'dict:delete',
    EXPORT: 'dict:export',
    ALL: 'dict:*',
  },

  // ==================== 系统设置 ====================
  SETTINGS: {
    VIEW: 'settings:view',
    EDIT: 'settings:edit',
    ALL: 'settings:*',
  },

  // ==================== 个人中心 ====================
  PROFILE: {
    VIEW: 'profile:view',
    EDIT: 'profile:edit',
    CHANGE_PASSWORD: 'profile:change_password',
    ALL: 'profile:*',
  },

  // ==================== 超级权限 ====================
  SUPER: {
    ALL: '*', // 所有权限
  },
} as const;

// ==================== 角色定义 ====================

/**
 * 所有角色定义
 */
export const ALL_ROLES = {
  // 超级管理员
  SUPER_ADMIN: {
    code: 'super_admin',
    name: '超级管理员',
    description: '拥有系统所有权限',
    level: 1,
  },

  // 管理员
  ADMIN: {
    code: 'admin',
    name: '管理员',
    description: '拥有大部分管理权限',
    level: 2,
  },

  // 用户管理员
  USER_MANAGER: {
    code: 'user_manager',
    name: '用户管理员',
    description: '负责用户和代理商管理',
    level: 3,
  },

  // 内容管理员
  CONTENT_MANAGER: {
    code: 'content_manager',
    name: '内容管理员',
    description: '负责内容管理（文章、FAQ等）',
    level: 3,
  },

  // 运营人员
  OPERATOR: {
    code: 'operator',
    name: '运营人员',
    description: '负责日常运营工作',
    level: 4,
  },

  // 只读用户
  VIEWER: {
    code: 'viewer',
    name: '查看者',
    description: '只能查看，不能编辑',
    level: 5,
  },
} as const;

// ==================== 角色权限映射 ====================

/**
 * 角色与权限的映射关系
 */
export const ROLE_PERMISSIONS_MAP: Record<string, string[]> = {
  // 超级管理员：所有权限
  [ALL_ROLES.SUPER_ADMIN.code]: [ALL_PERMISSIONS.SUPER.ALL],

  // 管理员：除系统设置外的所有权限
  [ALL_ROLES.ADMIN.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.USER.ALL,
    ALL_PERMISSIONS.AGENT.ALL,
    ALL_PERMISSIONS.FAQ.ALL,
    ALL_PERMISSIONS.ARTICLE.ALL,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.LOG.EXPORT,
    ALL_PERMISSIONS.DICT.ALL,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 用户管理员：用户和代理商管理
  [ALL_ROLES.USER_MANAGER.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.USER.VIEW,
    ALL_PERMISSIONS.USER.ADD,
    ALL_PERMISSIONS.USER.EDIT,
    ALL_PERMISSIONS.USER.EXPORT,
    ALL_PERMISSIONS.AGENT.VIEW,
    ALL_PERMISSIONS.AGENT.ADD,
    ALL_PERMISSIONS.AGENT.EDIT,
    ALL_PERMISSIONS.AGENT.APPROVE,
    ALL_PERMISSIONS.AGENT.REJECT,
    ALL_PERMISSIONS.AGENT.EXPORT,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 内容管理员：内容管理
  [ALL_ROLES.CONTENT_MANAGER.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.FAQ.ALL,
    ALL_PERMISSIONS.ARTICLE.ALL,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 运营人员：部分增删改权限
  [ALL_ROLES.OPERATOR.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.USER.VIEW,
    ALL_PERMISSIONS.USER.EXPORT,
    ALL_PERMISSIONS.AGENT.VIEW,
    ALL_PERMISSIONS.AGENT.EXPORT,
    ALL_PERMISSIONS.FAQ.VIEW,
    ALL_PERMISSIONS.FAQ.ADD,
    ALL_PERMISSIONS.FAQ.EDIT,
    ALL_PERMISSIONS.ARTICLE.VIEW,
    ALL_PERMISSIONS.ARTICLE.ADD,
    ALL_PERMISSIONS.ARTICLE.EDIT,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 只读用户：只能查看
  [ALL_ROLES.VIEWER.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.USER.VIEW,
    ALL_PERMISSIONS.AGENT.VIEW,
    ALL_PERMISSIONS.FAQ.VIEW,
    ALL_PERMISSIONS.ARTICLE.VIEW,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.PROFILE.VIEW,
  ],
};

// ==================== 模拟用户数据 ====================

/**
 * 模拟用户账号
 * 用于开发和测试
 */
export const MOCK_USERS = [
  {
    username: 'admin',
    password: 'admin123',
    name: '超级管理员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin',
    roles: [ALL_ROLES.SUPER_ADMIN.code],
    description: '拥有所有权限',
  },
  {
    username: 'manager',
    password: 'manager123',
    name: '系统管理员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=manager',
    roles: [ALL_ROLES.ADMIN.code],
    description: '拥有大部分管理权限',
  },
  {
    username: 'user_admin',
    password: 'user123',
    name: '用户管理员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user_admin',
    roles: [ALL_ROLES.USER_MANAGER.code],
    description: '负责用户和代理商管理',
  },
  {
    username: 'content_admin',
    password: 'content123',
    name: '内容管理员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=content_admin',
    roles: [ALL_ROLES.CONTENT_MANAGER.code],
    description: '负责内容管理',
  },
  {
    username: 'operator',
    password: 'operator123',
    name: '运营专员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=operator',
    roles: [ALL_ROLES.OPERATOR.code],
    description: '日常运营工作',
  },
  {
    username: 'viewer',
    password: 'viewer123',
    name: '访客',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=viewer',
    roles: [ALL_ROLES.VIEWER.code],
    description: '只能查看数据',
  },
];

// ==================== 工具函数 ====================

/**
 * 根据角色获取权限列表
 * @param roles 用户角色列表
 * @returns 权限列表
 */
export function getPermissionsByRoles(roles: string[]): string[] {
  const permissions = new Set<string>();

  roles.forEach((role) => {
    const rolePermissions = ROLE_PERMISSIONS_MAP[role] || [];
    rolePermissions.forEach((permission) => permissions.add(permission));
  });

  return Array.from(permissions);
}

/**
 * 根据用户名获取用户信息和权限
 * @param username 用户名
 * @returns 用户信息和权限
 */
export function getUserPermissions(username: string) {
  const user = MOCK_USERS.find((u) => u.username === username);

  if (!user) {
    return null;
  }

  const permissions = getPermissionsByRoles(user.roles);
  const isAdmin = permissions.includes(ALL_PERMISSIONS.SUPER.ALL);

  return {
    user: {
      username: user.username,
      name: user.name,
      avatar: user.avatar,
      roles: user.roles,
    },
    permissions,
    isAdmin,
  };
}

/**
 * 验证用户登录
 * @param username 用户名
 * @param password 密码
 * @returns 用户信息和权限，如果验证失败返回 null
 */
export function validateUser(username: string, password: string) {
  const user = MOCK_USERS.find((u) => u.username === username && u.password === password);

  if (!user) {
    return null;
  }

  return getUserPermissions(username);
}

/**
 * 获取所有用户列表（用于显示测试账号）
 */
export function getAllMockUsers() {
  return MOCK_USERS.map((user) => ({
    username: user.username,
    password: user.password,
    name: user.name,
    description: user.description,
    roles: user.roles.map((code) => {
      const role = Object.values(ALL_ROLES).find((r) => r.code === code);
      return role?.name || code;
    }),
  }));
}

/**
 * 获取扁平化的所有权限列表
 */
export function getAllPermissionsList(): string[] {
  const permissions: string[] = [];

  Object.values(ALL_PERMISSIONS).forEach((module) => {
    Object.values(module).forEach((permission) => {
      if (permission !== '*' && !permissions.includes(permission)) {
        permissions.push(permission);
      }
    });
  });

  return permissions;
}

/**
 * 获取所有角色列表
 */
export function getAllRolesList() {
  return Object.values(ALL_ROLES);
}

// ==================== 权限配置导出 ====================

/**
 * 权限配置对象
 * 包含所有权限相关的配置和工具函数
 */
export const PERMISSION_CONFIG = {
  // 权限常量
  PERMISSIONS: ALL_PERMISSIONS,

  // 角色常量
  ROLES: ALL_ROLES,

  // 角色权限映射
  ROLE_PERMISSIONS: ROLE_PERMISSIONS_MAP,

  // 模拟用户
  MOCK_USERS,

  // 工具函数
  getPermissionsByRoles,
  getUserPermissions,
  validateUser,
  getAllMockUsers,
  getAllPermissionsList,
  getAllRolesList,
};

// 默认导出
export default PERMISSION_CONFIG;
