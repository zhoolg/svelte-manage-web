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
import { writable, derived, get } from 'svelte/store';
import frameworkZhCN from './zh-CN';
import frameworkEnUS from './en-US';
// 语言选项（带图标）
export const localeOptions = [
    { label: '简体中文', value: 'zh-CN', icon: '🇨🇳' },
    { label: 'English', value: 'en-US', icon: '🇺🇸' },
];
/**
 * 检测浏览器语言
 */
export function detectBrowserLocale() {
    if (typeof window === 'undefined')
        return 'zh-CN';
    const browserLang = navigator.language || 'zh-CN';
    // 精确匹配
    if (browserLang === 'zh-CN' || browserLang === 'en-US') {
        return browserLang;
    }
    // 模糊匹配
    const langPrefix = browserLang.split('-')[0].toLowerCase();
    if (langPrefix === 'zh')
        return 'zh-CN';
    if (langPrefix === 'en')
        return 'en-US';
    // 默认中文
    return 'zh-CN';
}
// ============================================================
// 深度合并工具函数
// ============================================================
function deepMerge(target, source) {
    const result = { ...target };
    for (const key in source) {
        if (Object.prototype.hasOwnProperty.call(source, key)) {
            const sourceValue = source[key];
            const targetValue = result[key];
            if (sourceValue &&
                typeof sourceValue === 'object' &&
                !Array.isArray(sourceValue) &&
                targetValue &&
                typeof targetValue === 'object' &&
                !Array.isArray(targetValue)) {
                result[key] = deepMerge(targetValue, sourceValue);
            }
            else if (sourceValue !== undefined) {
                result[key] = sourceValue;
            }
        }
    }
    return result;
}
// ============================================================
// 翻译函数工具
// ============================================================
/**
 * 获取嵌套对象的值
 */
function getNestedValue(obj, path) {
    const keys = path.split('.');
    let result = obj;
    for (const key of keys) {
        if (result && typeof result === 'object' && key in result) {
            result = result[key];
        }
        else {
            return path; // 找不到则返回原始 key
        }
    }
    return typeof result === 'string' ? result : path;
}
/**
 * 替换变量
 */
function replaceVariables(text, variables) {
    if (!variables)
        return text;
    return text.replace(/\{(\w+)\}/g, (_, key) => {
        return variables[key] !== undefined ? String(variables[key]) : `{${key}}`;
    });
}
// 全局实例（单例）
let globalInstance = null;
/**
 * 初始化国际化
 * @param userLocales 业务项目的翻译包
 * @returns 国际化实例
 */
export function initI18n(userLocales = {}) {
    // 合并框架翻译和业务翻译
    const messages = {
        'zh-CN': deepMerge(frameworkZhCN, userLocales['zh-CN'] || {}),
        'en-US': deepMerge(frameworkEnUS, userLocales['en-US'] || {}),
    };
    // 从 localStorage 读取保存的语言设置
    function getInitialLocale() {
        if (typeof window === 'undefined')
            return 'zh-CN';
        const saved = localStorage.getItem('locale-storage');
        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                if (parsed.locale && (parsed.locale === 'zh-CN' || parsed.locale === 'en-US')) {
                    return parsed.locale;
                }
            }
            catch {
                // 忽略解析错误
            }
        }
        return detectBrowserLocale();
    }
    // 创建 locale store
    const initialLocale = getInitialLocale();
    const locale = writable(initialLocale);
    // 设置语言
    function setLocale(newLocale) {
        locale.set(newLocale);
        if (typeof window !== 'undefined') {
            localStorage.setItem('locale-storage', JSON.stringify({ locale: newLocale }));
            document.documentElement.lang = newLocale;
        }
    }
    // 获取当前语言
    function getLocale() {
        return get(locale);
    }
    // 创建翻译函数
    function createTranslator(currentLocale) {
        const currentMessages = messages[currentLocale];
        return function t(key, variables) {
            const text = getNestedValue(currentMessages, key);
            return replaceVariables(text, variables);
        };
    }
    // 派生翻译函数 store
    const t = derived(locale, ($locale) => createTranslator($locale));
    // 获取翻译函数（非响应式）
    function getTranslator() {
        return createTranslator(get(locale));
    }
    // 初始化 HTML lang 属性
    if (typeof window !== 'undefined') {
        document.documentElement.lang = initialLocale;
    }
    const instance = {
        locale,
        t,
        setLocale,
        getLocale,
        getTranslator,
    };
    // 保存全局实例
    globalInstance = instance;
    return instance;
}
/**
 * 获取全局国际化实例
 * 如果未初始化，则使用默认配置初始化
 */
export function getI18n() {
    if (!globalInstance) {
        globalInstance = initI18n();
    }
    return globalInstance;
}
// 导出框架基础翻译（供业务项目参考）
export { frameworkZhCN, frameworkEnUS };
