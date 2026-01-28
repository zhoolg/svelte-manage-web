/**
 * Toast 消息工具
 * 通过 window 对象访问，在 Toast.svelte 组件挂载后可用
 * @zhoolg/svelte-admin-framework
 */
// 获取 toast 实例
function getToast() {
    if (typeof window !== 'undefined' && window.toast) {
        return window.toast;
    }
    // 返回空实现，避免报错
    return {
        success: (msg) => console.log('[Toast Success]', msg),
        error: (msg) => console.error('[Toast Error]', msg),
        warning: (msg) => console.warn('[Toast Warning]', msg),
        info: (msg) => console.info('[Toast Info]', msg),
    };
}
export const toast = {
    success: (message) => getToast().success(message),
    error: (message) => getToast().error(message),
    warning: (message) => getToast().warning(message),
    info: (message) => getToast().info(message),
};
export default toast;
