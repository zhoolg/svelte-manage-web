/**
 * 模块配置类型定义
 * 用于配置 CRUD 页面的表格列、搜索字段、表单字段等
 */

import type { SvelteComponent } from 'svelte';

// ==================== 表格列配置 ====================

export interface TableColumn<T = Record<string, unknown>> {
  /** 字段名 */
  field: keyof T | string;
  /** 列标题 */
  label: string;
  /** 列宽度 */
  width?: number | string;
  /** 最小宽度 */
  minWidth?: number;
  /** 对齐方式 */
  align?: 'left' | 'center' | 'right';
  /** 是否可排序 */
  sortable?: boolean;
  /** 是否固定列 */
  fixed?: 'left' | 'right';
  /** 自定义渲染组件 */
  render?: typeof SvelteComponent;
  /** 格式化类型 */
  format?: 'date' | 'datetime' | 'money' | 'percent' | 'status' | 'switch' | 'image' | 'tags';
  /** 状态映射（当 format 为 status 时使用） */
  statusMap?: Record<string | number, { label: string; color: string }>;
  /** Switch 配置（当 format 为 switch 时使用） */
  switchConfig?: {
    /** 状态切换 API */
    api: string;
    /** 激活值 */
    activeValue?: unknown;
    /** 非激活值 */
    inactiveValue?: unknown;
    /** ID 字段名 */
    idField?: string;
    /** 状态字段名 */
    statusField?: string;
    /** 成功消息 */
    successMessage?: string;
    /** 错误消息 */
    errorMessage?: string;
  };
}

// ==================== 搜索字段配置 ====================

export type SearchFieldType = 'input' | 'select' | 'date' | 'dateRange' | 'number' | 'radio';

export interface SearchField {
  /** 字段名 */
  field: string;
  /** 标签 */
  label: string;
  /** 字段类型 */
  type: SearchFieldType;
  /** 占位符 */
  placeholder?: string;
  /** 选项（select/radio 类型使用） */
  options?: Array<{ label: string; value: string | number }>;
  /** 默认值 */
  defaultValue?: unknown;
  /** 栅格占比 (1-4) */
  span?: number;
}

// ==================== 表单字段配置 ====================

export type FormFieldType =
  | 'input'
  | 'textarea'
  | 'select'
  | 'radio'
  | 'checkbox'
  | 'switch'
  | 'number'
  | 'date'
  | 'datetime'
  | 'upload'
  | 'editor'
  | 'tags';

export interface FormField {
  /** 字段名 */
  field: string;
  /** 标签 */
  label: string;
  /** 字段类型 */
  type: FormFieldType;
  /** 占位符 */
  placeholder?: string;
  /** 是否必填 */
  required?: boolean;
  /** 选项（select/radio/checkbox 类型使用） */
  options?: Array<{ label: string; value: string | number }>;
  /** 默认值 */
  defaultValue?: unknown;
  /** 验证规则 */
  rules?: FormRule[];
  /** 栅格占比 (1-2) */
  span?: number;
  /** 是否禁用 */
  disabled?: boolean;
  /** 提示信息 */
  tip?: string;
  /** 最大长度 */
  maxLength?: number;
  /** textarea 行数 */
  rows?: number;
}

export interface FormRule {
  /** 是否必填 */
  required?: boolean;
  /** 错误提示 */
  message: string;
  /** 最小长度 */
  min?: number;
  /** 最大长度 */
  max?: number;
  /** 正则表达式 */
  pattern?: RegExp;
  /** 自定义验证函数 */
  validator?: (value: unknown) => boolean | string;
}

// ==================== 操作按钮配置 ====================

export interface ActionButton<T = Record<string, unknown>> {
  /** 按钮文字 */
  label: string;
  /** 按钮类型 */
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  /** 图标 */
  icon?: string;
  /** 点击事件 */
  onClick?: (row: T) => void;
  /** 是否显示 */
  show?: boolean | ((row: T) => boolean);
  /** 确认提示 */
  confirm?: string;
}

// ==================== 模块配置 ====================

export interface ModuleConfig<T = Record<string, unknown>> {
  /** 模块名称 */
  name: string;
  /** 模块标题 */
  title: string;

  // API 配置
  api: {
    /** 列表接口 */
    list: string;
    /** 新增接口 */
    add?: string;
    /** 编辑接口 */
    edit?: string;
    /** 删除接口 */
    delete?: string;
    /** 详情接口 */
    detail?: string;
  };

  // 表格配置
  table: {
    /** 表格列 */
    columns: TableColumn<T>[];
    /** 主键字段 */
    rowKey?: string;
    /** 是否显示序号列 */
    showIndex?: boolean;
    /** 是否显示选择列 */
    showSelection?: boolean;
    /** 操作列按钮 */
    actions?: ActionButton<T>[];
    /** 操作列宽度 */
    actionWidth?: number;
  };

  // 搜索配置
  search?: {
    /** 搜索字段 */
    fields: SearchField[];
    /** 是否展开 */
    expand?: boolean;
  };

  // 表单配置（新增/编辑）
  form?: {
    /** 表单字段 */
    fields: FormField[];
    /** 表单宽度 */
    width?: number | string;
    /** 标签宽度 */
    labelWidth?: number | string;
  };

  // 工具栏配置
  toolbar?: {
    /** 是否显示新增按钮 */
    showAdd?: boolean;
    /** 新增按钮文字 */
    addText?: string;
    /** 是否显示导出按钮 */
    showExport?: boolean;
    /** 是否显示刷新按钮 */
    showRefresh?: boolean;
    /** 自定义按钮 */
    buttons?: Array<{
      label: string;
      icon?: string;
      type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
      onClick?: () => void;
    }>;
  };

  // 分页配置
  pagination?: {
    /** 每页条数 */
    pageSize?: number;
    /** 每页条数选项 */
    pageSizes?: number[];
  };
}

// ==================== 辅助函数 ====================

/**
 * 创建模块配置（带类型推断）
 */
export function defineModule<T = Record<string, unknown>>(
  config: ModuleConfig<T>
): ModuleConfig<T> {
  return {
    ...config,
    table: {
      rowKey: 'id',
      showIndex: false,
      showSelection: false,
      actionWidth: 150,
      ...config.table,
    },
    toolbar: {
      showAdd: true,
      addText: '新增',
      showRefresh: true,
      ...config.toolbar,
    },
    pagination: {
      pageSize: 10,
      pageSizes: [10, 20, 50, 100],
      ...config.pagination,
    },
  };
}
