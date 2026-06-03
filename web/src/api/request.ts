import { authStore } from '../stores/authStore';
import { getTranslator } from '../lib/locales';
import { APP_CONFIG } from '../config/app.config';

export const BASE_URL = APP_CONFIG.apiBaseUrl;
export const AUTH_SESSION_EXPIRED_CODE = 40101;

export interface ApiResponse<T = unknown> {
  code: number;
  data: T;
  msg: string;
}

// 请求配置
interface RequestConfig extends RequestInit {
  timeout?: number;
}

// 错误类型
export class ApiError extends Error {
  code: number;
  constructor(message: string, code: number) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
}

// 请求拦截器：鉴权改为 httpOnly Cookie（随 credentials: 'include' 自动携带），不再手动设置 Authorization 头
const requestInterceptor = (config: RequestInit): RequestInit => {
  const headers = new Headers(config.headers);
  headers.set('Content-Type', 'application/json');
  return {
    ...config,
    headers,
  };
};

function isAuthEntryRequest(url: string): boolean {
  return (
    url.includes('/admin/auth/login') ||
    url.includes('/admin/auth/captcha') ||
    url.includes('/admin/auth/public-key') ||
    url.includes('/admin/auth/passkeys/assertion/')
  );
}

// 响应拦截器
const responseInterceptor = async <T>(response: Response, url: string): Promise<ApiResponse<T>> => {
  const t = getTranslator();
  let data: ApiResponse<T> | null = null;
  let text = '';

  try {
    text = await response.text();
    if (text) {
      data = JSON.parse(text);
    }
  } catch (e) {
    // 解析失败，如果 response 也不 ok，则在下面处理
  }

  // 如果解析到了业务数据
  if (data) {
    // 管理后台会话失效：清除登录态，App 会立即回到登录页。
    if (data.code === AUTH_SESSION_EXPIRED_CODE) {
      if (isAuthEntryRequest(url)) {
        throw new ApiError(data.msg || t('api.requestFailed'), data.code);
      }

      authStore.logout();
      throw new ApiError(t('message.sessionExpired'), data.code);
    }

    // 业务错误
    if (data.code !== 0 && data.code !== 200) {
      throw new ApiError(data.msg || t('api.requestFailed'), data.code);
    }

    return data as ApiResponse<T>;
  }

  // 如果没有解析到 JSON 数据，但响应不成功，则按 HTTP 状态码抛错
  if (!response.ok) {
    throw new ApiError(t('api.serverError', { status: response.status }), response.status);
  }

  // 如果响应成功但没有数据（且不是预期内的空），抛错
  if (!text) {
    throw new ApiError(t('api.noResponse'), 500);
  }

  // 这里的兜底处理，防止意外
  throw new ApiError(t('api.responseFormatError'), 500);
};

// 统一请求方法
export async function request<T>(url: string, config: RequestConfig = {}): Promise<ApiResponse<T>> {
  const { timeout = 15000, ...restConfig } = config;

  // 应用请求拦截器
  const finalConfig = requestInterceptor(restConfig);

  // 创建超时控制
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  const t = getTranslator();

  try {
    const response = await fetch(`${BASE_URL}${url}`, {
      ...finalConfig,
      credentials: 'include',
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    // 无论响应是否 ok，都尝试通过 responseInterceptor 解析内容
    return await responseInterceptor<T>(response, url);
  } catch (error) {
    clearTimeout(timeoutId);

    // 已经是 ApiError，直接抛出
    if (error instanceof ApiError) {
      throw error;
    }

    // 处理各种错误类型
    if (error instanceof Error) {
      // 超时错误
      if (error.name === 'AbortError') {
        throw new ApiError(t('api.timeout'), 408);
      }

      // 网络错误 - 包括代理错误
      if (
        error.message === 'Failed to fetch' ||
        error.message.includes('NetworkError') ||
        error.message.includes('network') ||
        error.message.includes('ECONNREFUSED') ||
        error.message.includes('ETIMEDOUT') ||
        error.message.includes('fetch')
      ) {
        throw new ApiError(t('api.networkError'), 0);
      }

      // 其他错误
      throw new ApiError(error.message || t('api.requestFailed'), 500);
    }

    throw new ApiError(t('api.unknownError'), 500);
  }
}

// GET 请求
export function get<T>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> {
  const queryString = params
    ? '?' +
      new URLSearchParams(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== null && v !== '')
          .map(([k, v]) => [k, String(v)])
      ).toString()
    : '';
  return request<T>(url + queryString);
}

// POST 请求
export function post<T>(
  url: string,
  data?: unknown,
  options?: { params?: Record<string, unknown> }
): Promise<ApiResponse<T>> {
  // 如果有 query 参数，添加到 URL
  const queryString = options?.params
    ? '?' +
      new URLSearchParams(
        Object.entries(options.params)
          .filter(([, v]) => v !== undefined && v !== null && v !== '')
          .map(([k, v]) => [k, String(v)])
      ).toString()
    : '';

  return request<T>(url + queryString, {
    method: 'POST',
    body: data ? JSON.stringify(data) : undefined,
  });
}

// PUT 请求
export function put<T>(url: string, data?: unknown): Promise<ApiResponse<T>> {
  return request<T>(url, {
    method: 'PUT',
    body: data ? JSON.stringify(data) : undefined,
  });
}

// DELETE 请求
export function del<T>(url: string): Promise<ApiResponse<T>> {
  return request<T>(url, {
    method: 'DELETE',
  });
}

export default { get, post, put, del };
