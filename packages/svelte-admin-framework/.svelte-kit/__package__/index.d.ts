/**
 * @zhoolg/svelte-admin-framework
 * 基于 Svelte 5 + TypeScript + Tailwind CSS 4 的管理后台框架
 *
 * @example
 * // 在业务项目中使用
 * import {
 *   // 配置系统
 *   createModuleRegistry,
 *   defineModule,
 *   type AppModule,
 *   type MenuStructure,
 *   type ModuleConfig,
 *
 *   // 状态管理
 *   authStore,
 *   permissionStore,
 *   settingsStore,
 *
 *   // 工具函数
 *   toast,
 *   confirm,
 *
 *   // API
 *   get,
 *   post,
 *   setBaseUrl,
 *
 *   // 国际化
 *   initI18n,
 *   getI18n,
 *
 *   // 组件
 *   CrudPage,
 *   AdminLayout,
 * } from '@zhoolg/svelte-admin-framework';
 */
export type { TableColumn, SearchField, SearchFieldType, FormField, FormFieldType, FormRule, ActionButton, ModuleConfig, AppModule, CrudConfig, MenuStructure, ModuleRegistry, } from './config';
export { defineModule, createModuleRegistry, toMenuConfig } from './config';
export type { UserInfo, Role, SettingsState } from './stores';
export { authStore, isLoggedIn, permissionStore, permissions, roles, isAdmin, hasPermission, usePermission, settingsStore, initTheme, visitHistoryStore, recordVisit, topVisitedPaths, getVisitCount, clearVisitHistory, } from './stores';
export type { ToastApi, ConfirmOptions } from './utils';
export { toast, confirm, TokenStorage, UserStorage, clearAuth } from './utils';
export type { ApiResponse } from './api';
export { request, get, post, put, del, setBaseUrl, getBaseUrl, ApiError } from './api';
export type { Locale, I18nInstance } from './locales';
export { initI18n, getI18n, detectBrowserLocale, localeOptions, frameworkZhCN, frameworkEnUS, } from './locales';
export type { ToastMessage, MenuItem } from './components';
export { Icon, Loading, Empty, Toast, Modal, Permission, FormSelect, DatePicker, DateRangePicker, TagInput, ImageUpload, AdminLayout, Sidebar, TagsView, CrudPage, NotFound, } from './components';
//# sourceMappingURL=index.d.ts.map