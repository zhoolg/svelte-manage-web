/**
 * Toast 消息工具
 * 通过 window 对象访问，在 Toast.svelte 组件挂载后可用
 * @zhoolg/svelte-admin-framework
 */
export interface ToastApi {
    success: (message: string) => void;
    error: (message: string) => void;
    warning: (message: string) => void;
    info: (message: string) => void;
}
export declare const toast: ToastApi;
export default toast;
//# sourceMappingURL=toast.d.ts.map