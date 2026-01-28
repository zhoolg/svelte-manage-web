import type { ModuleConfig } from '../config/module';
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
declare const CrudPage: $$__sveltets_2_IsomorphicComponent<{
    config: ModuleConfig;
    onAction?: ((action: string, row: Record<string, unknown>) => void) | undefined;
    title?: string | undefined;
    showTitle?: boolean;
}, {
    [evt: string]: CustomEvent<any>;
}, {}, {
    title: string | undefined;
    showTitle: boolean;
}, string>;
type CrudPage = InstanceType<typeof CrudPage>;
export default CrudPage;
//# sourceMappingURL=CrudPage.svelte.d.ts.map