/**
 * 访问历史记录 Store
 * ============================================================
 *
 * 功能：
 * - 记录用户访问的页面路径
 * - 统计访问次数
 * - 按访问频率排序
 * - 持久化到 localStorage
 */

import { writable, derived } from 'svelte/store';

interface VisitRecord {
  path: string;
  count: number;
  lastVisit: number; // 时间戳
}

const STORAGE_KEY = 'menu_visit_history';

// 从 localStorage 加载访问历史
function loadVisitHistory(): Record<string, VisitRecord> {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : {};
  } catch {
    return {};
  }
}

// 保存访问历史到 localStorage
function saveVisitHistory(history: Record<string, VisitRecord>) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
  } catch (error) {
    console.error('Failed to save visit history:', error);
  }
}

// 创建 store
const visitHistoryStore = writable<Record<string, VisitRecord>>(loadVisitHistory());

// 记录访问
export function recordVisit(path: string) {
  if (!path || path === '/') return; // 不记录首页

  visitHistoryStore.update(history => {
    const now = Date.now();

    if (history[path]) {
      // 已存在，增加计数
      history[path].count++;
      history[path].lastVisit = now;
    } else {
      // 新记录
      history[path] = {
        path,
        count: 1,
        lastVisit: now,
      };
    }

    // 保存到 localStorage
    saveVisitHistory(history);

    return history;
  });
}

// 获取访问次数最多的路径列表
export const topVisitedPaths = derived(
  visitHistoryStore,
  $history => {
    return Object.values($history)
      .sort((a, b) => {
        // 先按访问次数降序
        if (b.count !== a.count) {
          return b.count - a.count;
        }
        // 次数相同则按最后访问时间降序
        return b.lastVisit - a.lastVisit;
      })
      .map(record => record.path);
  }
);

// 获取指定路径的访问次数
export function getVisitCount(path: string): number {
  let count = 0;
  visitHistoryStore.subscribe(history => {
    count = history[path]?.count || 0;
  })();
  return count;
}

// 清除访问历史
export function clearVisitHistory() {
  visitHistoryStore.set({});
  localStorage.removeItem(STORAGE_KEY);
}

// 导出 store（只读）
export { visitHistoryStore };
