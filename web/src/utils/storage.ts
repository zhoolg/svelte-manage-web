// 本地存储工具
//
// 注意：会话令牌已改为服务端 httpOnly Cookie，前端不再存储 token，
// 这里仅保留非敏感的用户信息用于 UI 展示。

const USER_KEY = 'user_info';

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
  UserStorage.remove();
};
