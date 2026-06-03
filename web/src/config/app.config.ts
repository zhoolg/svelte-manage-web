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
  copyright: `Copyright © ${new Date().getFullYear()} ${
    import.meta.env.VITE_APP_COPYRIGHT_OWNER || '管理平台'
  }. All Rights Reserved.`,

  /** API 基础路径 */
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',

  /** 图片基础路径 */
  imageBaseUrl: import.meta.env.VITE_IMAGE_BASE_URL || '',

  /** 是否启用 Mock */
  enableMock: import.meta.env.VITE_ENABLE_MOCK === 'true',
};

export default APP_CONFIG;
