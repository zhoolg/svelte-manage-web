/// <reference types="svelte" />
/// <reference types="vite/client" />

interface ImportMetaEnv {
  // 应用基础信息
  readonly VITE_APP_TITLE: string;
  readonly VITE_APP_SHORT_TITLE: string;
  readonly VITE_APP_DESCRIPTION: string;
  readonly VITE_APP_LOGO_ICON: string;
  readonly VITE_APP_VERSION: string;
  readonly VITE_APP_COPYRIGHT_OWNER: string;
  readonly VITE_APP_FAVICON?: string;

  // API 配置
  readonly VITE_API_BASE_URL: string;
  readonly VITE_APP_BASE_API: string;
  readonly VITE_APP_TARGET_URL: string;

  // 开发服务器配置
  readonly VITE_PORT: string;

  // 功能开关
  readonly VITE_ENABLE_MOCK: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
