/**
 * Confirm 弹窗工具
 * 通过 window 对象访问，在 Modal.svelte 组件挂载后可用
 * @zhoolg/svelte-admin-framework
 */
// 获取 confirm 实例
function getConfirm() {
    if (typeof window !== 'undefined' && window.confirm) {
        return window.confirm;
    }
    // 返回默认实现
    return async (options) => {
        return window.confirm(options.content);
    };
}
export const confirm = (options) => {
    return getConfirm()(options);
};
export default confirm;
