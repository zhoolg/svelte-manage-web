/**
 * 认证状态管理 - Svelte Store
 * @zhoolg/svelte-admin-framework
 */
import { writable, derived } from 'svelte/store';
import { TokenStorage, UserStorage, clearAuth } from '../utils/storage';

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

// 创建 store
function createAuthStore() {
  const { subscribe, set, update } = writable<AuthState>({
    token: null,
    user: null,
  });

  return {
    subscribe,

    // 初始化（从 localStorage 加载）
    init: () => {
      const token = TokenStorage.get();
      const user = UserStorage.get<UserInfo>();
      set({ token, user });
    },

    // 设置 token
    setToken: (token: string) => {
      TokenStorage.set(token);
      update((state) => ({ ...state, token }));
    },

    // 设置用户信息
    setUser: (user: UserInfo) => {
      UserStorage.set(user);
      update((state) => ({ ...state, user }));
    },

    // 登录
    login: (token: string, user: UserInfo) => {
      TokenStorage.set(token);
      UserStorage.set(user);
      set({ token, user });
    },

    // 登出
    logout: () => {
      clearAuth();
      set({ token: null, user: null });
    },
  };
}

// 导出 store 实例
export const authStore = createAuthStore();

// 导出派生的 isLoggedIn
export const isLoggedIn = derived(authStore, ($auth) => !!$auth.token);
