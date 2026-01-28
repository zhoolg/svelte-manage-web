declare const Sidebar: import("svelte").Component<{
    /** 是否折叠 */
    collapsed?: boolean;
    /** 菜单配置 */
    menuConfig: MenuItem[];
    /** 当前路径 */
    currentPath: string;
    /** 导航函数 */
    onNavigate: (path: string) => void;
    /** 应用标题 */
    appTitle?: string;
    /** Logo 图标 */
    logoIcon?: string;
}, {}, "">;
type Sidebar = ReturnType<typeof Sidebar>;
export default Sidebar;
//# sourceMappingURL=Sidebar.svelte.d.ts.map