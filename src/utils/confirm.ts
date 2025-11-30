/**
 * Confirm 弹窗工具
 * 通过 window 对象访问，在 Modal.svelte 组件挂载后可用
 */

export interface ConfirmOptions {
  title?: string;
  content: string;
  type?: 'info' | 'warning' | 'danger';
  confirmText?: string;
  cancelText?: string;
}

// 获取 confirm 实例
function getConfirm(): (options: ConfirmOptions) => Promise<boolean> {
  if (typeof window !== 'undefined' && (window as any).confirm) {
    return (window as any).confirm;
  }
  // 返回默认实现
  return async (options: ConfirmOptions) => {
    return window.confirm(options.content);
  };
}

export const confirm = (options: ConfirmOptions): Promise<boolean> => {
  return getConfirm()(options);
};

export default confirm;
