/**
 *  超级懒人权限配置 - 一个文件搞定所有权限
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

  // ==================== 房源管理 ====================
  PROPERTY: {
    VIEW: 'property:view',
    ADD: 'property:add',
    EDIT: 'property:edit',
    DELETE: 'property:delete',
    EXPORT: 'property:export',
    BATCH: 'property:batch',
    ALL: 'property:*',
  },

  // ==================== 租客管理 ====================
  TENANT: {
    VIEW: 'tenant:view',
    ADD: 'tenant:add',
    EDIT: 'tenant:edit',
    DELETE: 'tenant:delete',
    EXPORT: 'tenant:export',
    ALL: 'tenant:*',
  },

  // ==================== 租房申请 ====================
  APPLICATION: {
    VIEW: 'application:view',
    APPROVE: 'application:approve',
    REJECT: 'application:reject',
    ALL: 'application:*',
  },

  // ==================== 合同管理 ====================
  CONTRACT: {
    VIEW: 'contract:view',
    ADD: 'contract:add',
    EDIT: 'contract:edit',
    RENEW: 'contract:renew',
    TERMINATE: 'contract:terminate',
    EXPORT: 'contract:export',
    ALL: 'contract:*',
  },

  // ==================== 财务管理 ====================
  FINANCE: {
    VIEW: 'finance:view',
    EXPORT: 'finance:export',
    ALL: 'finance:*',
  },

  // ==================== 分销管理 ====================
  DISTRIBUTION: {
    VIEW: 'distribution:view',
    ADD: 'distribution:add',
    EDIT: 'distribution:edit',
    DELETE: 'distribution:delete',
    ALL: 'distribution:*',
  },

  // ==================== 客服中心 ====================
  SERVICE: {
    VIEW: 'service:view',
    REPLY: 'service:reply',
    ALL: 'service:*',
  },

  // ==================== 管理员管理 ====================
  ADMIN: {
    VIEW: 'admin:view',
    ADD: 'admin:add',
    EDIT: 'admin:edit',
    DELETE: 'admin:delete',
    RESET_PASSWORD: 'admin:reset_password',
    ALL: 'admin:*',
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
    description: '拥有大部分业务管理权限',
    level: 2,
  },

  // 房源管理员
  PROPERTY_MANAGER: {
    code: 'property_manager',
    name: '房源管理员',
    description: '负责房源、租客、合同管理',
    level: 3,
  },

  // 财务人员
  FINANCE_STAFF: {
    code: 'finance_staff',
    name: '财务人员',
    description: '负责租金、押金、退款等财务管理',
    level: 3,
  },

  // 客服人员
  CUSTOMER_SERVICE: {
    code: 'customer_service',
    name: '客服人员',
    description: '负责租客咨询消息和常见问题管理',
    level: 4,
  },

  // 运营人员
  OPERATOR: {
    code: 'operator',
    name: '运营人员',
    description: '负责分销推广和日常运营',
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

  // 管理员：除系统设置外的所有业务权限
  [ALL_ROLES.ADMIN.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.PROPERTY.ALL,
    ALL_PERMISSIONS.TENANT.ALL,
    ALL_PERMISSIONS.APPLICATION.ALL,
    ALL_PERMISSIONS.CONTRACT.ALL,
    ALL_PERMISSIONS.FINANCE.ALL,
    ALL_PERMISSIONS.DISTRIBUTION.ALL,
    ALL_PERMISSIONS.SERVICE.ALL,
    ALL_PERMISSIONS.ADMIN.ALL,
    ALL_PERMISSIONS.FAQ.ALL,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.LOG.EXPORT,
    ALL_PERMISSIONS.DICT.ALL,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 房源管理员：房源、租客、合同、申请
  [ALL_ROLES.PROPERTY_MANAGER.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.PROPERTY.ALL,
    ALL_PERMISSIONS.TENANT.ALL,
    ALL_PERMISSIONS.APPLICATION.ALL,
    ALL_PERMISSIONS.CONTRACT.ALL,
    ALL_PERMISSIONS.SERVICE.VIEW,
    ALL_PERMISSIONS.SERVICE.REPLY,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 财务人员：财务管理
  [ALL_ROLES.FINANCE_STAFF.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.FINANCE.ALL,
    ALL_PERMISSIONS.CONTRACT.VIEW,
    ALL_PERMISSIONS.TENANT.VIEW,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 客服人员：客服和FAQ
  [ALL_ROLES.CUSTOMER_SERVICE.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.SERVICE.ALL,
    ALL_PERMISSIONS.FAQ.ALL,
    ALL_PERMISSIONS.PROPERTY.VIEW,
    ALL_PERMISSIONS.TENANT.VIEW,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 运营人员：分销推广
  [ALL_ROLES.OPERATOR.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.DISTRIBUTION.ALL,
    ALL_PERMISSIONS.PROPERTY.VIEW,
    ALL_PERMISSIONS.TENANT.VIEW,
    ALL_PERMISSIONS.TENANT.EXPORT,
    ALL_PERMISSIONS.FAQ.VIEW,
    ALL_PERMISSIONS.LOG.VIEW,
    ALL_PERMISSIONS.PROFILE.ALL,
  ],

  // 只读用户：只能查看
  [ALL_ROLES.VIEWER.code]: [
    ALL_PERMISSIONS.DASHBOARD.VIEW,
    ALL_PERMISSIONS.PROPERTY.VIEW,
    ALL_PERMISSIONS.TENANT.VIEW,
    ALL_PERMISSIONS.CONTRACT.VIEW,
    ALL_PERMISSIONS.FINANCE.VIEW,
    ALL_PERMISSIONS.FAQ.VIEW,
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
    description: '拥有大部分业务管理权限',
  },
  {
    username: 'property',
    password: 'property123',
    name: '房源管理员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=property',
    roles: [ALL_ROLES.PROPERTY_MANAGER.code],
    description: '负责房源、租客、合同管理',
  },
  {
    username: 'finance',
    password: 'finance123',
    name: '财务人员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=finance',
    roles: [ALL_ROLES.FINANCE_STAFF.code],
    description: '负责租金、押金、退款管理',
  },
  {
    username: 'service',
    password: 'service123',
    name: '客服人员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=service',
    roles: [ALL_ROLES.CUSTOMER_SERVICE.code],
    description: '负责租客咨询和FAQ管理',
  },
  {
    username: 'operator',
    password: 'operator123',
    name: '运营专员',
    avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=operator',
    roles: [ALL_ROLES.OPERATOR.code],
    description: '负责分销推广和日常运营',
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

  roles.forEach(role => {
    const rolePermissions = ROLE_PERMISSIONS_MAP[role] || [];
    rolePermissions.forEach(permission => permissions.add(permission));
  });

  return Array.from(permissions);
}

/**
 * 根据用户名获取用户信息和权限
 * @param username 用户名
 * @returns 用户信息和权限
 */
export function getUserPermissions(username: string) {
  const user = MOCK_USERS.find(u => u.username === username);

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
  const user = MOCK_USERS.find(u => u.username === username && u.password === password);

  if (!user) {
    return null;
  }

  return getUserPermissions(username);
}

/**
 * 获取所有用户列表（用于显示测试账号）
 */
export function getAllMockUsers() {
  return MOCK_USERS.map(user => ({
    username: user.username,
    password: user.password,
    name: user.name,
    description: user.description,
    roles: user.roles.map(code => {
      const role = Object.values(ALL_ROLES).find(r => r.code === code);
      return role?.name || code;
    }),
  }));
}

/**
 * 获取扁平化的所有权限列表
 */
export function getAllPermissionsList(): string[] {
  const permissions: string[] = [];

  Object.values(ALL_PERMISSIONS).forEach(module => {
    Object.values(module).forEach(permission => {
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
