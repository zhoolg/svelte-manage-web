/**
 * 后台认证 API。
 */
import { del, get, post, put } from './request';

export interface CaptchaResponse {
  captchaId: string;
  image: string;
}

export interface LoginPayload {
  accountNo: string;
  password: string;
  captchaId: string;
  captcha: string;
}

export interface LoginResponse {
  user?: {
    id?: number;
    username?: string;
    name?: string;
    avatar?: string;
    roles?: string[];
    usingDefaultPassword?: boolean;
  };
  permissions?: string[];
  isAdmin?: boolean;
  usingDefaultPassword?: boolean;
}

export interface UserPayload {
  id?: number;
  username?: string;
  name?: string;
  avatar?: string;
  roles?: string[];
  usingDefaultPassword?: boolean;
}

export interface UpdateProfilePayload {
  name: string;
  avatar?: string | null;
}

export interface ChangePasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface PasskeyStartResponse {
  requestId: string;
  publicKey: Record<string, unknown>;
}

export interface PasskeyRegisterFinishPayload {
  requestId: string;
  credentialJson: string;
  displayName?: string;
}

export interface PasskeyLoginStartPayload {
  username?: string;
}

export interface PasskeyLoginFinishPayload {
  requestId: string;
  credentialJson: string;
}

export interface PasskeyCredential {
  id: number;
  displayName: string;
  username: string;
  lastUsedTime?: string | null;
  createTime?: string | null;
}

export const authApi = {
  getCaptcha: () => get<CaptchaResponse>('/admin/auth/captcha'),
  login: (payload: LoginPayload) => post<LoginResponse>('/admin/auth/login', payload),
  startPasskeyRegistration: () =>
    post<PasskeyStartResponse>('/admin/auth/passkeys/registration/options'),
  finishPasskeyRegistration: (payload: PasskeyRegisterFinishPayload) =>
    post<{ success: boolean }>('/admin/auth/passkeys/registration/finish', payload),
  listPasskeys: () => get<PasskeyCredential[]>('/admin/auth/passkeys'),
  deletePasskey: (id: number) => del<{ success: boolean }>(`/admin/auth/passkeys/${id}`),
  startPasskeyLogin: (payload: PasskeyLoginStartPayload = {}) =>
    post<PasskeyStartResponse>('/admin/auth/passkeys/assertion/options', payload),
  finishPasskeyLogin: (payload: PasskeyLoginFinishPayload) =>
    post<LoginResponse>('/admin/auth/passkeys/assertion/finish', payload),
  getMe: () => get<UserPayload>('/admin/auth/me'),
  updateProfile: (payload: UpdateProfilePayload) =>
    put<UserPayload>('/admin/auth/profile', payload),
  changePassword: (payload: ChangePasswordPayload) =>
    put<{ success: boolean }>('/admin/auth/password', payload),
  logout: () => post('/admin/auth/logout'),
};
