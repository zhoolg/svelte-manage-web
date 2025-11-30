// 本地存储工具

const TOKEN_KEY = 'access_token';
const USER_KEY = 'user_info';

// Token 管理
export const TokenStorage = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  remove: () => localStorage.removeItem(TOKEN_KEY),
};

// 用户信息管理
export const UserStorage = {
  get: <T = unknown>(): T | null => {
    const data = localStorage.getItem(USER_KEY);
    return data ? JSON.parse(data) : null;
  },
  set: <T>(user: T) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  remove: () => localStorage.removeItem(USER_KEY),
};

// 清除所有登录信息
export const clearAuth = () => {
  TokenStorage.remove();
  UserStorage.remove();
};
