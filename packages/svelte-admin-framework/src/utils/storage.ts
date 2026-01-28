/**
 * 本地存储工具
 * @zhoolg/svelte-admin-framework
 */

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_info';

// Token 管理
export const TokenStorage = {
  get: (): string | null => {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(TOKEN_KEY);
  },
  set: (token: string) => {
    if (typeof window === 'undefined') return;
    localStorage.setItem(TOKEN_KEY, token);
  },
  remove: () => {
    if (typeof window === 'undefined') return;
    localStorage.removeItem(TOKEN_KEY);
  },
};

// 用户信息管理
export const UserStorage = {
  get: <T = unknown>(): T | null => {
    if (typeof window === 'undefined') return null;
    const data = localStorage.getItem(USER_KEY);
    return data ? JSON.parse(data) : null;
  },
  set: <T>(user: T) => {
    if (typeof window === 'undefined') return;
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  remove: () => {
    if (typeof window === 'undefined') return;
    localStorage.removeItem(USER_KEY);
  },
};

// 清除所有登录信息
export const clearAuth = () => {
  TokenStorage.remove();
  UserStorage.remove();
};
