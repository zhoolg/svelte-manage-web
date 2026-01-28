import { type MenuItem } from './Sidebar.svelte';
interface Props {
    /** 菜单配置 */
    menuConfig: MenuItem[];
    /** 当前路径 */
    currentPath: string;
    /** 路由名称映射 */
    routeNames: Record<string, string>;
    /** 导航函数 */
    onNavigate: (path: string) => void;
    /** 应用标题 */
    appTitle?: string;
    /** Logo 图标 */
    logoIcon?: string;
    /** 是否显示标签页 */
    showTagsView?: boolean;
    /** 自定义 Header 组件 */
    children?: import('svelte').Snippet;
    /** Header 插槽 */
    header?: import('svelte').Snippet<[{
        collapsed: boolean;
        onToggle: () => void;
    }]>;
}
interface $$__sveltets_2_IsomorphicComponent<Props extends Record<string, any> = any, Events extends Record<string, any> = any, Slots extends Record<string, any> = any, Exports = {}, Bindings = string> {
    new (options: import('svelte').ComponentConstructorOptions<Props>): import('svelte').SvelteComponent<Props, Events, Slots> & {
        $$bindings?: Bindings;
    } & Exports;
    (internal: unknown, props: Props & {
        $$events?: Events;
        $$slots?: Slots;
    }): Exports & {
        $set?: any;
        $on?: any;
    };
    z_$$bindings?: Bindings;
}
type $$__sveltets_2_PropsWithChildren<Props, Slots> = Props & (Slots extends {
    default: any;
} ? Props extends Record<string, never> ? any : {
    children?: any;
} : {});
declare const AdminLayout: $$__sveltets_2_IsomorphicComponent<$$__sveltets_2_PropsWithChildren<Props, {
    default: {};
}>, {
    [evt: string]: CustomEvent<any>;
}, {
    default: {};
}, {}, "">;
type AdminLayout = InstanceType<typeof AdminLayout>;
export default AdminLayout;
//# sourceMappingURL=AdminLayout.svelte.d.ts.map