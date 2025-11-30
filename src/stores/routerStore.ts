/**
 * 简单的路由状态管理
 * 使用 Svelte store 实现基本的路由功能
 */

import { writable, derived } from 'svelte/store';
import { recordVisit } from './visitHistoryStore';
import { getTranslator } from '../lib/locales';

// 当前路径
export const currentPath = writable('/');

// 路由历史
export const routeHistory = writable<string[]>(['/']);

// 导航函数
export function navigate(path: string) {
  currentPath.set(path);
  routeHistory.update((history) => [...history, path]);

  // 记录访问
  recordVisit(path);
}

// 返回上一页
export function goBack() {
  routeHistory.update((history) => {
    if (history.length > 1) {
      history.pop();
      const previousPath = history[history.length - 1] || '/';
      currentPath.set(previousPath);
    }
    return history;
  });
}

// 根据路径获取路由名称（使用翻译键）
export const routeNames: Record<string, string> = {
  '/': 'menu.home',
  '/users': 'menu.users',
  '/agents': 'menu.agents',
  '/faq': 'menu.faq',
  '/logs': 'menu.logs',
  '/dict': 'menu.dict',
  '/settings': 'menu.settings',
  '/profile': 'menu.profile',
  '/articles': 'menu.articles',
};

// 获取当前路由名称（翻译后的）
export const currentRouteName = derived(currentPath, ($path) => {
  const t = getTranslator();
  const key = routeNames[$path] || 'menu.home';
  return t(key);
});
