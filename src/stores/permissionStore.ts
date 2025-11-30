/**
 * 权限管理状态
 * ============================================================
 *
 * 使用说明：
 * 1. 在登录成功后调用 setPermissions 设置用户权限
 * 2. 使用 hasPermission 检查单个权限
 * 3. 使用 hasAnyPermission 检查是否有任一权限
 * 4. 使用 hasAllPermissions 检查是否有所有权限
 *
 * 权限格式：
 * - 模块:操作，如 'user:add', 'user:edit', 'user:delete'
 * - 支持通配符，如 'user:*' 表示用户模块所有权限
 * - '*' 表示超级管理员，拥有所有权限
 */

import { writable, derived, get } from 'svelte/store';

// 角色类型
export interface Role {
  id: string;
  name: string;
  code: string;
  permissions: string[];
}

// 权限状态
interface PermissionState {
  // 用户角色列表
  roles: Role[];
  // 用户权限列表
  permissions: string[];
  // 是否为超级管理员
  isAdmin: boolean;
}

// 初始状态
const initialState: PermissionState = {
  roles: [],
  permissions: [],
  isAdmin: false,
};

// 从 localStorage 读取
function loadFromStorage(): PermissionState {
  if (typeof window === 'undefined') return initialState;

  const saved = localStorage.getItem('permission-storage');
  if (saved) {
    try {
      return JSON.parse(saved);
    } catch {
      return initialState;
    }
  }
  return initialState;
}

// 保存到 localStorage
function saveToStorage(state: PermissionState) {
  if (typeof window !== 'undefined') {
    localStorage.setItem('permission-storage', JSON.stringify(state));
  }
}

// 创建 permission store
function createPermissionStore() {
  const { subscribe, set, update } = writable<PermissionState>(loadFromStorage());

  return {
    subscribe,

    // 设置角色
    setRoles: (roles: Role[]) => {
      update((state) => {
        const newState = { ...state, roles };
        saveToStorage(newState);
        return newState;
      });
    },

    // 设置权限
    setPermissions: (permissions: string[]) => {
      update((state) => {
        const isAdmin = permissions.includes('*');
        const newState = { ...state, permissions, isAdmin };
        saveToStorage(newState);
        return newState;
      });
    },

    // 设置管理员状态
    setIsAdmin: (isAdmin: boolean) => {
      update((state) => {
        const newState = { ...state, isAdmin };
        saveToStorage(newState);
        return newState;
      });
    },

    // 清除权限
    clearPermissions: () => {
      const newState = { roles: [], permissions: [], isAdmin: false };
      set(newState);
      saveToStorage(newState);
    },

    // 检查单个权限
    hasPermission: (permission: string): boolean => {
      const state = get(permissionStore);

      // 超级管理员拥有所有权限
      if (state.isAdmin) return true;

      // 精确匹配
      if (state.permissions.includes(permission)) return true;

      // 通配符匹配：user:add 匹配 user:*
      const [module] = permission.split(':');
      if (state.permissions.includes(`${module}:*`)) return true;

      return false;
    },

    // 检查是否有任一权限
    hasAnyPermission: (permissionList: string[]): boolean => {
      const state = get(permissionStore);
      if (state.isAdmin) return true;
      return permissionList.some((p) => permissionStore.hasPermission(p));
    },

    // 检查是否有所有权限
    hasAllPermissions: (permissionList: string[]): boolean => {
      const state = get(permissionStore);
      if (state.isAdmin) return true;
      return permissionList.every((p) => permissionStore.hasPermission(p));
    },

    // 检查是否有某个角色
    hasRole: (roleCode: string): boolean => {
      const state = get(permissionStore);
      if (state.isAdmin) return true;
      return state.roles.some((r) => r.code === roleCode);
    },
  };
}

export const permissionStore = createPermissionStore();

// 派生 stores
export const permissions = derived(permissionStore, ($store) => $store.permissions);
export const roles = derived(permissionStore, ($store) => $store.roles);
export const isAdmin = derived(permissionStore, ($store) => $store.isAdmin);

// 创建响应式权限检查函数
export const hasPermission = derived(permissionStore, ($store) => {
  return (permission: string): boolean => {
    // 超级管理员拥有所有权限
    if ($store.isAdmin) return true;

    // 精确匹配
    if ($store.permissions.includes(permission)) return true;

    // 通配符匹配：user:add 匹配 user:*
    const [module] = permission.split(':');
    if ($store.permissions.includes(`${module}:*`)) return true;

    return false;
  };
});

// ============================================================
// 权限 Hook（便捷函数）
// ============================================================

/**
 * 获取权限检查函数
 */
export function usePermission() {
  return {
    permissions: get(permissionStore).permissions,
    roles: get(permissionStore).roles,
    isAdmin: get(permissionStore).isAdmin,
    hasPermission: permissionStore.hasPermission,
    hasAnyPermission: permissionStore.hasAnyPermission,
    hasAllPermissions: permissionStore.hasAllPermissions,
    hasRole: permissionStore.hasRole,
  };
}

// ============================================================
// 预定义权限常量
// ============================================================

export const PERMISSIONS = {
  // 用户管理
  USER: {
    VIEW: 'user:view',
    ADD: 'user:add',
    EDIT: 'user:edit',
    DELETE: 'user:delete',
    ALL: 'user:*',
  },
  // 代理商管理
  AGENT: {
    VIEW: 'agent:view',
    APPROVE: 'agent:approve',
    REJECT: 'agent:reject',
    ALL: 'agent:*',
  },
  // 问答管理
  FAQ: {
    VIEW: 'faq:view',
    ADD: 'faq:add',
    EDIT: 'faq:edit',
    DELETE: 'faq:delete',
    ALL: 'faq:*',
  },
  // 系统管理
  SYSTEM: {
    LOGS: 'system:logs',
    DICT: 'system:dict',
    SETTINGS: 'system:settings',
    ALL: 'system:*',
  },
} as const;

// ============================================================
// 角色常量
// ============================================================

export const ROLES = {
  ADMIN: 'admin',
  MANAGER: 'manager',
  OPERATOR: 'operator',
  VIEWER: 'viewer',
} as const;

export type Permission = typeof PERMISSIONS;
