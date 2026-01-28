/**
 * Confirm 弹窗工具
 * 通过 window 对象访问，在 Modal.svelte 组件挂载后可用
 * @zhoolg/svelte-admin-framework
 */
export interface ConfirmOptions {
    title?: string;
    content: string;
    type?: 'info' | 'warning' | 'danger';
    confirmText?: string;
    cancelText?: string;
}
export declare const confirm: (options: ConfirmOptions) => Promise<boolean>;
export default confirm;
//# sourceMappingURL=confirm.d.ts.map