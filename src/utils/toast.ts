/**
 * Toast 消息工具
 * 通过 window 对象访问，在 Toast.svelte 组件挂载后可用
 */

export interface ToastApi {
  success: (message: string) => void;
  error: (message: string) => void;
  warning: (message: string) => void;
  info: (message: string) => void;
}

// 获取 toast 实例
function getToast(): ToastApi {
  if (typeof window !== 'undefined' && (window as any).toast) {
    return (window as any).toast;
  }
  // 返回空实现，避免报错
  return {
    success: (msg) => console.log('[Toast Success]', msg),
    error: (msg) => console.error('[Toast Error]', msg),
    warning: (msg) => console.warn('[Toast Warning]', msg),
    info: (msg) => console.info('[Toast Info]', msg),
  };
}

export const toast: ToastApi = {
  success: (message: string) => getToast().success(message),
  error: (message: string) => getToast().error(message),
  warning: (message: string) => getToast().warning(message),
  info: (message: string) => getToast().info(message),
};

export default toast;
