/**
 * 设置状态管理 - Svelte Store
 * 替代 React 的 Zustand useSettingsStore
 */
import { writable } from 'svelte/store';

// 浏览器环境检测
const browser = typeof window !== 'undefined';

type PrimaryColorPreset = 'stripe' | 'linear' | 'pure' | 'emerald' | 'sunset' | 'rose';

interface SettingsState {
  theme: 'light' | 'dark' | 'system';
  /** 主题主色预设 */
  primaryColor: PrimaryColorPreset;
  /** 界面圆角风格 */
  cornerStyle: 'default' | 'rounded';
  /** 界面密度 */
  density: 'comfortable' | 'compact';
  sidebarCollapsed: boolean;
  showTagsView: boolean;
  showBreadcrumb: boolean;
  showFooter: boolean;
  layoutMode: 'side' | 'top';
  fixedHeader: boolean;
}

const defaultSettings: SettingsState = {
  theme: 'light',
  primaryColor: 'pure',
  cornerStyle: 'default',
  density: 'comfortable',
  sidebarCollapsed: false,
  showTagsView: true,
  showBreadcrumb: true,
  showFooter: true,
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
      // Fallback for old color presets
      let loadedColor = parsed.state?.primaryColor;
      if (!['stripe', 'linear', 'pure', 'emerald', 'sunset', 'rose'].includes(loadedColor)) {
        loadedColor = 'pure';
      }
      return { ...defaultSettings, ...parsed.state, primaryColor: loadedColor };
    }
  } catch (e) {
    // console.error('Failed to load settings:', e);
  }
  return defaultSettings;
}

// 保存到 localStorage
function saveSettings(settings: SettingsState) {
  if (!browser) return;

  try {
    localStorage.setItem('settings-storage', JSON.stringify({ state: settings }));
  } catch (e) {
    // console.error('Failed to save settings:', e);
  }
}

// 应用主题
function applyTheme(theme: 'light' | 'dark' | 'system') {
  if (!browser) return;

  if (theme === 'dark') {
    document.documentElement.classList.add('dark');
  } else if (theme === 'light') {
    document.documentElement.classList.remove('dark');
  } else {
    // system
    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  // Re-apply primary color to ensure meta theme-color syncs with dark/light mode context if needed
  const state = loadSettings();
  applyPrimaryColor(state.primaryColor);
}

// 应用主题主色 (世界顶级配色方案)
function applyPrimaryColor(preset: PrimaryColorPreset) {
  if (!browser) return;

  const palette: Record<PrimaryColorPreset, { primary: string }> = {
    stripe: { primary: '#635BFF' }, // Stripe Blurple - 极致科技与金融感
    linear: { primary: '#5E6AD2' }, // Linear Indigo - 硅谷极简设计标准
    pure: { primary: '#007AFF' }, // Pure Blue - 极致纯净的原生蓝
    emerald: { primary: '#10B981' }, // Modern Emerald - 现代 Tailwind 清爽绿
    sunset: { primary: '#FF4500' }, // Sunset Orange - Reddit/Vibrant 活力橙
    rose: { primary: '#F43F5E' }, // Rose Red - 高端女性态/柔和玫瑰红
  };

  const current = palette[preset] || palette.pure;

  const rootStyle = document.documentElement.style;
  rootStyle.setProperty('--el-color-primary', current.primary);
  rootStyle.setProperty('--color-primary', current.primary);
  rootStyle.setProperty('--sidebar-active-text', current.primary);

  // 注入更多全局联动变量 (按钮悬浮、焦点环、柔和背景等)
  rootStyle.setProperty(
    '--color-primary-hover',
    `color-mix(in srgb, ${current.primary} 85%, black)`
  );
  rootStyle.setProperty(
    '--color-primary-light',
    `color-mix(in srgb, ${current.primary} 20%, white)`
  );

  // 动态修改浏览器的 theme-color (移动端顶栏、PWA 顶栏联动)
  let metaThemeColor = document.querySelector('meta[name="theme-color"]');
  if (!metaThemeColor) {
    metaThemeColor = document.createElement('meta');
    metaThemeColor.setAttribute('name', 'theme-color');
    document.head.appendChild(metaThemeColor);
  }

  // 检查是否暗黑模式，暗黑模式下头部通常是深色，非暗黑模式可以是主色或白色
  const isDark = document.documentElement.classList.contains('dark');
  metaThemeColor.setAttribute('content', isDark ? '#121212' : current.primary);
}

// 应用圆角风格
function applyCornerStyle(style: 'default' | 'rounded') {
  if (!browser) return;

  const rootStyle = document.documentElement.style;
  const radius = style === 'rounded' ? '10px' : '6px';
  rootStyle.setProperty('--radius-card', radius);
}

// 创建 store
function createSettingsStore() {
  const initialState = loadSettings();
  const { subscribe, set, update } = writable<SettingsState>(initialState);

  // 订阅变化并自动保存
  if (browser) {
    // 首次应用主题和主色
    applyTheme(initialState.theme);
    applyPrimaryColor(initialState.primaryColor);
    applyCornerStyle(initialState.cornerStyle);

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

    setPrimaryColor: (preset: PrimaryColorPreset) => {
      applyPrimaryColor(preset);
      update(state => ({ ...state, primaryColor: preset }));
    },

    setCornerStyle: (style: 'default' | 'rounded') => {
      applyCornerStyle(style);
      update(state => ({ ...state, cornerStyle: style }));
    },

    setDensity: (density: 'comfortable' | 'compact') => {
      update(state => ({ ...state, density }));
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

    setLayoutMode: (mode: 'side' | 'top') => {
      update(state => ({ ...state, layoutMode: mode }));
    },

    setFixedHeader: (fixed: boolean) => {
      update(state => ({ ...state, fixedHeader: fixed }));
    },

    resetSettings: () => {
      set(defaultSettings);
      applyTheme(defaultSettings.theme);
      applyPrimaryColor(defaultSettings.primaryColor);
      applyCornerStyle(defaultSettings.cornerStyle);
    },
  };
}

export const settingsStore = createSettingsStore();

// 初始化主题（在浏览器环境）
export function initTheme() {
  if (!browser) return;

  const settings = loadSettings();
  applyTheme(settings.theme);
  applyPrimaryColor(settings.primaryColor);
  applyCornerStyle(settings.cornerStyle);
}
