/**
 * 国际化配置
 * ============================================================
 *
 * 使用说明：
 * 1. 在组件中使用 locale store 获取翻译函数
 * 2. 使用 t('key') 获取翻译文本
 * 3. 支持嵌套 key，如 t('login.username')
 * 4. 支持变量替换，如 t('table.total', { total: 100 })
 *
 * @example
 * import { t, locale, setLocale } from '$lib/locales';
 * <span>{$t('common.confirm')}</span>
 * <span>{t('table.total', { total: 100 })}</span>
 */

import { writable, derived, get } from 'svelte/store';
import zhCN from './zh-CN';
import enUS from './en-US';

// 支持的语言
export type Locale = 'zh-CN' | 'en-US';

// 语言包类型
type Messages = typeof zhCN;

// 语言包映射
const messages: Record<Locale, Messages> = {
  'zh-CN': zhCN,
  'en-US': enUS,
};

// 语言选项（带图标）
export const localeOptions = [
  { label: '简体中文', value: 'zh-CN' as Locale, icon: '🇨🇳' },
  { label: 'English', value: 'en-US' as Locale, icon: '🇺🇸' },
];

/**
 * 检测浏览器语言
 */
export function detectBrowserLocale(): Locale {
  if (typeof window === 'undefined') return 'zh-CN';

  const browserLang = navigator.language || 'zh-CN';

  // 精确匹配
  if (browserLang === 'zh-CN' || browserLang === 'en-US') {
    return browserLang;
  }

  // 模糊匹配
  const langPrefix = browserLang.split('-')[0].toLowerCase();
  if (langPrefix === 'zh') return 'zh-CN';
  if (langPrefix === 'en') return 'en-US';

  // 默认中文
  return 'zh-CN';
}

// ============================================================
// 国际化状态管理
// ============================================================

// 从 localStorage 读取保存的语言设置
function getInitialLocale(): Locale {
  if (typeof window === 'undefined') return 'zh-CN';

  const saved = localStorage.getItem('locale-storage');
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      if (parsed.locale && (parsed.locale === 'zh-CN' || parsed.locale === 'en-US')) {
        return parsed.locale;
      }
    } catch {
      // 忽略解析错误
    }
  }
  return detectBrowserLocale();
}

// 创建 locale store
function createLocaleStore() {
  const initialLocale = getInitialLocale();
  // console.log('[i18n] Initial locale detected:', initialLocale);

  const { subscribe, set, update } = writable<Locale>(initialLocale);

  return {
    subscribe,
    set: (locale: Locale) => {
      // console.log('[i18n] Setting locale to:', locale);
      set(locale);
      // 保存到 localStorage
      if (typeof window !== 'undefined') {
        localStorage.setItem('locale-storage', JSON.stringify({ locale }));
        // console.log('[i18n] Saved to localStorage:', locale);
        // 更新 HTML lang 属性
        document.documentElement.lang = locale;
        // console.log('[i18n] Updated HTML lang attribute:', locale);
      }
    },
    update,
  };
}

export const locale = createLocaleStore();

// ============================================================
// 翻译函数
// ============================================================

/**
 * 获取嵌套对象的值
 */
function getNestedValue(obj: Record<string, unknown>, path: string): string {
  const keys = path.split('.');
  let result: unknown = obj;

  for (const key of keys) {
    if (result && typeof result === 'object' && key in result) {
      result = (result as Record<string, unknown>)[key];
    } else {
      return path; // 找不到则返回原始 key
    }
  }

  return typeof result === 'string' ? result : path;
}

/**
 * 替换变量
 */
function replaceVariables(text: string, variables?: Record<string, unknown>): string {
  if (!variables) return text;

  return text.replace(/\{(\w+)\}/g, (_, key) => {
    return variables[key] !== undefined ? String(variables[key]) : `{${key}}`;
  });
}

/**
 * 创建翻译函数
 */
function createTranslator(currentLocale: Locale) {
  const currentMessages = messages[currentLocale];

  return function t(key: string, variables?: Record<string, unknown>): string {
    const text = getNestedValue(currentMessages as unknown as Record<string, unknown>, key);
    return replaceVariables(text, variables);
  };
}

// 派生翻译函数 store
export const t = derived(locale, ($locale) => createTranslator($locale));

// 当前语言的消息
export const currentMessages = derived(locale, ($locale) => messages[$locale]);

// ============================================================
// 便捷函数
// ============================================================

/**
 * 设置语言
 */
export function setLocale(newLocale: Locale) {
  locale.set(newLocale);
}

/**
 * 获取当前语言的翻译函数（用于非响应式环境）
 */
export function getTranslator() {
  const currentLocale = get(locale);
  return createTranslator(currentLocale);
}

/**
 * 获取当前语言
 */
export function getLocale(): Locale {
  return get(locale);
}

// ============================================================
// 导出
// ============================================================

export { zhCN, enUS };
export default { locale, t, setLocale, getTranslator };
