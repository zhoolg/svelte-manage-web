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

import type { ModuleConfig, TableColumn, SearchField, FormField, ActionButton } from './module';

// ==================== 配置接口 ====================

export interface AppModule<T = Record<string, unknown>> {
  /** 模块唯一标识 */
  id: string;
  /** 菜单名称 */
  label: string;
  /** 菜单图标 (Lucide 图标名称) */
  icon: string;
  /** 路由路径 */
  path: string;
  /** 是否隐藏菜单 */
  hidden?: boolean;

  /** 子菜单 */
  children?: AppModule[];

  /** CRUD 配置（如果是 CRUD 页面） */
  crud?: CrudConfig<T>;

  /** 自定义页面组件路径（相对于 src/pages/） */
  customPage?: string;

  /** 权限控制 */
  /** 允许访问的角色列表 */
  roles?: string[];
  /** 允许访问的权限列表 */
  permissions?: string[];
}

export interface CrudConfig<T = Record<string, unknown>> {
  /** 页面标题 */
  title: string;

  /** API 基础路径（自动生成 list/add/edit/delete） */
  apiBase: string;

  /** 或者自定义 API 路径 */
  api?: {
    list?: string;
    add?: string;
    edit?: string;
    delete?: string;
  };

  /** 表格列配置 */
  columns: TableColumn<T>[];

  /** 搜索字段配置 */
  search?: SearchField[];

  /** 表单字段配置 */
  form?: FormField[];

  /** 是否显示新增按钮 */
  showAdd?: boolean;

  /** 是否显示导出按钮 */
  showExport?: boolean;

  /** 是否显示复选框 */
  showSelection?: boolean;

  /** 操作按钮 */
  actions?: ActionButton<T>[];

  /** 操作列宽度（默认 150） */
  actionWidth?: number;

  /** 操作权限配置 */
  actionPermissions?: {
    /** 添加权限 */
    add?: string;
    /** 编辑权限 */
    edit?: string;
    /** 删除权限 */
    delete?: string;
    /** 导出权限 */
    export?: string;
    /** 查看权限 */
    view?: string;
  };
}

// ==================== 菜单结构配置 ====================

export interface MenuStructure {
  /** 模块 ID */
  id: string;
  /** 子菜单 ID 列表 */
  children?: string[];
}

// ==================== 模块注册表 ====================

export interface ModuleRegistry {
  /** 应用模块列表（菜单树） */
  appModules: AppModule[];
  /** 获取扁平化的模块列表 */
  getFlatModules: () => AppModule[];
  /** 根据路径获取模块 */
  getModuleByPath: (path: string) => AppModule | undefined;
  /** 根据 ID 获取模块 */
  getModuleById: (id: string) => AppModule | undefined;
  /** 获取所有 CRUD 模块 */
  getCrudModules: () => AppModule[];
  /** 转换为旧的 ModuleConfig 格式 */
  toModuleConfig: (module: AppModule) => ModuleConfig | null;
}

/**
 * 创建模块注册表
 * 采用控制反转模式，由业务项目传入 glob 结果
 *
 * @param globResult - import.meta.glob 的结果
 * @param menuStructure - 菜单结构配置
 * @returns 模块注册表
 */
export function createModuleRegistry(
  globResult: Record<string, { default: AppModule }>,
  menuStructure: MenuStructure[]
): ModuleRegistry {
  // 提取所有模块配置
  const allModules = Object.values(globResult).map((module) => module.default);

  // 创建模块映射表（通过 id 快速查找）
  const moduleMap = new Map<string, AppModule>();
  allModules.forEach((module) => {
    moduleMap.set(module.id, module);
  });

  // 构建菜单树
  function buildMenuTree(): AppModule[] {
    const result: AppModule[] = [];

    menuStructure.forEach((item) => {
      const module = moduleMap.get(item.id);
      if (!module) {
        console.warn(`[Config] Module "${item.id}" not found in modules/`);
        return;
      }

      // 如果有子菜单，递归构建
      if (item.children && item.children.length > 0) {
        const children: AppModule[] = [];
        item.children.forEach((childId) => {
          const childModule = moduleMap.get(childId);
          if (childModule) {
            children.push(childModule);
          } else {
            console.warn(`[Config] Child module "${childId}" not found in modules/`);
          }
        });

        // 将子菜单添加到父模块
        result.push({
          ...module,
          children,
        });
      } else {
        result.push(module);
      }
    });

    return result;
  }

  const appModules = buildMenuTree();

  // 获取扁平化的模块列表
  function getFlatModules(modules: AppModule[] = appModules): AppModule[] {
    const result: AppModule[] = [];

    function traverse(items: AppModule[]) {
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
  function getModuleByPath(path: string): AppModule | undefined {
    // 移除 query 参数和 hash，只保留路径部分
    const cleanPath = path.split('?')[0].split('#')[0];
    return getFlatModules().find((m) => m.path === cleanPath);
  }

  // 根据 ID 获取模块
  function getModuleById(id: string): AppModule | undefined {
    return getFlatModules().find((m) => m.id === id);
  }

  // 获取所有 CRUD 模块
  function getCrudModules(): AppModule[] {
    return getFlatModules().filter((m) => m.crud);
  }

  // 转换为旧的 ModuleConfig 格式（兼容现有 CrudPage）
  function toModuleConfig(module: AppModule): ModuleConfig | null {
    if (!module.crud) return null;

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
export function toMenuConfig(
  modules: AppModule[]
): Array<{
  path?: string;
  label: string;
  icon: string;
  hidden?: boolean;
  children?: ReturnType<typeof toMenuConfig>;
}> {
  return modules.map((module) => ({
    path: module.path,
    label: module.label,
    icon: module.icon,
    hidden: module.hidden,
    children: module.children ? toMenuConfig(module.children) : undefined,
  }));
}
