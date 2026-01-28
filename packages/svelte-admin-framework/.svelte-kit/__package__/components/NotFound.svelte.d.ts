interface Props {
    /** 当前访问的路径 */
    path?: string;
    /** 导航到首页的函数 */
    onGoHome?: () => void;
    /** 返回上一页的函数 */
    onGoBack?: () => void;
    /** 应用标题 */
    appTitle?: string;
    /** 支持邮箱 */
    supportEmail?: string;
}
declare const NotFound: import("svelte").Component<Props, {}, "">;
type NotFound = ReturnType<typeof NotFound>;
export default NotFound;
//# sourceMappingURL=NotFound.svelte.d.ts.map