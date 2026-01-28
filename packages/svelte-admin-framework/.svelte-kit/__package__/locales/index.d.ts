/**
 * 国际化配置 - 支持业务项目扩展
 * ============================================================
 *
 * 使用说明：
 * 1. 在业务项目中调用 initI18n 初始化，传入业务翻译
 * 2. 使用 t('key') 获取翻译文本
 * 3. 支持嵌套 key，如 t('login.username')
 * 4. 支持变量替换，如 t('table.total', { total: 100 })
 *
 * @example
 * // 在业务项目中初始化
 * import { initI18n } from '@zhoolg/svelte-admin-framework/locales';
 * import zhCN from './locales/zh-CN';
 * import enUS from './locales/en-US';
 *
 * const { t, locale, setLocale } = initI18n({
 *   'zh-CN': zhCN,
 *   'en-US': enUS,
 * });
 *
 * @zhoolg/svelte-admin-framework
 */
import { type Readable, type Writable } from 'svelte/store';
import frameworkZhCN from './zh-CN';
import frameworkEnUS from './en-US';
export type Locale = 'zh-CN' | 'en-US';
type Messages = Record<string, unknown>;
export declare const localeOptions: {
    label: string;
    value: Locale;
    icon: string;
}[];
/**
 * 检测浏览器语言
 */
export declare function detectBrowserLocale(): Locale;
export interface I18nInstance {
    /** 当前语言 store */
    locale: Writable<Locale>;
    /** 翻译函数 store */
    t: Readable<(key: string, variables?: Record<string, unknown>) => string>;
    /** 设置语言 */
    setLocale: (locale: Locale) => void;
    /** 获取当前语言 */
    getLocale: () => Locale;
    /** 获取翻译函数（非响应式） */
    getTranslator: () => (key: string, variables?: Record<string, unknown>) => string;
}
/**
 * 初始化国际化
 * @param userLocales 业务项目的翻译包
 * @returns 国际化实例
 */
export declare function initI18n(userLocales?: {
    'zh-CN'?: Messages;
    'en-US'?: Messages;
}): I18nInstance;
/**
 * 获取全局国际化实例
 * 如果未初始化，则使用默认配置初始化
 */
export declare function getI18n(): I18nInstance;
export { frameworkZhCN, frameworkEnUS };
//# sourceMappingURL=index.d.ts.map