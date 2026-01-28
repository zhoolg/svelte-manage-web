/**
 * 简单的路由状态管理
 * 使用 Svelte store 实现基本的路由功能
 */

import { writable, derived } from 'svelte/store';
import { recordVisit } from './visitHistoryStore';
import { getTranslator } from '../lib/locales';
import { getFlatModules } from '../config/app.modules';

// 当前路径
export const currentPath = writable('/');

// 路由历史
export const routeHistory = writable<string[]>(['/']);

// 导航函数
export function navigate(path: string) {
  currentPath.set(path);
  routeHistory.update(history => [...history, path]);

  // 记录访问
  recordVisit(path);
}

// 返回上一页
export function goBack() {
  routeHistory.update(history => {
    if (history.length > 1) {
      history.pop();
      const previousPath = history[history.length - 1] || '/';
      currentPath.set(previousPath);
    }
    return history;
  });
}

/**
 * 自动从模块配置生成路由名称映射
 * 这样新增模块时无需手动添加路由配置
 */
export const routeNames: Record<string, string> = (() => {
  const names: Record<string, string> = {};

  // 从所有模块配置中提取路径和标签
  getFlatModules().forEach(module => {
    if (module.path) {
      names[module.path] = module.label;
    }
  });

  return names;
})();

// 获取当前路由名称（翻译后的）
export const currentRouteName = derived(currentPath, $path => {
  const t = getTranslator();
  // 移除 query 参数和 hash，只保留路径部分
  const cleanPath = $path.split('?')[0].split('#')[0];
  const key = routeNames[cleanPath] || 'menu.home';
  return t(key);
});
