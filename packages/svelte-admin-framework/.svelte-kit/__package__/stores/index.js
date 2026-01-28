/**
 * 状态管理统一导出
 * @zhoolg/svelte-admin-framework
 */
export { authStore, isLoggedIn } from './authStore';
export { permissionStore, permissions, roles, isAdmin, hasPermission, usePermission, } from './permissionStore';
export { settingsStore, initTheme } from './settingsStore';
export { visitHistoryStore, recordVisit, topVisitedPaths, getVisitCount, clearVisitHistory, } from './visitHistoryStore';
