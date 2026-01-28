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
export { defineModule, createModuleRegistry, toMenuConfig } from './config';
export { authStore, isLoggedIn, permissionStore, permissions, roles, isAdmin, hasPermission, usePermission, settingsStore, initTheme, visitHistoryStore, recordVisit, topVisitedPaths, getVisitCount, clearVisitHistory, } from './stores';
export { toast, confirm, TokenStorage, UserStorage, clearAuth } from './utils';
export { request, get, post, put, del, setBaseUrl, getBaseUrl, ApiError } from './api';
export { initI18n, getI18n, detectBrowserLocale, localeOptions, frameworkZhCN, frameworkEnUS, } from './locales';
export { 
// UI 组件
Icon, Loading, Empty, Toast, Modal, Permission, 
// 表单组件
FormSelect, DatePicker, DateRangePicker, TagInput, ImageUpload, 
// 布局组件
AdminLayout, Sidebar, TagsView, 
// 核心组件
CrudPage, 
// 页面组件
NotFound, } from './components';
