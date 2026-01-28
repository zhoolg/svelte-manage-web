interface Props {
    /** 当前路径 */
    currentPath: string;
    /** 路由名称映射 */
    routeNames: Record<string, string>;
    /** 导航函数 */
    onNavigate: (path: string) => void;
}
declare const TagsView: import("svelte").Component<Props, {}, "">;
type TagsView = ReturnType<typeof TagsView>;
export default TagsView;
//# sourceMappingURL=TagsView.svelte.d.ts.map