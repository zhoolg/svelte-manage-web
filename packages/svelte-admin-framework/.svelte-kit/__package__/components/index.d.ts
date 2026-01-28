/**
 * 组件统一导出
 * @zhoolg/svelte-admin-framework
 */
export { default as Icon } from './Icon.svelte';
export { default as Loading } from './Loading.svelte';
export { default as Empty } from './Empty.svelte';
export { default as Toast } from './Toast.svelte';
export { default as Modal } from './Modal.svelte';
export { default as Permission } from './Permission.svelte';
export { default as FormSelect } from './FormSelect.svelte';
export { default as DatePicker } from './DatePicker.svelte';
export { default as DateRangePicker } from './DateRangePicker.svelte';
export { default as TagInput } from './TagInput.svelte';
export { default as ImageUpload } from './ImageUpload.svelte';
export { default as AdminLayout } from './AdminLayout.svelte';
export { default as Sidebar, type MenuItem } from './Sidebar.svelte';
export { default as TagsView } from './TagsView.svelte';
export { default as CrudPage } from './CrudPage.svelte';
export { default as NotFound } from './NotFound.svelte';
export type { ToastMessage } from './Toast.svelte';
export type { ConfirmOptions } from './Modal.svelte';
/**
 * 以下组件需要从原项目复制并适配：
 *
 * 其他组件：
 * - Login.svelte         - 登录页组件（业务相关，建议在业务项目中实现）
 * - Captcha.svelte       - 验证码组件（需要 captchaUtils）
 */
//# sourceMappingURL=index.d.ts.map