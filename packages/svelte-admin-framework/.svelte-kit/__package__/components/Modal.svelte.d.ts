export interface ConfirmOptions {
    title?: string;
    content: string;
    type?: 'info' | 'warning' | 'danger';
    confirmText?: string;
    cancelText?: string;
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
declare const Modal: $$__sveltets_2_IsomorphicComponent<{
    confirm?: (options: ConfirmOptions) => Promise<boolean>;
}, {
    [evt: string]: CustomEvent<any>;
}, {}, {
    confirm: (options: ConfirmOptions) => Promise<boolean>;
}, string>;
type Modal = InstanceType<typeof Modal>;
export default Modal;
//# sourceMappingURL=Modal.svelte.d.ts.map