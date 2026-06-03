/**
 * 应用模块配置
 * ============================================================
 *
 * 运行时模块只来自后端 /admin/meta/modules。
 * 前端不再维护本地模块清单，也不提供本地菜单或 CRUD 兜底。
 */

import { writable } from 'svelte/store';
import type { ModuleConfig, TableColumn, SearchField, FormField } from './module';
import type { LocaleTextMap } from '../lib/locales';
import { AUTH_SESSION_EXPIRED_CODE, ApiError, get as apiGet } from '../api/request';

// 运行时模块只来自后端 /admin/meta/modules。
const moduleMap = new Map<string, AppModule>();

// ==================== 配置接口 ====================

export interface AppModule<T = Record<string, unknown>> {
  /** 模块唯一标识 */
  id: string;
  /** 菜单名称 */
  label: string;
  /** 多语言菜单名称 */
  labelI18n?: LocaleTextMap;
  /** 菜单图标 (PrimeIcons) */
  icon: string;
  /** 路由路径 */
  path: string;
  /** 是否隐藏菜单 */
  hidden?: boolean;

  /** 子菜单 */
  children?: AppModule[];

  /** CRUD 配置（如果是 CRUD 页面） */
  crud?: CrudConfig<T>;

  /** 自定义页面组件标识 */
  customPage?: string;

  /** 权限控制 */
  /** 允许访问的角色列表 */
  roles?: string[];
  /** 允许访问的权限列表 */
  permissions?: string[];
  /** 模块来源 */
  source?: 'local' | 'ai-applied' | string;
  /** 来源任务编号 */
  taskNo?: string;
}

export interface CrudConfig<T = Record<string, unknown>> {
  /** 页面标题 */
  title: string;
  /** 多语言页面标题 */
  titleI18n?: LocaleTextMap;

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

  /** 是否显示序号列 */
  showIndex?: boolean;

  /** 操作按钮 */
  actions?: Array<{
    /** 内置动作标识 */
    action?: 'edit' | 'delete' | string;
    label: string;
    labelI18n?: LocaleTextMap;
    type?: 'primary' | 'success' | 'warning' | 'danger';
    icon?: string;
    show?: boolean | ((row: T) => boolean);
    confirm?: string;
    /** 操作权限 */
    permission?: string;
    /** 操作角色 */
    role?: string;
    /** 工作流动作标识 */
    workflowAction?: string;
  }>;

  /** 工作流流转配置 */
  workflow?: Array<{
    action: string;
    label?: string;
    labelI18n?: LocaleTextMap;
    from: string | number;
    to: string | number;
    statusField?: string;
    permission?: string;
    confirm?: string;
    confirmI18n?: LocaleTextMap;
    type?: 'primary' | 'success' | 'warning' | 'danger';
  }>;

  /** 操作列宽度（默认 150） */
  actionWidth?: number;

  /** 分页配置 */
  pagination?: ModuleConfig['pagination'];

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

// ==================== 应用配置 ====================

/**
 * 后端元数据模块缓存。
 */
export const APP_MODULES: AppModule[] = [];
export const modulesVersion = writable(0);

function replaceAppModules(modules: AppModule[]) {
  moduleMap.clear();
  modules.forEach(module => {
    moduleMap.set(module.id, module);
  });
  APP_MODULES.splice(0, APP_MODULES.length, ...modules);
  modulesVersion.update(version => version + 1);
}

export function clearRemoteModules() {
  replaceAppModules([]);
}

export async function loadRemoteModules() {
  try {
    const result = await apiGet<AppModule[]>('/admin/meta/modules');
    const remoteModules = Array.isArray(result?.data) ? (result.data as AppModule[]) : [];
    const normalized = remoteModules.filter(module => Boolean(module?.id && module.path));
    replaceAppModules(normalized);
    console.log(
      '[Config] Remote modules loaded:',
      normalized.map(module => module.id)
    );
    return true;
  } catch (error) {
    clearRemoteModules();
    if (!(error instanceof ApiError && error.code === AUTH_SESSION_EXPIRED_CODE)) {
      console.warn('[Config] Failed to load remote modules, local modules are disabled:', error);
    }
    return false;
  }
}

// ==================== 辅助函数 ====================

/**
 * 获取扁平化的模块列表
 */
export function getFlatModules(modules: AppModule[] = APP_MODULES): AppModule[] {
  const result: AppModule[] = [];

  function traverse(items: AppModule[]) {
    items.forEach(item => {
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

/**
 * 根据路径获取模块
 * 支持带 query 参数的路径匹配
 */
export function getModuleByPath(path: string): AppModule | undefined {
  // 移除 query 参数和 hash，只保留路径部分
  const cleanPath = path.split('?')[0].split('#')[0];
  return getFlatModules().find(m => m.path === cleanPath);
}

/**
 * 根据 ID 获取模块
 */
export function getModuleById(id: string): AppModule | undefined {
  return getFlatModules().find(m => m.id === id);
}

/**
 * 获取所有 CRUD 模块
 */
export function getCrudModules(): AppModule[] {
  return getFlatModules().filter(m => m.crud);
}

/**
 * 转换为旧的 ModuleConfig 格式（兼容现有 CrudPage）
 */
export function toModuleConfig(module: AppModule): ModuleConfig | null {
  if (!module.crud) return null;

  const { crud } = module;

  // 生成 API 配置
  const api = crud.api || {
    list: `${crud.apiBase}/list`,
    add: `${crud.apiBase}/add`,
    edit: `${crud.apiBase}/update`,
    delete: `${crud.apiBase}/delete`,
  };
  const workflowActions = (crud.workflow || []).map(transition => ({
    label: transition.label || transition.action,
    labelI18n: transition.labelI18n,
    type: transition.type || 'primary',
    icon: 'workflow',
    permission: transition.permission,
    confirm: transition.confirm,
    confirmI18n: transition.confirmI18n,
    workflowAction: transition.action,
    workflowStatusField: transition.statusField || 'status',
    workflowTo: transition.to,
    show: (row: Record<string, unknown>) =>
      String(row[transition.statusField || 'status']) === String(transition.from),
  }));
  const defaultActions = crud.actions?.length ? [] : defaultCrudActions(crud, api);
  const tableActions = [...defaultActions, ...(crud.actions || []), ...workflowActions];

  return {
    name: module.id,
    title: crud.title,
    titleI18n: crud.titleI18n,
    api: {
      list: api.list || `${crud.apiBase}/list`,
      add: api.add,
      edit: api.edit,
      delete: api.delete,
      workflow: `${crud.apiBase.replace(/\/$/, '')}`,
    },
    table: {
      columns: crud.columns,
      rowKey: 'id',
      showIndex: crud.showIndex ?? false,
      showSelection: crud.showSelection ?? false,
      actions: tableActions,
      actionWidth: crud.actionWidth ?? (tableActions.length > 2 ? 168 : 120),
    },
    search: crud.search ? { fields: crud.search } : undefined,
    form: crud.form ? { fields: crud.form, width: 600 } : undefined,
    toolbar: {
      showAdd: crud.showAdd ?? true,
      showExport: crud.showExport ?? false,
      showRefresh: true,
    },
    pagination: {
      pageSize: crud.pagination?.pageSize ?? 10,
      pageSizes: crud.pagination?.pageSizes ?? [10, 20, 50, 100],
    },
    actionPermissions: crud.actionPermissions,
  };
}

function defaultCrudActions(
  crud: CrudConfig,
  api: { list?: string; add?: string; edit?: string; delete?: string }
): NonNullable<CrudConfig['actions']> {
  const actions: NonNullable<CrudConfig['actions']> = [];
  if (api.edit && crud.form?.length) {
    actions.push({
      action: 'edit',
      label: 'common.edit',
      permission: crud.actionPermissions?.edit,
    });
  }
  if (api.delete && crud.showSelection !== false) {
    actions.push({
      action: 'delete',
      label: 'common.delete',
      type: 'danger',
      permission: crud.actionPermissions?.delete,
    });
  }
  return actions;
}

/**
 * 转换为菜单配置（兼容现有菜单）
 */
export function toMenuConfig(modules: AppModule[] = APP_MODULES): Array<{
  path?: string;
  label: string;
  labelI18n?: LocaleTextMap;
  icon: string;
  hidden?: boolean;
  permissions?: string[];
  roles?: string[];
  children?: ReturnType<typeof toMenuConfig>;
}> {
  return modules.map(module => ({
    path: module.path,
    label: module.label,
    labelI18n: module.labelI18n,
    icon: module.icon,
    hidden: module.hidden,
    permissions: module.permissions,
    roles: module.roles,
    children: module.children ? toMenuConfig(module.children) : undefined,
  }));
}
