/**
 * 认证相关 API 服务
 * 统一管理所有认证相关的接口调用
 */
import { post } from './request';

export interface LoginResponse {
  token: string;
  user: {
    username: string;
    name: string;
    roles: string[];
  };
  permissions: string[];
  isAdmin: boolean;
}

export interface LoginParams {
  username: string;
  password: string;
  captcha: string;
}

/**
 * 认证 API 服务
 */
export const authApi = {
  /**
   * 用户登录
   */
  login: (params: LoginParams) =>
    post<LoginResponse>('/auth/login', params),

  /**
   * 用户登出
   */
  logout: () =>
    post('/auth/logout'),

  /**
   * 刷新 Token
   */
  refreshToken: () =>
    post('/auth/refresh'),
};
