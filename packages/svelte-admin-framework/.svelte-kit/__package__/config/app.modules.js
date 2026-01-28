/**
 * 应用模块配置 - 控制反转模式
 * ============================================================
 *
 * 🚀 使用方式：
 * 1. 在业务项目中使用 import.meta.glob 加载模块配置
 * 2. 调用 createModuleRegistry 初始化模块系统
 *
 * @example
 * // 在业务项目的 main.ts 或 App.svelte 中
 * import { createModuleRegistry } from '@zhoolg/svelte-admin-framework/config';
 * import { MENU_STRUCTURE } from './config/menu-structure.config';
 *
 * const modules = import.meta.glob('./config/modules/*.config.ts', { eager: true });
 * const { appModules, getModuleByPath, getFlatModules } = createModuleRegistry(modules, MENU_STRUCTURE);
 *
 * @zhoolg/svelte-admin-framework
 */
/**
 * 创建模块注册表
 * 采用控制反转模式，由业务项目传入 glob 结果
 *
 * @param globResult - import.meta.glob 的结果
 * @param menuStructure - 菜单结构配置
 * @returns 模块注册表
 */
export function createModuleRegistry(globResult, menuStructure) {
    // 提取所有模块配置
    const allModules = Object.values(globResult).map((module) => module.default);
    // 创建模块映射表（通过 id 快速查找）
    const moduleMap = new Map();
    allModules.forEach((module) => {
        moduleMap.set(module.id, module);
    });
    // 构建菜单树
    function buildMenuTree() {
        const result = [];
        menuStructure.forEach((item) => {
            const module = moduleMap.get(item.id);
            if (!module) {
                console.warn(`[Config] Module "${item.id}" not found in modules/`);
                return;
            }
            // 如果有子菜单，递归构建
            if (item.children && item.children.length > 0) {
                const children = [];
                item.children.forEach((childId) => {
                    const childModule = moduleMap.get(childId);
                    if (childModule) {
                        children.push(childModule);
                    }
                    else {
                        console.warn(`[Config] Child module "${childId}" not found in modules/`);
                    }
                });
                // 将子菜单添加到父模块
                result.push({
                    ...module,
                    children,
                });
            }
            else {
                result.push(module);
            }
        });
        return result;
    }
    const appModules = buildMenuTree();
    // 获取扁平化的模块列表
    function getFlatModules(modules = appModules) {
        const result = [];
        function traverse(items) {
            items.forEach((item) => {
                if (item.path && item.path !== '/') {
                    result.push(item);
                }
                if (item.children) {
                    traverse(item.children);
                }
            });
        }
        traverse(modules);
        return result;
    }
    // 根据路径获取模块
    function getModuleByPath(path) {
        // 移除 query 参数和 hash，只保留路径部分
        const cleanPath = path.split('?')[0].split('#')[0];
        return getFlatModules().find((m) => m.path === cleanPath);
    }
    // 根据 ID 获取模块
    function getModuleById(id) {
        return getFlatModules().find((m) => m.id === id);
    }
    // 获取所有 CRUD 模块
    function getCrudModules() {
        return getFlatModules().filter((m) => m.crud);
    }
    // 转换为旧的 ModuleConfig 格式（兼容现有 CrudPage）
    function toModuleConfig(module) {
        if (!module.crud)
            return null;
        const { crud } = module;
        // 生成 API 配置
        const api = crud.api || {
            list: `${crud.apiBase}/list`,
            add: `${crud.apiBase}/add`,
            edit: `${crud.apiBase}/update`,
            delete: `${crud.apiBase}/delete`,
        };
        return {
            name: module.id,
            title: crud.title,
            api: {
                list: api.list || `${crud.apiBase}/list`,
                add: api.add,
                edit: api.edit,
                delete: api.delete,
            },
            table: {
                columns: crud.columns,
                rowKey: 'id',
                showSelection: crud.showSelection ?? false,
                actions: crud.actions,
                actionWidth: crud.actionWidth ?? 150,
            },
            search: crud.search ? { fields: crud.search } : undefined,
            form: crud.form ? { fields: crud.form, width: 600 } : undefined,
            toolbar: {
                showAdd: crud.showAdd ?? true,
                showExport: crud.showExport ?? false,
                showRefresh: true,
            },
        };
    }
    return {
        appModules,
        getFlatModules,
        getModuleByPath,
        getModuleById,
        getCrudModules,
        toModuleConfig,
    };
}
/**
 * 转换为菜单配置（兼容现有菜单）
 */
export function toMenuConfig(modules) {
    return modules.map((module) => ({
        path: module.path,
        label: module.label,
        icon: module.icon,
        hidden: module.hidden,
        children: module.children ? toMenuConfig(module.children) : undefined,
    }));
}
