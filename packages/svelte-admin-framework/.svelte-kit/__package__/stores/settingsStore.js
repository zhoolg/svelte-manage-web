/**
 * 设置状态管理 - Svelte Store
 * @zhoolg/svelte-admin-framework
 */
import { writable } from 'svelte/store';
// 浏览器环境检测
const browser = typeof window !== 'undefined';
const defaultSettings = {
    theme: 'light',
    sidebarCollapsed: false,
    showTagsView: true,
    showBreadcrumb: true,
    showFooter: true,
    layoutMode: 'side',
    fixedHeader: true,
};
// 从 localStorage 加载
function loadSettings() {
    if (!browser)
        return defaultSettings;
    try {
        const stored = localStorage.getItem('settings-storage');
        if (stored) {
            const parsed = JSON.parse(stored);
            return { ...defaultSettings, ...parsed.state };
        }
    }
    catch {
        // 忽略错误
    }
    return defaultSettings;
}
// 保存到 localStorage
function saveSettings(settings) {
    if (!browser)
        return;
    try {
        localStorage.setItem('settings-storage', JSON.stringify({ state: settings }));
    }
    catch {
        // 忽略错误
    }
}
// 应用主题
function applyTheme(theme) {
    if (!browser)
        return;
    if (theme === 'dark') {
        document.documentElement.classList.add('dark');
    }
    else if (theme === 'light') {
        document.documentElement.classList.remove('dark');
    }
    else {
        // system
        const isDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        if (isDark) {
            document.documentElement.classList.add('dark');
        }
        else {
            document.documentElement.classList.remove('dark');
        }
    }
}
// 创建 store
function createSettingsStore() {
    const initialState = loadSettings();
    const { subscribe, set, update } = writable(initialState);
    // 订阅变化并自动保存
    if (browser) {
        subscribe((state) => {
            saveSettings(state);
        });
    }
    return {
        subscribe,
        setTheme: (theme) => {
            applyTheme(theme);
            update((state) => ({ ...state, theme }));
        },
        setSidebarCollapsed: (collapsed) => {
            update((state) => ({ ...state, sidebarCollapsed: collapsed }));
        },
        toggleSidebar: () => {
            update((state) => ({ ...state, sidebarCollapsed: !state.sidebarCollapsed }));
        },
        setShowTagsView: (show) => {
            update((state) => ({ ...state, showTagsView: show }));
        },
        setShowBreadcrumb: (show) => {
            update((state) => ({ ...state, showBreadcrumb: show }));
        },
        setShowFooter: (show) => {
            update((state) => ({ ...state, showFooter: show }));
        },
        setLayoutMode: (mode) => {
            update((state) => ({ ...state, layoutMode: mode }));
        },
        setFixedHeader: (fixed) => {
            update((state) => ({ ...state, fixedHeader: fixed }));
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
    if (!browser)
        return;
    const settings = loadSettings();
    applyTheme(settings.theme);
}
