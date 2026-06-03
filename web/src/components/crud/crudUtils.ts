import type { ActionButton, FormField, FormRule, TableColumn } from '../../config/module';
import type { LocaleTextMap } from '../../lib/locales';

type PermissionChecker = {
  hasPermission(permission: string): boolean;
  hasRole(roleCode: string): boolean;
};

export type StatusView = {
  label: string;
  labelI18n?: LocaleTextMap;
  class: string;
};

export type CrudActionPermission = Pick<ActionButton, 'permission' | 'role'>;
export type SortOrder = 'asc' | 'desc' | '';

/**
 * 构建列表查询参数，统一过滤空值，避免组件内散落参数清理逻辑。
 */
export function buildListParams(
  pageNum: number,
  pageSize: number,
  searchForm: Record<string, unknown>
): Record<string, unknown> {
  const params: Record<string, unknown> = {
    pageNum,
    pageSize,
    ...searchForm,
  };

  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== '' && value !== undefined && value !== null
    )
  );
}

function isEmptyValue(value: unknown): boolean {
  return (
    value === undefined ||
    value === null ||
    value === '' ||
    (Array.isArray(value) && value.length === 0)
  );
}

function getComparableValue(value: unknown): string | number {
  if (value === undefined || value === null) {
    return '';
  }

  if (typeof value === 'number') {
    return value;
  }

  if (value instanceof Date) {
    return value.getTime();
  }

  const text = String(value);
  const numericValue = Number(text);
  if (text.trim() !== '' && !Number.isNaN(numericValue)) {
    return numericValue;
  }

  const dateValue = Date.parse(text);
  if (!Number.isNaN(dateValue) && /\d{4}[-/]\d{1,2}[-/]\d{1,2}/.test(text)) {
    return dateValue;
  }

  return text.toLocaleLowerCase();
}

/**
 * 对当前页数据做稳定排序，避免修改接口返回的原始数组。
 */
export function sortCrudRows(
  rows: Record<string, unknown>[],
  sortField: string,
  sortOrder: SortOrder
): Record<string, unknown>[] {
  if (!sortField || !sortOrder) {
    return rows;
  }

  return [...rows].sort((left, right) => {
    const leftValue = getComparableValue(left[sortField]);
    const rightValue = getComparableValue(right[sortField]);

    if (leftValue === rightValue) return 0;
    if (leftValue > rightValue) return sortOrder === 'asc' ? 1 : -1;
    return sortOrder === 'asc' ? -1 : 1;
  });
}

function validateRule(value: unknown, rule: FormRule): string | null {
  if (rule.required && isEmptyValue(value)) {
    return rule.message;
  }

  if (isEmptyValue(value)) {
    return null;
  }

  const text = String(value);
  if (rule.min !== undefined && text.length < rule.min) {
    return rule.message;
  }

  if (rule.max !== undefined && text.length > rule.max) {
    return rule.message;
  }

  if (rule.pattern && !rule.pattern.test(text)) {
    return rule.message;
  }

  if (rule.validator) {
    const result = rule.validator(value);
    if (result === false) {
      return rule.message;
    }
    if (typeof result === 'string') {
      return result;
    }
  }

  return null;
}

/**
 * 校验 CRUD 表单字段，统一处理 required 与 rules，返回首个错误提示。
 */
export function getCrudFormError(
  fields: FormField[],
  formData: Record<string, unknown>,
  translateField: (value: string, params?: Record<string, unknown>) => string
): string | null {
  for (const field of fields) {
    const value = formData[field.field];

    if (field.required && isEmptyValue(value)) {
      return translateField('table.pleaseFill', { field: translateField(field.label) });
    }

    for (const rule of field.rules || []) {
      const error = validateRule(value, rule);
      if (error) {
        return translateField(error);
      }
    }
  }

  return null;
}

export function getCrudFormErrors(
  fields: FormField[],
  formData: Record<string, unknown>,
  translateField: (value: string, params?: Record<string, unknown>) => string,
  resolveFieldLabel: (field: FormField) => string = field => translateField(field.label)
): Array<{ field: string; message: string }> {
  const errors: Array<{ field: string; message: string }> = [];

  for (const field of fields) {
    const value = formData[field.field];

    if (field.required && isEmptyValue(value)) {
      errors.push({
        field: field.field,
        message: translateField('table.pleaseFill', { field: resolveFieldLabel(field) }),
      });
      continue;
    }

    for (const rule of field.rules || []) {
      const error = validateRule(value, rule);
      if (error) {
        errors.push({
          field: field.field,
          message: translateField(error),
        });
        break;
      }
    }
  }

  return errors;
}

/**
 * 根据模块名和动作生成权限码，支持模块配置中的显式覆盖。
 */
export function resolveCrudPermission(
  moduleName: string,
  actionPermissions: Partial<Record<'add' | 'edit' | 'delete' | 'export' | 'view', string>> | null,
  action: 'add' | 'edit' | 'delete' | 'export' | 'view'
): string {
  return actionPermissions?.[action] || `${moduleName}:${action}`;
}

/**
 * 检查自定义操作按钮权限，保持按钮渲染逻辑可复用和可测试。
 */
export function hasCrudActionPermission(
  action: CrudActionPermission,
  permissions: PermissionChecker
): boolean {
  if (action.permission) {
    return permissions.hasPermission(action.permission);
  }

  if (action.role) {
    return permissions.hasRole(action.role);
  }

  return true;
}

export function getActionColor(type?: string): string {
  // 保留纯文本颜色的映射以兼容旧用法（目前主要由新按钮样式替代）
  switch (type) {
    case 'success':
      return 'text-[#67c23a]';
    case 'warning':
      return 'text-[#e6a23c]';
    case 'danger':
      return 'text-[#f56c6c]';
    case 'primary':
    default:
      return 'text-[color:var(--color-primary)]';
  }
}

// 统一 CRUD 操作列按钮样式，对齐 Element 风格的矩形操作按钮。
export function getActionButtonClasses(type?: string): string {
  switch (type) {
    case 'primary':
      return 'border-[#409eff] bg-[#409eff] text-white hover:border-[#66b1ff] hover:bg-[#66b1ff] active:border-[#3a8ee6] active:bg-[#3a8ee6]';
    case 'success':
      return 'border-[#67c23a] bg-[#67c23a] text-white hover:border-[#85ce61] hover:bg-[#85ce61] active:border-[#5daf34] active:bg-[#5daf34]';
    case 'warning':
      return 'border-[#e6a23c] bg-[#e6a23c] text-white hover:border-[#ebb563] hover:bg-[#ebb563] active:border-[#cf9236] active:bg-[#cf9236]';
    case 'danger':
      return 'border-[#f56c6c] bg-[#f56c6c] text-white hover:border-[#f78989] hover:bg-[#f78989] active:border-[#dd6161] active:bg-[#dd6161]';
    default:
      return 'border-[#409eff] bg-white text-[#409eff] hover:border-[#66b1ff] hover:bg-[#ecf5ff] hover:text-[#66b1ff] active:border-[#3a8ee6] active:text-[#3a8ee6] dark:bg-transparent dark:hover:bg-[#182f46]';
  }
}

export function renderCellValue(
  col: TableColumn,
  value: unknown,
  _row: Record<string, unknown>
): string {
  if (col.format === 'datetime') {
    if (!value) return '-';

    try {
      const date = new Date(String(value));
      return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
    } catch {
      return String(value);
    }
  }

  return value != null ? String(value) : '-';
}

function toObjectText(value: unknown): string {
  if (typeof value !== 'object' || value === null) {
    return String(value);
  }

  const record = value as Record<string, unknown>;
  return String(record.label || record.name || record.url || '');
}

export function getExportCellValue(row: Record<string, unknown>, col: TableColumn): string {
  const rawValue = row[col.field as string];

  if (col.format === 'status' && col.statusMap) {
    const status = col.statusMap[String(rawValue)];
    return status?.label || status?.text || String(rawValue ?? '');
  }

  if (col.format === 'switch' && col.switchConfig) {
    const activeValue = col.switchConfig.activeValue ?? true;
    const activeText = col.switchConfig.activeText || '开启';
    const inactiveText = col.switchConfig.inactiveText || '关闭';
    return rawValue === activeValue ? activeText : inactiveText;
  }

  if (col.format === 'image') {
    if (Array.isArray(rawValue)) {
      return rawValue.map(toObjectText).filter(Boolean).join(', ');
    }
    return rawValue != null ? toObjectText(rawValue) : '';
  }

  if (col.format === 'tags') {
    if (Array.isArray(rawValue)) {
      return rawValue.map(toObjectText).filter(Boolean).join(', ');
    }

    if (typeof rawValue === 'string') {
      return rawValue
        .split(',')
        .map(tag => tag.trim())
        .filter(Boolean)
        .join(', ');
    }

    return String(rawValue ?? '');
  }

  if (col.format === 'datetime') {
    return renderCellValue(col, rawValue, row);
  }

  return rawValue != null ? String(rawValue) : '';
}

export function getStatusConfig(col: TableColumn, value: unknown): StatusView | null {
  if (col.format !== 'status' || !col.statusMap) {
    return null;
  }

  const status = col.statusMap[value as string | number];
  if (!status) {
    return null;
  }

  const colorClass: Record<string, string> = {
    success: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
    warning: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
    danger: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
    info: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
  };

  return {
    label: status.label,
    labelI18n: status.labelI18n,
    class: colorClass[status.color] || 'bg-gray-100 text-gray-800',
  };
}
