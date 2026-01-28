export interface UserInfo {
    id?: number;
    username?: string;
    name?: string;
    avatar?: string;
    roles?: string[];
}
interface AuthState {
    token: string | null;
    user: UserInfo | null;
}
export declare const authStore: {
    subscribe: (this: void, run: import("svelte/store").Subscriber<AuthState>, invalidate?: () => void) => import("svelte/store").Unsubscriber;
    init: () => void;
    setToken: (token: string) => void;
    setUser: (user: UserInfo) => void;
    login: (token: string, user: UserInfo) => void;
    logout: () => void;
};
export declare const isLoggedIn: import("svelte/store").Readable<boolean>;
export {};
//# sourceMappingURL=authStore.d.ts.map