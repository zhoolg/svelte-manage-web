/**
 * 访问历史记录 Store
 * @zhoolg/svelte-admin-framework
 */
interface VisitRecord {
    path: string;
    count: number;
    lastVisit: number;
}
declare const visitHistoryStore: import("svelte/store").Writable<Record<string, VisitRecord>>;
export declare function recordVisit(path: string): void;
export declare const topVisitedPaths: import("svelte/store").Readable<string[]>;
export declare function getVisitCount(path: string): number;
export declare function clearVisitHistory(): void;
export { visitHistoryStore };
//# sourceMappingURL=visitHistoryStore.d.ts.map