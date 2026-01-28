/**
 * 本地存储工具
 * @zhoolg/svelte-admin-framework
 */
export declare const TokenStorage: {
    get: () => string | null;
    set: (token: string) => void;
    remove: () => void;
};
export declare const UserStorage: {
    get: <T = unknown>() => T | null;
    set: <T>(user: T) => void;
    remove: () => void;
};
export declare const clearAuth: () => void;
//# sourceMappingURL=storage.d.ts.map