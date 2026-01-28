/**
 * 权限管理状态
 * @zhoolg/svelte-admin-framework
 */
import { writable, derived, get } from 'svelte/store';
// 初始状态
const initialState = {
    roles: [],
    permissions: [],
    isAdmin: false,
};
// 从 localStorage 读取
function loadFromStorage() {
    if (typeof window === 'undefined')
        return initialState;
    const saved = localStorage.getItem('permission-storage');
    if (saved) {
        try {
            return JSON.parse(saved);
        }
        catch {
            return initialState;
        }
    }
    return initialState;
}
// 保存到 localStorage
function saveToStorage(state) {
    if (typeof window !== 'undefined') {
        localStorage.setItem('permission-storage', JSON.stringify(state));
    }
}
// 创建 permission store
function createPermissionStore() {
    const { subscribe, set, update } = writable(loadFromStorage());
    const store = {
        subscribe,
        setRoles: (roles) => {
            update((state) => {
                const newState = { ...state, roles };
                saveToStorage(newState);
                return newState;
            });
        },
        setPermissions: (permissions) => {
            update((state) => {
                const isAdmin = permissions.includes('*');
                const newState = { ...state, permissions, isAdmin };
                saveToStorage(newState);
                return newState;
            });
        },
        setIsAdmin: (isAdmin) => {
            update((state) => {
                const newState = { ...state, isAdmin };
                saveToStorage(newState);
                return newState;
            });
        },
        clearPermissions: () => {
            const newState = { roles: [], permissions: [], isAdmin: false };
            set(newState);
            saveToStorage(newState);
        },
        hasPermission: (permission) => {
            const state = get(store);
            if (state.isAdmin)
                return true;
            if (state.permissions.includes(permission))
                return true;
            const [module] = permission.split(':');
            if (state.permissions.includes(`${module}:*`))
                return true;
            return false;
        },
        hasAnyPermission: (permissionList) => {
            const state = get(store);
            if (state.isAdmin)
                return true;
            return permissionList.some((p) => store.hasPermission(p));
        },
        hasAllPermissions: (permissionList) => {
            const state = get(store);
            if (state.isAdmin)
                return true;
            return permissionList.every((p) => store.hasPermission(p));
        },
        hasRole: (roleCode) => {
            const state = get(store);
            if (state.isAdmin)
                return true;
            return state.roles.some((r) => r.code === roleCode);
        },
    };
    return store;
}
export const permissionStore = createPermissionStore();
// 派生 stores
export const permissions = derived(permissionStore, ($store) => $store.permissions);
export const roles = derived(permissionStore, ($store) => $store.roles);
export const isAdmin = derived(permissionStore, ($store) => $store.isAdmin);
// 创建响应式权限检查函数
export const hasPermission = derived(permissionStore, ($store) => {
    return (permission) => {
        if ($store.isAdmin)
            return true;
        if ($store.permissions.includes(permission))
            return true;
        const [module] = permission.split(':');
        if ($store.permissions.includes(`${module}:*`))
            return true;
        return false;
    };
});
// 权限 Hook
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
