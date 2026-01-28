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
declare const FormSelect: $$__sveltets_2_IsomorphicComponent<{
    value?: string | number;
    options?: Array<{
        label: string;
        value: string | number;
    }>;
    placeholder?: string;
    disabled?: boolean;
}, {
    [evt: string]: CustomEvent<any>;
}, {}, {}, string>;
type FormSelect = InstanceType<typeof FormSelect>;
export default FormSelect;
//# sourceMappingURL=FormSelect.svelte.d.ts.map