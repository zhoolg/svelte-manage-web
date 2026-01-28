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
declare const Permission: $$__sveltets_2_IsomorphicComponent<$$__sveltets_2_PropsWithChildren<{
    /** 单个权限码 */ permission?: string | undefined;
    /** 多个权限码 */ permissions?: string[] | undefined;
    /** 单个角色 */ role?: string | undefined;
    /** 多个角色 */ roles?: string[] | undefined;
    /** 检查模式：OR（任一）或 AND（全部） */ mode?: "OR" | "AND";
    /** 反向控制：true 时无权限才显示 */ fallback?: boolean;
}, {
    default: {};
}>, {
    [evt: string]: CustomEvent<any>;
}, {
    default: {};
}, {}, string>;
type Permission = InstanceType<typeof Permission>;
export default Permission;
//# sourceMappingURL=Permission.svelte.d.ts.map