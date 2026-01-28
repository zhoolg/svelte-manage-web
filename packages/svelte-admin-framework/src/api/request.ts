/**
 * HTTP 请求封装
 * @zhoolg/svelte-admin-framework
 */

import { TokenStorage, clearAuth } from '../utils/storage';
import { getI18n } from '../locales';

export interface ApiResponse<T = unknown> {
  code: number;
  data: T;
  msg: string;
}

interface RequestConfig extends RequestInit {
  timeout?: number;
}

export class ApiError extends Error {
  code: number;
  constructor(message: string, code: number) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
}

// 请求配置
let baseUrl = '/api';

/**
 * 设置 API 基础路径
 */
export function setBaseUrl(url: string) {
  baseUrl = url;
}

/**
 * 获取 API 基础路径
 */
export function getBaseUrl() {
  return baseUrl;
}

// 请求拦截器
const requestInterceptor = (config: RequestInit): RequestInit => {
  const token = TokenStorage.get();
  const headers = new Headers(config.headers);

  headers.set('Content-Type', 'application/json');

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  return {
    ...config,
    headers,
  };
};

// 响应拦截器
const responseInterceptor = async <T>(response: Response): Promise<ApiResponse<T>> => {
  const { getTranslator } = getI18n();
  const t = getTranslator();
  let data: ApiResponse<T>;

  try {
    const text = await response.text();
    if (!text) {
      throw new ApiError(t('api.noResponse'), 500);
    }
    data = JSON.parse(text);
  } catch (e) {
    if (e instanceof ApiError) {
      throw e;
    }
    throw new ApiError(t('api.responseFormatError'), 500);
  }

  // 未登录或 token 过期
  if (data.code === 401 || data.code === 403) {
    clearAuth();
    throw new ApiError(t('message.sessionExpired'), data.code);
  }

  // 业务错误
  if (data.code !== 0 && data.code !== 200) {
    throw new ApiError(data.msg || t('api.requestFailed'), data.code);
  }

  return data;
};

// 统一请求方法
export async function request<T>(url: string, config: RequestConfig = {}): Promise<ApiResponse<T>> {
  const { timeout = 15000, ...restConfig } = config;

  const finalConfig = requestInterceptor(restConfig);

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeout);

  const { getTranslator } = getI18n();
  const t = getTranslator();

  try {
    const response = await fetch(`${baseUrl}${url}`, {
      ...finalConfig,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new ApiError(t('api.serverError', { status: response.status }), response.status);
    }

    return await responseInterceptor<T>(response);
  } catch (error) {
    clearTimeout(timeoutId);

    if (error instanceof ApiError) {
      throw error;
    }

    if (error instanceof Error) {
      if (error.name === 'AbortError') {
        throw new ApiError(t('api.timeout'), 408);
      }

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

export default { get, post, put, del, setBaseUrl, getBaseUrl };
