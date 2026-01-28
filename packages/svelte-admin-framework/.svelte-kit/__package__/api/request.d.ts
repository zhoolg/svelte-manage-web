/**
 * HTTP 请求封装
 * @zhoolg/svelte-admin-framework
 */
export interface ApiResponse<T = unknown> {
    code: number;
    data: T;
    msg: string;
}
interface RequestConfig extends RequestInit {
    timeout?: number;
}
export declare class ApiError extends Error {
    code: number;
    constructor(message: string, code: number);
}
/**
 * 设置 API 基础路径
 */
export declare function setBaseUrl(url: string): void;
/**
 * 获取 API 基础路径
 */
export declare function getBaseUrl(): string;
export declare function request<T>(url: string, config?: RequestConfig): Promise<ApiResponse<T>>;
export declare function get<T>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>>;
export declare function post<T>(url: string, data?: unknown, options?: {
    params?: Record<string, unknown>;
}): Promise<ApiResponse<T>>;
export declare function put<T>(url: string, data?: unknown): Promise<ApiResponse<T>>;
export declare function del<T>(url: string): Promise<ApiResponse<T>>;
declare const _default: {
    get: typeof get;
    post: typeof post;
    put: typeof put;
    del: typeof del;
    setBaseUrl: typeof setBaseUrl;
    getBaseUrl: typeof getBaseUrl;
};
export default _default;
//# sourceMappingURL=request.d.ts.map