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
declare const Empty: $$__sveltets_2_IsomorphicComponent<{
    text?: string;
    icon?: string;
    description?: string;
}, {
    [evt: string]: CustomEvent<any>;
}, {}, {}, string>;
type Empty = InstanceType<typeof Empty>;
export default Empty;
//# sourceMappingURL=Empty.svelte.d.ts.map