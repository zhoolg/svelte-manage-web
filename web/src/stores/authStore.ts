/**
 * 认证状态管理 - Svelte Store
 *
 * 会话令牌存于服务端下发的 httpOnly Cookie，前端 JS 无法读取（防 XSS 窃取）。
 * 因此前端不再持有 token，仅保存非敏感的用户信息用于 UI 展示与登录态判断；
 * 真正的鉴权以 Cookie 为准，任意请求返回 401/403 时即清除前端登录态。
 */
import { writable, derived } from 'svelte/store';
import { UserStorage, clearAuth } from '../utils/storage';

export interface UserInfo {
  id?: number;
  username?: string;
  name?: string;
  avatar?: string;
  roles?: string[];
  usingDefaultPassword?: boolean;
}

interface AuthState {
  user: UserInfo | null;
}

// 创建 store
function createAuthStore() {
  const { subscribe, set, update } = writable<AuthState>({
    user: null,
  });

  return {
    subscribe,

    // 初始化（从 localStorage 加载用户信息提示）
    init: () => {
      const user = UserStorage.get<UserInfo>();
      set({ user });
    },

    // 设置用户信息
    setUser: (user: UserInfo) => {
      UserStorage.set(user);
      update(state => ({ ...state, user }));
    },

    updateUser: (patch: Partial<UserInfo>) => {
      update(state => {
        const user = state.user ? { ...state.user, ...patch } : null;
        if (user) {
          UserStorage.set(user);
        }
        return { ...state, user };
      });
    },

    // 登录：仅保存用户信息，凭据由 httpOnly Cookie 持有
    login: (user: UserInfo) => {
      UserStorage.set(user);
      set({ user });
    },

    // 登出
    logout: () => {
      clearAuth();
      set({ user: null });
    },
  };
}

// 导出 store 实例
export const authStore = createAuthStore();

// 导出派生的 isLoggedIn（以是否有用户信息为准）
export const isLoggedIn = derived(authStore, $auth => !!$auth.user);
