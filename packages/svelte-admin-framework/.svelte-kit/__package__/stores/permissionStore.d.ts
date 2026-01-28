/**
 * 权限管理状态
 * @zhoolg/svelte-admin-framework
 */
export interface Role {
    id: string;
    name: string;
    code: string;
    permissions: string[];
}
interface PermissionState {
    roles: Role[];
    permissions: string[];
    isAdmin: boolean;
}
export declare const permissionStore: {
    subscribe: (this: void, run: import("svelte/store").Subscriber<PermissionState>, invalidate?: () => void) => import("svelte/store").Unsubscriber;
    setRoles: (roles: Role[]) => void;
    setPermissions: (permissions: string[]) => void;
    setIsAdmin: (isAdmin: boolean) => void;
    clearPermissions: () => void;
    hasPermission: (permission: string) => boolean;
    hasAnyPermission: (permissionList: string[]) => boolean;
    hasAllPermissions: (permissionList: string[]) => boolean;
    hasRole: (roleCode: string) => boolean;
};
export declare const permissions: import("svelte/store").Readable<string[]>;
export declare const roles: import("svelte/store").Readable<Role[]>;
export declare const isAdmin: import("svelte/store").Readable<boolean>;
export declare const hasPermission: import("svelte/store").Readable<(permission: string) => boolean>;
export declare function usePermission(): {
    permissions: string[];
    roles: Role[];
    isAdmin: boolean;
    hasPermission: (permission: string) => boolean;
    hasAnyPermission: (permissionList: string[]) => boolean;
    hasAllPermissions: (permissionList: string[]) => boolean;
    hasRole: (roleCode: string) => boolean;
};
export {};
//# sourceMappingURL=permissionStore.d.ts.map