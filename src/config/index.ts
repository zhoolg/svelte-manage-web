/**
 * 配置文件统一导出
 * ============================================================
 *
 *  新版本使用方式：
 * import { APP_CONFIG, APP_MODULES } from '$config';
 *
 * 配置文件说明：
 * - app.modules.ts: 统一配置（菜单、路由、API、CRUD）← 推荐使用
 * - menu.ts: 菜单配置类型定义（兼容旧版）
 * - module.ts: 模块配置类型定义
 */

// ============================================================
// 应用基础配置（从环境变量读取）
// ============================================================

export const APP_CONFIG = {
  /** 应用标题 */
  title: import.meta.env.VITE_APP_TITLE || '管理平台',

  /** 应用简称 */
  shortTitle: import.meta.env.VITE_APP_SHORT_TITLE || '管理平台',

  /** 应用描述 */
  description: import.meta.env.VITE_APP_DESCRIPTION || '后台管理系统',

  /** Logo 图标（Lucide 图标名称） */
  logoIcon: import.meta.env.VITE_APP_LOGO_ICON || 'briefcase',

  /** 版本号 */
  version: import.meta.env.VITE_APP_VERSION || 'v1.0.0',

  /** 版权信息 */
  copyright: `Copyright © ${new Date().getFullYear()} ${import.meta.env.VITE_APP_COPYRIGHT_OWNER || '管理平台'}. All Rights Reserved.`,

  /** API 基础路径 */
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',

  /** 图片基础路径 */
  imageBaseUrl: import.meta.env.VITE_IMAGE_BASE_URL || '',

  /** 是否启用 Mock */
  enableMock: import.meta.env.VITE_ENABLE_MOCK === 'true',
};

// ============================================================
// 导出超级懒人配置（推荐使用）
// ============================================================

export * from './app.modules';

// ============================================================
// 导出菜单配置（兼容旧版）
// ============================================================

export * from './menu';

// ============================================================
// 导出模块配置类型
// ============================================================

export * from './module';

// ============================================================
// 默认导出
// ============================================================

export default APP_CONFIG;
