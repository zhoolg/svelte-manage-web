/**
 * 状态管理统一导出
 * @zhoolg/svelte-admin-framework
 */
export { authStore, isLoggedIn, type UserInfo } from './authStore';
export { permissionStore, permissions, roles, isAdmin, hasPermission, usePermission, type Role, } from './permissionStore';
export { settingsStore, initTheme, type SettingsState } from './settingsStore';
export { visitHistoryStore, recordVisit, topVisitedPaths, getVisitCount, clearVisitHistory, } from './visitHistoryStore';
//# sourceMappingURL=index.d.ts.map