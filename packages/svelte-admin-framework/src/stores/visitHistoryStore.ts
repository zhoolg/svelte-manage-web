/**
 * 访问历史记录 Store
 * @zhoolg/svelte-admin-framework
 */

import { writable, derived } from 'svelte/store';

interface VisitRecord {
  path: string;
  count: number;
  lastVisit: number;
}

const STORAGE_KEY = 'menu_visit_history';

function loadVisitHistory(): Record<string, VisitRecord> {
  if (typeof window === 'undefined') return {};
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : {};
  } catch {
    return {};
  }
}

function saveVisitHistory(history: Record<string, VisitRecord>) {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(history));
  } catch (error) {
    console.error('Failed to save visit history:', error);
  }
}

const visitHistoryStore = writable<Record<string, VisitRecord>>(loadVisitHistory());

export function recordVisit(path: string) {
  if (!path || path === '/') return;

  visitHistoryStore.update((history) => {
    const now = Date.now();

    if (history[path]) {
      history[path].count++;
      history[path].lastVisit = now;
    } else {
      history[path] = {
        path,
        count: 1,
        lastVisit: now,
      };
    }

    saveVisitHistory(history);
    return history;
  });
}

export const topVisitedPaths = derived(visitHistoryStore, ($history) => {
  return Object.values($history)
    .sort((a, b) => {
      if (b.count !== a.count) {
        return b.count - a.count;
      }
      return b.lastVisit - a.lastVisit;
    })
    .map((record) => record.path);
});

export function getVisitCount(path: string): number {
  let count = 0;
  visitHistoryStore.subscribe((history) => {
    count = history[path]?.count || 0;
  })();
  return count;
}

export function clearVisitHistory() {
  visitHistoryStore.set({});
  if (typeof window !== 'undefined') {
    localStorage.removeItem(STORAGE_KEY);
  }
}

export { visitHistoryStore };
