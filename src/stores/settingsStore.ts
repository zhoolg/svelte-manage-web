/**
 * 设置状态管理 - Svelte Store
 * 替代 React 的 Zustand useSettingsStore
 */
import { writable } from 'svelte/store';

// 浏览器环境检测
const browser = typeof window !== 'undefined';

interface SettingsState {
  theme: 'light' | 'dark' | 'system';
  sidebarCollapsed: boolean;
  showTagsView: boolean;
  showBreadcrumb: boolean;
  showFooter: boolean;
  primaryColor: string;
  layoutMode: 'side' | 'top';
  fixedHeader: boolean;
}

const DEFAULT_PRIMARY_COLOR = '#409eff';

const defaultSettings: SettingsState = {
  theme: 'light',
  sidebarCollapsed: false,
  showTagsView: true,
  showBreadcrumb: true,
  showFooter: true,
  primaryColor: DEFAULT_PRIMARY_COLOR,
  layoutMode: 'side',
  fixedHeader: true,
};

// 从 localStorage 加载
function loadSettings(): SettingsState {
  if (!browser) return defaultSettings;

  try {
    const stored = localStorage.getItem('settings-storage');
    if (stored) {
      const parsed = JSON.parse(stored);
      return { ...defaultSettings, ...parsed.state };
    }
  } catch (e) {
    console.error('Failed to load settings:', e);
  }
  return defaultSettings;
}

// 保存到 localStorage
function saveSettings(settings: SettingsState) {
  if (!browser) return;

  try {
    localStorage.setItem('settings-storage', JSON.stringify({ state: settings }));
  } catch (e) {
    console.error('Failed to save settings:', e);
  }
}

// 应用主题
function applyTheme(theme: 'light' | 'dark' | 'system') {
  if (!browser) return;

  console.log('[Theme] Applying theme:', theme);

  if (theme === 'dark') {
    document.documentElement.classList.add('dark');
    console.log('[Theme] Added dark class');
  } else if (theme === 'light') {
    document.documentElement.classList.remove('dark');
    console.log('[Theme] Removed dark class');
  } else {
    // system
    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    console.log('[Theme] System preference:', isDark ? 'dark' : 'light');
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  console.log('[Theme] Current classes:', document.documentElement.className);
}

// 应用主题色
function applyPrimaryColor(color: string) {
  if (!browser) return;

  const styleId = 'theme-primary-color';
  let styleEl = document.getElementById(styleId) as HTMLStyleElement | null;

  if (color === DEFAULT_PRIMARY_COLOR) {
    if (styleEl) styleEl.remove();
    return;
  }

  if (!styleEl) {
    styleEl = document.createElement('style');
    styleEl.id = styleId;
    document.head.appendChild(styleEl);
  }

  const lighterColor = getLighterColor(color, 0.3);
  const darkerColor = getDarkerColor(color, 0.2);
  const lightestColor = getLighterColor(color, 0.95);
  const lightBgColor = getLighterColor(color, 0.9);

  styleEl.textContent = `
    :root {
      --primary-color: ${color};
      --primary-light: ${lighterColor};
      --primary-dark: ${darkerColor};
      --primary-lightest: ${lightestColor};
      --primary-bg: ${lightBgColor};
    }

    /* ========== 背景色 ========== */
    /* 主色背景 */
    [class*="bg-[#409eff]"],
    .bg-\\[\\#409eff\\] { background-color: ${color} !important; }

    /* 浅色背景 */
    [class*="bg-[#66b1ff]"],
    .bg-\\[\\#66b1ff\\] { background-color: ${lighterColor} !important; }

    /* 最浅背景 #ecf5ff */
    [class*="bg-[#ecf5ff]"],
    .bg-\\[\\#ecf5ff\\] { background-color: ${lightBgColor} !important; }

    /* 透明度变体 */
    [class*="bg-[#409eff]/5"],
    .bg-\\[\\#409eff\\]\\/5 { background-color: ${lightestColor} !important; }

    [class*="bg-[#409eff]/10"],
    .bg-\\[\\#409eff\\]\\/10 { background-color: ${lightBgColor} !important; }

    /* ========== 文字颜色 ========== */
    [class*="text-[#409eff]"],
    .text-\\[\\#409eff\\] { color: ${color} !important; }

    /* ========== 边框颜色 ========== */
    [class*="border-[#409eff]"],
    .border-\\[\\#409eff\\] { border-color: ${color} !important; }

    /* ========== 悬停状态 ========== */
    /* 背景悬停 */
    [class*="hover:bg-[#409eff]"]:hover,
    .hover\\:bg-\\[\\#409eff\\]:hover { background-color: ${color} !important; }

    [class*="hover:bg-[#66b1ff]"]:hover,
    .hover\\:bg-\\[\\#66b1ff\\]:hover { background-color: ${lighterColor} !important; }

    [class*="hover:bg-[#ecf5ff]"]:hover,
    .hover\\:bg-\\[\\#ecf5ff\\]:hover { background-color: ${lightBgColor} !important; }

    /* 文字悬停 */
    [class*="hover:text-[#409eff]"]:hover,
    .hover\\:text-\\[\\#409eff\\]:hover { color: ${color} !important; }

    /* 边框悬停 */
    [class*="hover:border-[#409eff]"]:hover,
    .hover\\:border-\\[\\#409eff\\]:hover { border-color: ${color} !important; }

    /* ========== 焦点状态 ========== */
    [class*="focus:border-[#409eff]"]:focus,
    .focus\\:border-\\[\\#409eff\\]:focus { border-color: ${color} !important; }

    [class*="focus:ring-[#409eff]"]:focus,
    .focus\\:ring-\\[\\#409eff\\]:focus { --tw-ring-color: ${color} !important; }

    /* ========== Ring 颜色 ========== */
    [class*="ring-[#409eff]"],
    .ring-\\[\\#409eff\\] { --tw-ring-color: ${color} !important; }

    /* ========== 渐变色 ========== */
    [class*="from-[#409eff]"],
    .from-\\[\\#409eff\\] { --tw-gradient-from: ${color} !important; }

    [class*="to-[#66b1ff]"],
    .to-\\[\\#66b1ff\\] { --tw-gradient-to: ${lighterColor} !important; }

    /* ========== 分隔线 ========== */
    [class*="divide-[#409eff]"],
    .divide-\\[\\#409eff\\] > * + * { border-color: ${color} !important; }

    /* ========== 复选框和单选框 ========== */
    input[type="checkbox"].text-\\[\\#409eff\\]:checked,
    input[type="radio"].text-\\[\\#409eff\\]:checked {
      background-color: ${color} !important;
      border-color: ${color} !important;
    }

    /* ========== 活动状态 ========== */
    [class*="active:bg-[#409eff]"]:active,
    .active\\:bg-\\[\\#409eff\\]:active { background-color: ${darkerColor} !important; }

    [class*="active:bg-[#3a8ee6]"]:active,
    .active\\:bg-\\[\\#3a8ee6\\]:active { background-color: ${darkerColor} !important; }

    /* ========== 禁用状态 ========== */
    [class*="disabled:bg-[#409eff]"]:disabled,
    .disabled\\:bg-\\[\\#409eff\\]:disabled { background-color: ${color} !important; opacity: 0.5; }

    /* ========== 深色变体 #3a8ee6 ========== */
    [class*="bg-[#3a8ee6]"],
    .bg-\\[\\#3a8ee6\\] { background-color: ${darkerColor} !important; }

    [class*="hover:bg-[#3a8ee6]"]:hover,
    .hover\\:bg-\\[\\#3a8ee6\\]:hover { background-color: ${darkerColor} !important; }

    [class*="text-[#3a8ee6]"],
    .text-\\[\\#3a8ee6\\] { color: ${darkerColor} !important; }

    [class*="hover:text-[#66b1ff]"]:hover,
    .hover\\:text-\\[\\#66b1ff\\]:hover { color: ${lighterColor} !important; }

    [class*="text-[#66b1ff]"],
    .text-\\[\\#66b1ff\\] { color: ${lighterColor} !important; }
  `;
}

function getLighterColor(hex: string, amount: number = 0.3): string {
  const color = hex.replace('#', '');
  const r = parseInt(color.substring(0, 2), 16);
  const g = parseInt(color.substring(2, 4), 16);
  const b = parseInt(color.substring(4, 6), 16);
  const mix = (c: number) => Math.round(c + (255 - c) * amount);
  const toHex = (c: number) => c.toString(16).padStart(2, '0');
  return `#${toHex(mix(r))}${toHex(mix(g))}${toHex(mix(b))}`;
}

function getDarkerColor(hex: string, amount: number = 0.2): string {
  const color = hex.replace('#', '');
  const r = parseInt(color.substring(0, 2), 16);
  const g = parseInt(color.substring(2, 4), 16);
  const b = parseInt(color.substring(4, 6), 16);
  const mix = (c: number) => Math.round(c * (1 - amount));
  const toHex = (c: number) => c.toString(16).padStart(2, '0');
  return `#${toHex(mix(r))}${toHex(mix(g))}${toHex(mix(b))}`;
}

// 创建 store
function createSettingsStore() {
  const initialState = loadSettings();
  const { subscribe, set, update } = writable<SettingsState>(initialState);

  // 订阅变化并自动保存
  if (browser) {
    subscribe(state => {
      saveSettings(state);
    });
  }

  return {
    subscribe,

    setTheme: (theme: 'light' | 'dark' | 'system') => {
      console.log('[Store] setTheme called with:', theme);
      // 立即应用主题
      applyTheme(theme);
      // 更新状态
      update(state => {
        console.log('[Store] Updating state, old theme:', state.theme, 'new theme:', theme);
        return { ...state, theme };
      });
    },

    setSidebarCollapsed: (collapsed: boolean) => {
      update(state => ({ ...state, sidebarCollapsed: collapsed }));
    },

    toggleSidebar: () => {
      update(state => ({ ...state, sidebarCollapsed: !state.sidebarCollapsed }));
    },

    setShowTagsView: (show: boolean) => {
      update(state => ({ ...state, showTagsView: show }));
    },

    setShowBreadcrumb: (show: boolean) => {
      update(state => ({ ...state, showBreadcrumb: show }));
    },

    setShowFooter: (show: boolean) => {
      update(state => ({ ...state, showFooter: show }));
    },

    setPrimaryColor: (color: string) => {
      // 立即应用主题色
      applyPrimaryColor(color);
      // 更新状态
      update(state => ({ ...state, primaryColor: color }));
    },

    setLayoutMode: (mode: 'side' | 'top') => {
      update(state => ({ ...state, layoutMode: mode }));
    },

    setFixedHeader: (fixed: boolean) => {
      update(state => ({ ...state, fixedHeader: fixed }));
    },

    resetSettings: () => {
      set(defaultSettings);
      applyTheme(defaultSettings.theme);
      applyPrimaryColor(DEFAULT_PRIMARY_COLOR);
    },
  };
}

export const settingsStore = createSettingsStore();

// 初始化主题（在浏览器环境）
export function initTheme() {
  if (!browser) return;

  const settings = loadSettings();
  applyTheme(settings.theme);
  if (settings.primaryColor && settings.primaryColor !== DEFAULT_PRIMARY_COLOR) {
    applyPrimaryColor(settings.primaryColor);
  }
}
