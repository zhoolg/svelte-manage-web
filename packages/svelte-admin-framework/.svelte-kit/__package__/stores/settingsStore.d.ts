export interface SettingsState {
    theme: 'light' | 'dark' | 'system';
    sidebarCollapsed: boolean;
    showTagsView: boolean;
    showBreadcrumb: boolean;
    showFooter: boolean;
    layoutMode: 'side' | 'top';
    fixedHeader: boolean;
}
export declare const settingsStore: {
    subscribe: (this: void, run: import("svelte/store").Subscriber<SettingsState>, invalidate?: () => void) => import("svelte/store").Unsubscriber;
    setTheme: (theme: "light" | "dark" | "system") => void;
    setSidebarCollapsed: (collapsed: boolean) => void;
    toggleSidebar: () => void;
    setShowTagsView: (show: boolean) => void;
    setShowBreadcrumb: (show: boolean) => void;
    setShowFooter: (show: boolean) => void;
    setLayoutMode: (mode: "side" | "top") => void;
    setFixedHeader: (fixed: boolean) => void;
    resetSettings: () => void;
};
export declare function initTheme(): void;
//# sourceMappingURL=settingsStore.d.ts.map