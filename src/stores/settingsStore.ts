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
  layoutMode: 'side' | 'top';
  fixedHeader: boolean;
}

const defaultSettings: SettingsState = {
  theme: 'light',
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
      return { ...defaultSettings, ...parsed.state };
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

  // console.log('[Theme] Applying theme:', theme);

  if (theme === 'dark') {
    document.documentElement.classList.add('dark');
    // console.log('[Theme] Added dark class');
  } else if (theme === 'light') {
    document.documentElement.classList.remove('dark');
    // console.log('[Theme] Removed dark class');
  } else {
    // system
    const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    // console.log('[Theme] System preference:', isDark ? 'dark' : 'light');
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }

  // console.log('[Theme] Current classes:', document.documentElement.className);
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

    setLayoutMode: (mode: 'side' | 'top') => {
      update(state => ({ ...state, layoutMode: mode }));
    },

    setFixedHeader: (fixed: boolean) => {
      update(state => ({ ...state, fixedHeader: fixed }));
    },

    resetSettings: () => {
      set(defaultSettings);
      applyTheme(defaultSettings.theme);
    },
  };
}

export const settingsStore = createSettingsStore();

// 初始化主题（在浏览器环境）
export function initTheme() {
  if (!browser) return;

  const settings = loadSettings();
  applyTheme(settings.theme);
}
