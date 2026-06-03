<script lang="ts">
  /**
   * 通用 CRUD 页面组件
   * ============================================================
   *
   * 功能：
   * - 自动生成列表、搜索、新增、编辑、删除功能
   * - 支持批量选择和批量删除
   * - 支持数据导出（CSV/Excel）
   * - 支持详情查看
   * - 支持自定义操作按钮
   * - 支持权限控制
   */
  import { onMount, tick } from 'svelte';
  import { Dialog, Switch } from 'bits-ui';
  import {
    Search,
    Plus,
    Trash2,
    Download,
    Inbox,
    RefreshCw,
    ChevronLeft,
    ChevronRight,
    LayoutList,
    X,
    Loader2,
    Image,
    ArrowDownUp,
    ArrowDown,
    ArrowUp,
    Settings,
  } from 'lucide-svelte';
  import { toast } from '../utils/toast';
  import { confirm } from '../utils/confirm';
  import { get, post } from '../api/request';
  import { hasPermission, permissionStore } from '../stores/permissionStore';
  import { settingsStore } from '../stores/settingsStore';
  import {
    humanizeIdentifier,
    locale,
    resolveDynamicLabel,
    t,
    getTranslator,
  } from '../lib/locales';
  import type {
    ActionButton,
    ModuleConfig,
    TableColumn,
    SearchField,
    FormField,
  } from '../config/module';
  import FormSelect from './FormSelect.svelte';
  import MenuIconPicker from './MenuIconPicker.svelte';
  import TagInput from './TagInput.svelte';
  import DatePicker from './DatePicker.svelte';
  import DateRangePicker from './DateRangePicker.svelte';
  import { exportToXlsx } from '../utils/export';
  import {
    buildListParams,
    getActionColor,
    getActionButtonClasses,
    getCrudFormErrors,
    getExportCellValue,
    getStatusConfig,
    hasCrudActionPermission,
    renderCellValue,
    resolveCrudPermission,
    sortCrudRows,
    type CrudActionPermission,
    type SortOrder,
  } from './crud/crudUtils';

  // 获取翻译函数（用于函数调用）
  const translate = getTranslator();

  // Props
  export let config: ModuleConfig;
  export let onAction: ((action: string, row: Record<string, unknown>) => void) | undefined =
    undefined;
  export let previewMode = false;
  export let previewRows: Record<string, unknown>[] = [];
  export let previewResetKey: string | number = '';
  export const title: string | undefined = undefined;
  export const showTitle: boolean = true;

  // 状态
  let loading = false;
  let data: Record<string, unknown>[] = [];
  let total = 0;
  let currentPage = 1;
  let pageSize = config.pagination?.pageSize || 10;
  let searchForm: Record<string, unknown> = {};
  let dialogVisible = false;
  let dialogMode: 'add' | 'edit' | 'detail' = 'add';
  let formData: Record<string, unknown> = {};
  let submitting = false;
  let selectedRows: Record<string, unknown>[] = [];
  let selectedRowIds = new Set<unknown>();
  let jumpToPage = '';
  let isExporting = false;
  let mounted = false;
  let loadedListApi = '';
  let sortField = '';
  let sortOrder: SortOrder = '';
  let listError = '';
  let fieldErrors: Record<string, string> = {};
  let formContentRef: HTMLDivElement;
  let tableDensity: 'comfortable' | 'compact' = 'comfortable';
  let previousFormData: Record<string, unknown> = {};
  let previewSourceRows: Record<string, unknown>[] = [];
  let loadedPreviewResetKey = '';
  let nextPreviewId = 1000;
  const COLUMN_VISIBILITY_PREFIX = 'crud-column-visibility:';

  // 列显隐
  let columnVisibility: Record<string, boolean> = {};
  let visibleColumns: TableColumn[] = [];
  let showColumnSettings = false;

  $: totalPages = Math.ceil(total / pageSize);
  $: rowKey = config.table.rowKey || 'id';
  $: moduleName = config.name;
  $: visibleColumns =
    config.table.columns?.filter(col => columnVisibility[String(col.field)] !== false) ||
    config.table.columns;
  $: displayData = sortCrudRows(data, sortField, sortOrder);
  $: tableColumnSpan =
    visibleColumns.length +
    (config.table.actions?.length ? 1 : 0) +
    (config.table.showSelection ? 1 : 0) +
    (config.table.showIndex ? 1 : 0);
  $: selectedRows = data.filter(row => selectedRowIds.has(row[rowKey]));

  // 响应式计算分页按钮
  $: pageButtons = (() => {
    const buttons: number[] = [];
    const maxButtons = 5;

    if (totalPages <= maxButtons) {
      for (let i = 1; i <= totalPages; i++) buttons.push(i);
    } else if (currentPage <= 3) {
      for (let i = 1; i <= maxButtons; i++) buttons.push(i);
    } else if (currentPage >= totalPages - 2) {
      for (let i = totalPages - maxButtons + 1; i <= totalPages; i++) buttons.push(i);
    } else {
      for (let i = currentPage - 2; i <= currentPage + 2; i++) buttons.push(i);
    }

    return buttons;
  })();

  // 获取权限配置（优先使用 actionPermissions，否则使用模块名称）
  function getPermission(action: 'add' | 'edit' | 'delete' | 'export' | 'view'): string {
    return resolveCrudPermission(moduleName, config.actionPermissions || null, action);
  }

  function labelText(item: { label?: string; labelI18n?: unknown } | null | undefined): string {
    const fallbackSource =
      item && 'field' in item
        ? (item as { field?: unknown }).field
        : item && 'value' in item
          ? (item as { value?: unknown }).value
          : item && 'action' in item
            ? (item as { action?: unknown }).action
            : '';
    return resolveDynamicLabel(
      item?.label,
      item?.labelI18n,
      $locale,
      $t,
      humanizeIdentifier(fallbackSource)
    );
  }

  function titleText(): string {
    return resolveDynamicLabel(
      config.title,
      config.titleI18n,
      $locale,
      $t,
      humanizeIdentifier(config.name)
    );
  }

  function placeholderText(item: { placeholder?: string; placeholderI18n?: unknown }): string {
    return item.placeholder
      ? resolveDynamicLabel(item.placeholder, item.placeholderI18n, $locale, $t)
      : '';
  }

  function actionConfirmText(action: ActionButton): string {
    if (!action.confirm) {
      return '';
    }
    return resolveDynamicLabel(action.confirm, action.confirmI18n, $locale, $t);
  }

  // 权限检查
  $: canAdd = config.toolbar?.showAdd && (previewMode || $hasPermission(getPermission('add')));
  $: canEdit = previewMode || $hasPermission(getPermission('edit'));
  $: canDelete = previewMode || $hasPermission(getPermission('delete'));
  $: canExport =
    config.toolbar?.showExport && (previewMode || $hasPermission(getPermission('export')));

  // 检查操作按钮权限
  function hasActionPermission(action: CrudActionPermission): boolean {
    if (previewMode) {
      return true;
    }
    return hasCrudActionPermission(action, permissionStore);
  }

  $: isAllSelected =
    displayData.length > 0 && displayData.every(row => selectedRowIds.has(row[rowKey]));
  $: isInitialLoading = loading && data.length === 0;
  $: isRefreshing = loading && data.length > 0;
  $: tableDensity = $settingsStore.density;
  $: isCompactTable = tableDensity === 'compact';
  $: for (const field of config.form?.fields || []) {
    if (fieldErrors[field.field] && formData[field.field] !== previousFormData[field.field]) {
      clearFieldError(field.field);
    }
  }
  $: previousFormData = { ...formData };
  $: if (previewMode && String(previewResetKey) !== loadedPreviewResetKey) {
    resetPreviewRows();
  }

  function getColumnStorageKey() {
    return `${COLUMN_VISIBILITY_PREFIX}${moduleName || 'default'}`;
  }

  function initColumnVisibilityFromStorage() {
    try {
      const key = getColumnStorageKey();
      const saved = localStorage.getItem(key);
      if (saved) {
        const fields = JSON.parse(saved) as string[];
        if (Array.isArray(fields) && fields.length > 0) {
          const visibility: Record<string, boolean> = {};
          config.table.columns.forEach(col => {
            const fieldKey = String(col.field);
            visibility[fieldKey] = fields.includes(fieldKey);
          });
          columnVisibility = visibility;
          return;
        }
      }
    } catch (err) {
      console.warn('Failed to load column visibility', err);
    }
    // 默认全部可见
    const visibility: Record<string, boolean> = {};
    config.table.columns.forEach(col => {
      visibility[String(col.field)] = true;
    });
    columnVisibility = visibility;
  }

  function persistColumnVisibility() {
    try {
      const key = getColumnStorageKey();
      const fields = visibleColumns.map(col => String(col.field));
      localStorage.setItem(key, JSON.stringify(fields));
    } catch (err) {
      console.warn('Failed to save column visibility', err);
    }
  }

  function toggleColumn(fieldKey: string) {
    const current = columnVisibility[fieldKey] !== false;
    const visibleCount = Object.entries(columnVisibility).filter(([, v]) => v !== false).length;
    if (current && visibleCount <= 1) {
      toast.warning(translate('table.atLeastOneColumn') || '至少保留一列可见');
      return;
    }
    columnVisibility = {
      ...columnVisibility,
      [fieldKey]: !current,
    };
    persistColumnVisibility();
  }

  // 获取列表数据
  async function fetchData() {
    if (previewMode) {
      applyPreviewRows();
      return;
    }

    loading = true;
    listError = '';
    try {
      const params = buildListParams(currentPage, pageSize, searchForm);
      const res = await get<{ list: Record<string, unknown>[]; total: number }>(
        config.api.list,
        params
      );
      data = res.data?.list || [];
      total = res.data?.total || 0;
      pruneSelectionToCurrentRows(data);
    } catch (err) {
      listError = err instanceof Error ? err.message : translate('common.failed');
      toast.error(listError);
    } finally {
      loading = false;
    }
  }

  function resetPreviewRows() {
    loadedPreviewResetKey = String(previewResetKey);
    previewSourceRows = previewRows.map(row => ({ ...row }));
    nextPreviewId =
      previewSourceRows.reduce((maxId, row) => {
        const id = Number(row[rowKey]);
        return Number.isFinite(id) ? Math.max(maxId, id) : maxId;
      }, 999) + 1;
    currentPage = 1;
    searchForm = {};
    clearSelection();
    sortField = '';
    sortOrder = '';
    applyPreviewRows();
  }

  function applyPreviewRows() {
    loading = false;
    listError = '';
    const filtered = previewSourceRows.filter(rowMatchesPreviewSearch);
    total = filtered.length;
    const start = (currentPage - 1) * pageSize;
    data = filtered.slice(start, start + pageSize);
    pruneSelectionToCurrentRows(data);
  }

  function rowMatchesPreviewSearch(row: Record<string, unknown>) {
    return (config.search?.fields || []).every(field => {
      const value = searchForm[field.field];
      if (value === undefined || value === null || value === '') {
        return true;
      }
      const rowValue = row[field.field];
      if (field.type === 'select' || field.type === 'radio') {
        return String(rowValue ?? '') === String(value);
      }
      return String(rowValue ?? '').includes(String(value));
    });
  }

  // 监听 config 变化，当路由切换时重新获取数据
  $: if (mounted && config?.api?.list && config.api.list !== loadedListApi) {
    loadedListApi = config.api.list;
    // 重置状态
    currentPage = 1;
    pageSize = config.pagination?.pageSize || 10;
    searchForm = {};
    clearSelection();
    sortField = '';
    sortOrder = '';
    initColumnVisibilityFromStorage();
    // 重新获取数据
    fetchData();
  }

  function handleRetryFetch() {
    fetchData();
  }

  onMount(() => {
    mounted = true;
    loadedListApi = config.api.list;
    initColumnVisibilityFromStorage();
    if (previewMode) {
      resetPreviewRows();
    } else {
      fetchData();
    }
  });

  // 搜索
  function handleSearch() {
    currentPage = 1;
    fetchData();
  }

  // 重置
  function handleReset() {
    searchForm = {};
    currentPage = 1;
    fetchData();
  }

  // 导出 Excel
  async function handleExportExcel() {
    if (isExporting) return;

    const exportData = selectedRows.length > 0 ? selectedRows : data;
    if (exportData.length === 0) {
      toast.warning(translate('common.noData'));
      return;
    }

    isExporting = true;
    try {
      const columns = visibleColumns;
      const headers = columns.map(col => labelText(col));
      const rows = exportData.map(row => columns.map(col => getExportCellValue(row, col)));

      await exportToXlsx({
        filename: `${config.title}_${new Date().toISOString().slice(0, 10)}.xlsx`,
        headers,
        rows,
      });

      toast.success(translate('common.success'));
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('common.failed'));
    } finally {
      isExporting = false;
    }
  }

  // 批量删除
  async function handleBatchDelete() {
    if (selectedRows.length === 0) {
      toast.warning(translate('table.pleaseSelect'));
      return;
    }

    const confirmed = await confirm({
      title: translate('table.batchDeleteConfirmTitle'),
      content: translate('table.batchDeleteConfirmContent', { count: selectedRows.length }),
      type: 'warning',
    });
    if (!confirmed) return;

    try {
      if (previewMode) {
        const ids = new Set(selectedRows.map(row => row[rowKey]));
        previewSourceRows = previewSourceRows.filter(row => !ids.has(row[rowKey]));
        toast.success(translate('table.batchDeleteSuccess'));
        clearSelection();
        applyPreviewRows();
        return;
      }

      const ids = selectedRows.map(row => row[rowKey]);
      await post(`${config.api.delete}/batch`, { ids });
      toast.success(translate('table.batchDeleteSuccess'));
      clearSelection();
      fetchData();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('table.batchDeleteFailed'));
    }
  }

  // 选择行
  function handleSelectRow(row: Record<string, unknown>, checked: boolean) {
    const id = row[rowKey];
    const next = new Set(selectedRowIds);
    if (checked) {
      next.add(id);
    } else {
      next.delete(id);
    }
    selectedRowIds = next;
  }

  // 全选
  function handleSelectAll(checked: boolean) {
    const next = new Set(selectedRowIds);
    if (checked) {
      displayData.forEach(row => next.add(row[rowKey]));
    } else {
      displayData.forEach(row => next.delete(row[rowKey]));
    }
    selectedRowIds = next;
  }

  // 状态切换 (Switch)
  async function handleSwitchChange(
    row: Record<string, unknown>,
    col: TableColumn,
    checked: boolean
  ) {
    if (!col.switchConfig?.api) return;

    const {
      api,
      activeValue = true,
      inactiveValue = false,
      idField = 'id',
      statusField = col.field as string,
      successMessage,
      errorMessage,
    } = col.switchConfig;

    const newValue = checked ? activeValue : inactiveValue;
    const oldValue = row[col.field as string];

    // 乐观更新 (Optimistic update)
    row[col.field as string] = newValue;
    data = [...data];

    try {
      if (previewMode) {
        previewSourceRows = previewSourceRows.map(item =>
          item[rowKey] === row[rowKey] ? { ...item, [col.field as string]: newValue } : item
        );
        toast.success(successMessage || translate('common.success'));
        return;
      }

      await post(api, {
        [idField]: row[rowKey] || row[idField],
        [statusField]: newValue,
      });
      toast.success(successMessage || translate('common.success'));
    } catch (err) {
      // 失败时回滚
      row[col.field as string] = oldValue;
      data = [...data];
      toast.error(err instanceof Error ? err.message : errorMessage || translate('common.failed'));
    }
  }

  function isRowSelected(row: Record<string, unknown>) {
    return selectedRowIds.has(row[rowKey]);
  }

  function pruneSelectionToCurrentRows(rows: Record<string, unknown>[]) {
    if (selectedRowIds.size === 0) return;
    const currentIds = new Set(rows.map(row => row[rowKey]));
    selectedRowIds = new Set([...selectedRowIds].filter(id => currentIds.has(id)));
  }

  function clearSelection() {
    selectedRowIds = new Set();
  }

  function clearFieldErrors() {
    fieldErrors = {};
  }

  function clearFieldError(field: string) {
    if (!fieldErrors[field]) return;
    const { [field]: _removed, ...rest } = fieldErrors;
    fieldErrors = rest;
  }

  async function focusFirstInvalidField(field: string) {
    await tick();
    const wrapper = formContentRef?.querySelector<HTMLElement>(`[data-form-field="${field}"]`);
    wrapper?.scrollIntoView({ block: 'center', behavior: 'smooth' });
    const focusable = wrapper?.querySelector<HTMLElement>(
      'input:not([disabled]), textarea:not([disabled]), button:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );
    focusable?.focus();
  }

  function toggleTableDensity() {
    const next = $settingsStore.density === 'comfortable' ? 'compact' : 'comfortable';
    settingsStore.setDensity(next);
  }

  // 新增
  function handleAdd() {
    dialogMode = 'add';
    clearFieldErrors();
    // 设置默认值
    const defaultValues: Record<string, unknown> = {};
    config.form?.fields.forEach(field => {
      if (field.defaultValue !== undefined) {
        defaultValues[field.field] = field.defaultValue;
      }
    });
    formData = defaultValues;
    dialogVisible = true;
  }

  // 编辑
  function handleEdit(row: Record<string, unknown>) {
    dialogMode = 'edit';
    clearFieldErrors();
    formData = { ...row };
    dialogVisible = true;
  }

  // 删除
  async function handleDelete(row: Record<string, unknown>) {
    const confirmed = await confirm({
      title: translate('table.deleteConfirmTitle'),
      content: translate('table.deleteConfirmContent'),
      type: 'warning',
    });
    if (!confirmed) return;

    try {
      if (previewMode) {
        previewSourceRows = previewSourceRows.filter(item => item[rowKey] !== row[rowKey]);
        toast.success(translate('table.deleteSuccess'));
        applyPreviewRows();
        return;
      }

      await post(`${config.api.delete}/${row[rowKey]}`);
      toast.success(translate('table.deleteSuccess'));
      fetchData();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('table.deleteFailed'));
    }
  }

  // 提交表单
  async function handleSubmit() {
    const errors = config.form
      ? getCrudFormErrors(config.form.fields, formData, translate, field => labelText(field))
      : [];
    if (errors.length > 0) {
      fieldErrors = Object.fromEntries(errors.map(error => [error.field, error.message]));
      toast.warning(errors[0].message);
      focusFirstInvalidField(errors[0].field);
      return;
    }

    clearFieldErrors();
    submitting = true;
    try {
      if (previewMode) {
        if (dialogMode === 'add') {
          const id = formData[rowKey] ?? nextPreviewId++;
          previewSourceRows = [{ ...formData, [rowKey]: id }, ...previewSourceRows];
          toast.success(translate('table.addSuccess'));
        } else {
          const id = formData[rowKey];
          previewSourceRows = previewSourceRows.map(row =>
            row[rowKey] === id ? { ...row, ...formData, [rowKey]: id } : row
          );
          toast.success(translate('table.editSuccess'));
        }
        dialogVisible = false;
        clearFieldErrors();
        applyPreviewRows();
        return;
      }

      const api = dialogMode === 'add' ? config.api.add : config.api.edit;
      if (!api) {
        toast.error(translate('table.apiNotConfigured'));
        return;
      }
      await post(api, formData);
      toast.success(
        dialogMode === 'add' ? translate('table.addSuccess') : translate('table.editSuccess')
      );
      dialogVisible = false;
      clearFieldErrors();
      fetchData();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('common.failed'));
    } finally {
      submitting = false;
    }
  }

  // 操作按钮点击（带确认）
  async function handleActionClick(action: ActionButton, row: Record<string, unknown>) {
    if (action.confirm) {
      const confirmed = await confirm({
        title: translate('table.operationConfirmTitle'),
        content: actionConfirmText(action),
        type: 'warning',
      });
      if (!confirmed) return;
    }
    if (action.workflowAction) {
      await handleWorkflowAction(action.workflowAction, row);
      return;
    }
    if (action.action === 'edit') {
      handleEdit(row);
      return;
    }
    if (action.action === 'delete') {
      await handleDelete(row);
      return;
    }
    handleAction(action.label, row);
  }

  async function handleWorkflowAction(action: string, row: Record<string, unknown>) {
    const workflowBase = config.api.workflow;
    if (!workflowBase) {
      toast.error('工作流接口未配置');
      return;
    }
    try {
      if (previewMode) {
        const metadataWorkflow = config.table.actions?.find(item => item.workflowAction === action);
        const statusField = metadataWorkflow?.workflowStatusField || 'status';
        const nextValue = metadataWorkflow?.workflowTo ?? action;
        previewSourceRows = previewSourceRows.map(item =>
          item[rowKey] === row[rowKey] ? { ...item, [statusField]: nextValue } : item
        );
        toast.success('工作流流转成功');
        applyPreviewRows();
        return;
      }

      await post(`${workflowBase}/${row[rowKey]}/workflow/${action}`, {});
      toast.success('工作流流转成功');
      fetchData();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('common.failed'));
    }
  }

  // 操作按钮点击
  function handleAction(action: string, row: Record<string, unknown>) {
    const actionText = translate(action);
    if (action === 'common.edit' || actionText === translate('common.edit')) {
      handleEdit(row);
    } else if (action === 'common.delete' || actionText === translate('common.delete')) {
      handleDelete(row);
    } else if (onAction) {
      onAction(action, row);
    }
  }

  // 分页
  function setPage(page: number) {
    currentPage = page;
    clearSelection();
    fetchData();
  }

  function handlePageSizeChange(event: Event) {
    const nextPageSize = Number((event.currentTarget as HTMLSelectElement).value);
    if (!nextPageSize || nextPageSize === pageSize) return;

    pageSize = nextPageSize;
    currentPage = 1;
    clearSelection();
    fetchData();
  }

  function toggleSort(col: TableColumn) {
    if (!col.sortable) return;

    const field = String(col.field);
    if (sortField !== field) {
      sortField = field;
      sortOrder = 'asc';
      return;
    }

    if (sortOrder === 'asc') {
      sortOrder = 'desc';
    } else if (sortOrder === 'desc') {
      sortField = '';
      sortOrder = '';
    } else {
      sortOrder = 'asc';
    }
  }

  function getRowIndex(index: number): number {
    return (currentPage - 1) * pageSize + index + 1;
  }

  function isCheckboxValueSelected(field: FormField, optionValue: string | number): boolean {
    return getCheckboxValues(formData[field.field]).includes(optionValue);
  }

  function handleCheckboxFieldChange(
    field: FormField,
    optionValue: string | number,
    checked: boolean
  ) {
    if (!field.options?.length) {
      formData = {
        ...formData,
        [field.field]: checked,
      };
      return;
    }

    const values = getCheckboxValues(formData[field.field]);
    clearFieldError(field.field);
    formData = {
      ...formData,
      [field.field]: checked
        ? [...values, optionValue]
        : values.filter(value => value !== optionValue),
    };
  }

  function handleBooleanFieldChange(field: FormField, checked: boolean) {
    clearFieldError(field.field);
    formData = {
      ...formData,
      [field.field]: checked,
    };
  }

  function getCheckboxValues(value: unknown): Array<string | number> {
    if (Array.isArray(value)) {
      return value as Array<string | number>;
    }
    if (typeof value === 'string') {
      return value
        .split(',')
        .map(item => item.trim())
        .filter(Boolean);
    }
    if (value == null || value === '') {
      return [];
    }
    return [value as string | number];
  }

  function getSelectOptions(
    field: FormField | SearchField
  ): Array<{ label: string; value: string | number }> {
    if (isMenuPermissionField(field)) {
      return getMenuPermissionOptions();
    }
    return field.options || [];
  }

  function isMenuPermissionField(field: FormField | SearchField): boolean {
    return config.name === 'menus' && field.field === 'permissionCode';
  }

  function isMenuIconField(field: FormField | SearchField): boolean {
    return config.name === 'menus' && field.field === 'icon';
  }

  function getFieldTip(field: FormField): string {
    if (isMenuIconField(field)) {
      return '点击选择侧边栏图标，保存后会同步到菜单显示';
    }
    if (isMenuPermissionField(field)) {
      return '可同时选择多个权限；不勾选任何权限表示登录后可见';
    }
    return field.tip || '';
  }

  function getMenuPermissionOptions(): Array<{ label: string; value: string }> {
    const rawModule =
      stringValue(formData.moduleId) ||
      stringValue(formData.menuKey) ||
      pathToModuleKey(formData.path);
    const moduleKey = normalizePermissionModuleKey(rawModule);
    if (!moduleKey) {
      return [{ label: '不限制', value: '__none__' }];
    }

    return [
      { label: '查看', value: `${moduleKey}:view` },
      { label: '新增', value: `${moduleKey}:add` },
      { label: '编辑', value: `${moduleKey}:edit` },
      { label: '删除', value: `${moduleKey}:delete` },
      { label: '全部', value: `${moduleKey}:*` },
    ];
  }

  function normalizePermissionModuleKey(value: string): string {
    const key = value.trim().replace(/^ai-/, '');
    const aliases: Record<string, string> = {
      admins: 'admin',
      applications: 'application',
      logs: 'log',
      menus: 'menu',
      aiModules: 'settings',
      'ai-modules': 'settings',
    };
    return aliases[key] || key;
  }

  function pathToModuleKey(value: unknown): string {
    const path = stringValue(value);
    return path.startsWith('/') ? path.slice(1).split('/')[0] : path;
  }

  function stringValue(value: unknown): string {
    return value == null ? '' : String(value).trim();
  }

  // 跳转到指定页
  function handleJumpToPage() {
    const page = parseInt(jumpToPage);
    if (isNaN(page) || page < 1 || page > totalPages) {
      toast.warning(`请输入 1 到 ${totalPages} 之间的页码`);
      return;
    }
    setPage(page);
  }

  // 处理回车键跳转
  function handleJumpKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      handleJumpToPage();
    }
  }
</script>

<div class="space-y-4 fade-in">
  <!-- 页面标题区域 -->
  <div class="flex items-center justify-between gap-2">
    <div>
      <h1 class="text-lg font-semibold text-gray-800 dark:text-white">
        {titleText()}
      </h1>
      <p class="text-xs text-gray-500 mt-1">
        {$t('table.totalRecords', { total })}
      </p>
    </div>
  </div>

  <!-- 搜索区域 -->
  {#if config.search?.fields.length}
    <div class="el-card bg-white dark:bg-[#1d1d1d] rounded-lg p-4">
      <div class="flex items-center justify-between mb-3">
        <span class="text-sm font-medium text-gray-700 dark:text-gray-200">
          {$t('common.search')}
        </span>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {#each config.search.fields as field}
          <div>
            <label
              for="search-{field.field}"
              class="block text-sm text-gray-600 dark:text-gray-400 mb-1.5"
            >
              {labelText(field)}
            </label>
            {#if field.type === 'select'}
              <FormSelect
                bind:value={searchForm[field.field] as string | number}
                options={getSelectOptions(field)}
                placeholder={placeholderText(field) || $t('table.selectPlaceholder')}
              />
            {:else if field.type === 'date'}
              <DatePicker
                bind:value={searchForm[field.field] as string}
                placeholder={placeholderText(field)}
              />
            {:else if field.type === 'dateRange'}
              <DateRangePicker
                bind:startValue={searchForm[`${field.field}Start`] as string}
                bind:endValue={searchForm[`${field.field}End`] as string}
                placeholder={placeholderText(field)}
              />
            {:else}
              <input
                id="search-{field.field}"
                type="text"
                bind:value={searchForm[field.field]}
                placeholder={placeholderText(field)}
                class="w-full h-9 px-3 text-sm border-0 rounded-md bg-gray-50 dark:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-[color:color-mix(in_srgb,var(--color-primary)_30%,transparent)] hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              />
            {/if}
          </div>
        {/each}

        <div class="flex items-end gap-2">
          <button
            type="button"
            onclick={handleSearch}
            class="h-9 px-4 text-white text-sm font-medium rounded-md transition-colors flex items-center gap-1 hover:brightness-105 active:brightness-95"
            style="background-color: var(--color-primary);"
          >
            <Search size={12} />
            {$t('common.search')}
          </button>
          <button
            type="button"
            onclick={handleReset}
            class="h-9 px-4 border border-gray-200 dark:border-gray-700 text-sm rounded-md transition-colors hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
          >
            {$t('common.reset')}
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- 表格区域 -->
  <div class="el-card bg-white dark:bg-[#1d1d1d] rounded-lg">
    <!-- 工具栏 -->
    <div
      class="p-4 border-b border-gray-100 dark:border-gray-800 flex flex-wrap items-center justify-between gap-4"
    >
      <div class="flex items-center gap-4">
        {#if selectedRows.length > 0}
          <div class="flex items-center gap-2 relative">
            <span class="text-sm text-gray-500">
              {$t('table.selected', { count: selectedRows.length })}
            </span>
            {#if canDelete && config.api.delete}
              <button
                type="button"
                onclick={handleBatchDelete}
                class="h-8 px-3 text-white text-sm rounded-md transition-colors flex items-center gap-1 hover:brightness-105 active:brightness-95"
                style="background-color: var(--color-danger);"
              >
                <Trash2 size={12} />
                {$t('table.batchDelete')}
              </button>
            {/if}
            <button
              type="button"
              onclick={clearSelection}
              class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-sm rounded-md transition-colors hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
            >
              {$t('table.cancelSelect')}
            </button>
          </div>
        {:else}
          <div class="text-sm text-gray-500">
            {$t('table.totalRecords', { total })}
          </div>
        {/if}
      </div>
      <div class="flex items-center gap-2">
        <!-- 列设置 -->
        <div class="relative">
          <button
            type="button"
            onclick={() => (showColumnSettings = !showColumnSettings)}
            class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-sm rounded-md transition-colors flex items-center gap-1 hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
          >
            <Settings size={12} />
            {$t('table.columnSettings') || '列设置'}
          </button>

          {#if showColumnSettings}
            <div
              class="absolute right-0 top-full mt-2 w-56 bg-white dark:bg-[#1d1d1d] border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl z-50 p-3 space-y-2"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs font-semibold text-gray-600 dark:text-gray-300 tracking-wide">
                  {$t('table.columnSettings') || '列显示设置'}
                </span>
                <button
                  type="button"
                  class="text-xs text-gray-400 transition-colors hover:text-[color:var(--color-primary)] p-1 rounded-md hover:bg-gray-100 dark:hover:bg-gray-800"
                  onclick={() => (showColumnSettings = false)}
                >
                  <X size={14} />
                </button>
              </div>
              <div class="max-h-60 overflow-y-auto space-y-1.5 scrollbar-thin">
                {#each config.table.columns as col}
                  {@const fieldKey = String(col.field)}
                  <label
                    class="flex items-center gap-2.5 text-sm text-gray-700 dark:text-gray-200 cursor-pointer p-1 rounded hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                  >
                    <input
                      type="checkbox"
                      checked={columnVisibility[fieldKey] !== false}
                      onchange={() => toggleColumn(fieldKey)}
                      class="w-4 h-4 rounded border-gray-300 text-[color:var(--color-primary)] focus:ring-[color:var(--color-primary)] cursor-pointer"
                    />
                    <span class="truncate flex-1">{labelText(col)}</span>
                  </label>
                {/each}
              </div>
            </div>
          {/if}
        </div>
        {#if config.toolbar?.showAdd && config.api.add && canAdd}
          <button
            type="button"
            onclick={handleAdd}
            class="h-8 px-3 text-white text-sm rounded-md transition-colors flex items-center gap-1 hover:brightness-105 active:brightness-95"
            style="background-color: var(--color-primary);"
          >
            <Plus size={12} />
            {config.toolbar.addText
              ? resolveDynamicLabel(config.toolbar.addText, undefined, $locale, $t)
              : $t('common.add')}
          </button>
        {/if}
        {#if canExport}
          <button
            type="button"
            disabled={isExporting}
            onclick={handleExportExcel}
            class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-sm rounded transition-colors flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
          >
            {#if isExporting}
              <Loader2 size={12} class="animate-spin" />
              {$t('common.exporting')}
            {:else}
              <Download size={12} />
              {$t('common.export')}
            {/if}
          </button>
        {/if}
        {#if config.toolbar?.showRefresh}
          <button
            type="button"
            onclick={() => {
              clearSelection();
              fetchData();
            }}
            class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-sm rounded-md transition-colors flex items-center gap-1 hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
          >
            <RefreshCw size={12} />
            {$t('common.refresh')}
          </button>
        {/if}
      </div>
    </div>

    <!-- 表格 -->
    <div class="overflow-x-auto">
      <table class="w-full">
        <thead class="bg-gray-50 dark:bg-gray-800/50">
          <tr>
            {#if config.table.showSelection}
              <th class="w-12 px-4 {isCompactTable ? 'py-2' : 'py-3'}">
                <input
                  type="checkbox"
                  bind:checked={() => isAllSelected, checked => handleSelectAll(Boolean(checked))}
                  aria-label="选择当前页"
                  class="crud-checkbox"
                />
              </th>
            {/if}
            {#if config.table.showIndex}
              <th
                class="w-16 px-4 {isCompactTable
                  ? 'py-2'
                  : 'py-3'} text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider"
              >
                #
              </th>
            {/if}
            {#each visibleColumns as col}
              <th
                class="px-4 {isCompactTable
                  ? 'py-2'
                  : 'py-3'} text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider"
                style:width={col.width ? `${col.width}px` : undefined}
                style:min-width={col.minWidth ? `${col.minWidth}px` : undefined}
              >
                {#if col.sortable}
                  <button
                    type="button"
                    onclick={() => toggleSort(col)}
                    class="group inline-flex items-center gap-1 text-xs font-medium uppercase tracking-wider transition-colors hover:text-[color:var(--color-primary)]"
                    aria-label="按 {labelText(col)} 排序"
                  >
                    <span>{labelText(col)}</span>
                    {#if sortField === String(col.field) && sortOrder === 'asc'}
                      <ArrowUp size={12} style="color: var(--color-primary);" />
                    {:else if sortField === String(col.field) && sortOrder === 'desc'}
                      <ArrowDown size={12} style="color: var(--color-primary);" />
                    {:else}
                      <ArrowDownUp
                        size={12}
                        class="text-gray-300 group-hover:text-[color:var(--color-primary)]"
                      />
                    {/if}
                  </button>
                {:else}
                  {labelText(col)}
                {/if}
              </th>
            {/each}
            {#if config.table.actions && config.table.actions.length > 0}
              <th
                class="px-4 {isCompactTable
                  ? 'py-2'
                  : 'py-3'} text-center text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider whitespace-nowrap"
                style:width="{config.table.actionWidth || 150}px"
                style:min-width="{config.table.actionWidth || 150}px"
              >
                {$t('common.action')}
              </th>
            {/if}
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
          {#if isInitialLoading}
            {#each Array.from({ length: 5 }) as _, index}
              <tr class="animate-pulse">
                {#if config.table.showSelection}
                  <td class="px-4 {isCompactTable ? 'py-2' : 'py-3'}">
                    <div class="h-4 w-4 rounded bg-gray-200 dark:bg-gray-700"></div>
                  </td>
                {/if}
                {#if config.table.showIndex}
                  <td class="px-4 py-3">
                    <div class="h-4 w-6 rounded bg-gray-200 dark:bg-gray-700"></div>
                  </td>
                {/if}
                {#each visibleColumns as col}
                  <td class="px-4 py-3">
                    <div
                      class="h-4 rounded bg-gray-200 dark:bg-gray-700"
                      style:width={col.width
                        ? `${Math.min(Number(col.width) || 120, 140)}px`
                        : '60%'}
                    ></div>
                  </td>
                {/each}
                {#if config.table.actions && config.table.actions.length > 0}
                  <td class="px-4 py-3">
                    <div class="h-4 w-20 rounded bg-gray-200 dark:bg-gray-700"></div>
                  </td>
                {/if}
              </tr>
            {/each}
          {:else if listError}
            <tr>
              <td colspan={tableColumnSpan} class="px-4 py-12 text-center text-gray-500">
                <div class="flex flex-col items-center justify-center gap-3">
                  <div
                    class="w-16 h-16 rounded-full bg-red-50 dark:bg-red-900/20 flex items-center justify-center"
                  >
                    <X size={28} class="text-[#f56c6c]" />
                  </div>
                  <div class="space-y-1">
                    <p class="text-sm font-medium text-gray-700 dark:text-gray-200">
                      {$t('common.failed')}
                    </p>
                    <p class="max-w-md text-xs text-gray-400 break-words">{listError}</p>
                  </div>
                  <button
                    type="button"
                    onclick={handleRetryFetch}
                    class="h-8 px-3 rounded-md text-white text-sm transition-colors flex items-center gap-1 hover:brightness-105 active:brightness-95"
                    style="background-color: var(--color-primary);"
                  >
                    <RefreshCw size={12} />
                    {$t('common.retry')}
                  </button>
                </div>
              </td>
            </tr>
          {:else if displayData.length === 0}
            <tr>
              <td colspan={tableColumnSpan} class="px-4 py-8 text-center text-gray-500">
                <div class="flex flex-col items-center justify-center gap-3">
                  <div
                    class="w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center"
                  >
                    <Inbox size={32} class="text-gray-300 dark:text-gray-600" />
                  </div>
                  <span>{$t('common.noData')}</span>
                </div>
              </td>
            </tr>
          {:else}
            {#each displayData as row, index (row[rowKey] || index)}
              <tr
                class="hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-colors {isRowSelected(
                  row
                )
                  ? 'bg-blue-50/50 dark:bg-blue-900/10'
                  : ''}"
              >
                {#if config.table.showSelection}
                  <td class="px-4 py-3">
                    <input
                      type="checkbox"
                      bind:checked={
                        () => isRowSelected(row), checked => handleSelectRow(row, Boolean(checked))
                      }
                      aria-label="选择行"
                      class="crud-checkbox"
                    />
                  </td>
                {/if}
                {#if config.table.showIndex}
                  <td
                    class="px-4 {isCompactTable
                      ? 'py-2 text-xs'
                      : 'py-3 text-sm'} text-gray-500 dark:text-gray-400"
                  >
                    {getRowIndex(index)}
                  </td>
                {/if}
                {#each visibleColumns as col}
                  {@const status = getStatusConfig(col, row[col.field as string])}
                  <td
                    class="px-4 {isCompactTable
                      ? 'py-2 text-xs'
                      : 'py-3 text-sm'} text-gray-600 dark:text-gray-300"
                  >
                    {#if status}
                      <span
                        class="inline-flex items-center px-2 py-0.5 text-xs rounded whitespace-nowrap {status?.class}"
                      >
                        {resolveDynamicLabel(status?.label, status?.labelI18n, $locale, $t)}
                      </span>
                    {:else if col.format === 'switch' && col.switchConfig}
                      <div class="flex items-center whitespace-nowrap">
                        <Switch.Root
                          checked={row[col.field as string] ===
                            (col.switchConfig.activeValue ?? true)}
                          onCheckedChange={checked => handleSwitchChange(row, col, checked)}
                          class="peer inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-white disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-[color:var(--color-primary)] data-[state=unchecked]:bg-gray-200 dark:data-[state=unchecked]:bg-gray-700 dark:focus-visible:ring-offset-gray-950"
                        >
                          <Switch.Thumb
                            class="pointer-events-none block h-4 w-4 rounded-full bg-white shadow-lg ring-0 transition-transform data-[state=checked]:translate-x-4 data-[state=unchecked]:translate-x-0"
                          />
                        </Switch.Root>
                      </div>
                    {:else if col.format === 'image'}
                      {#if row[col.field as string]}
                        <div
                          class="h-10 w-10 rounded overflow-hidden border border-gray-100 dark:border-gray-700 bg-gray-50 dark:bg-gray-800"
                        >
                          <img
                            src={String(row[col.field as string])}
                            alt={labelText(col)}
                            class="h-full w-full object-cover"
                          />
                        </div>
                      {:else}
                        <div
                          class="h-10 w-10 rounded bg-gray-100 dark:bg-gray-800 flex items-center justify-center border border-gray-100 dark:border-gray-700"
                        >
                          <Image size={16} class="text-gray-400" />
                        </div>
                      {/if}
                    {:else if col.format === 'tags'}
                      {#if row[col.field as string]}
                        <div class="flex flex-wrap gap-1">
                          {#each String(row[col.field as string])
                            .split(',')
                            .filter(Boolean) as tag}
                            <span
                              class="inline-flex items-center px-2 py-0.5 text-xs rounded bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-800"
                            >
                              {tag.trim()}
                            </span>
                          {/each}
                        </div>
                      {:else}
                        -
                      {/if}
                    {:else}
                      {renderCellValue(col, row[col.field as string], row)}
                    {/if}
                  </td>
                {/each}
                {#if config.table.actions && config.table.actions.length > 0}
                  <td class="px-4 {isCompactTable ? 'py-2' : 'py-3'}">
                    <div class="flex items-center justify-center gap-[5px] whitespace-nowrap">
                      {#each config.table.actions as action}
                        {@const show =
                          typeof action.show === 'function'
                            ? action.show(row)
                            : action.show !== false}
                        {@const hasPermission = hasActionPermission(action)}
                        {#if show && hasPermission}
                          <button
                            type="button"
                            onclick={() => handleActionClick(action, row)}
                            class="inline-flex h-[26px] min-w-[50px] shrink-0 items-center justify-center rounded-[5px] border px-2 text-xs font-normal leading-none transition-colors duration-150 {getActionButtonClasses(
                              action.type
                            )}"
                          >
                            {labelText(action)}
                          </button>
                        {/if}
                      {/each}
                    </div>
                  </td>
                {/if}
              </tr>
            {/each}
          {/if}
        </tbody>
      </table>
      {#if isRefreshing}
        <div
          class="pointer-events-none absolute right-4 top-4 rounded-full border border-gray-200 bg-white/90 px-3 py-1.5 text-xs text-gray-500 shadow-sm dark:border-gray-700 dark:bg-[#1d1d1d]/90"
        >
          <div class="flex items-center gap-2">
            <Loader2 size={12} class="animate-spin" style="color: var(--color-primary);" />
            <span>{$t('common.loading')}</span>
          </div>
        </div>
      {/if}
    </div>

    <!-- 分页 -->
    {#if totalPages > 0}
      <div
        class="p-4 border-t border-gray-100 dark:border-gray-800 flex flex-wrap items-center justify-between gap-4"
      >
        <div class="text-sm text-gray-500">
          {$t('table.showRange', {
            start: (currentPage - 1) * pageSize + 1,
            end: Math.min(currentPage * pageSize, total),
            total,
          })}
        </div>
        <div class="flex flex-wrap items-center gap-2">
          {#if config.pagination?.pageSizes?.length}
            <select
              value={pageSize}
              onchange={handlePageSizeChange}
              class="h-8 rounded-md border border-gray-200 bg-white px-2 text-sm text-gray-600 transition-colors hover:border-[color:var(--color-primary)] focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300"
              aria-label="每页条数"
            >
              {#each config.pagination.pageSizes as size}
                <option value={size}>{size} / 页</option>
              {/each}
            </select>
          {/if}
          <button
            type="button"
            onclick={() => setPage(Math.max(1, currentPage - 1))}
            disabled={currentPage === 1}
            class="w-8 h-8 flex items-center justify-center rounded-md border border-gray-200 dark:border-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
            aria-label="上一页"
          >
            <ChevronLeft size={14} />
          </button>
          {#each pageButtons as page}
            <button
              type="button"
              onclick={() => setPage(page)}
              class="w-8 h-8 flex items-center justify-center rounded-md text-sm transition-all duration-200 {currentPage ===
              page
                ? 'text-white shadow-sm bg-[color:var(--color-primary)]'
                : 'border border-gray-200 dark:border-gray-700 hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)]'}"
            >
              {page}
            </button>
          {/each}
          <button
            type="button"
            onclick={() => setPage(Math.min(totalPages, currentPage + 1))}
            disabled={currentPage === totalPages}
            class="w-8 h-8 flex items-center justify-center rounded-md border border-gray-200 dark:border-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200 hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
            aria-label="下一页"
          >
            <ChevronRight size={14} />
          </button>
          <span class="ml-2 text-sm text-gray-500">{$t('pagination.jumpTo')}</span>
          <input
            type="number"
            bind:value={jumpToPage}
            onkeydown={handleJumpKeydown}
            min="1"
            max={totalPages}
            class="w-12 h-8 px-2 text-sm text-center border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus:outline-none focus:border-[color:var(--color-primary)] [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
            placeholder=""
          />
          <span class="text-sm text-gray-500"
            >{$t('pagination.pageTotal', { total: totalPages })}</span
          >
        </div>
      </div>
    {/if}
  </div>
</div>

<!-- 表单弹窗 -->
{#if config.form}
  <Dialog.Root bind:open={dialogVisible}>
    <Dialog.Portal>
      <Dialog.Overlay class="fixed inset-0 z-50 bg-black/50" />
      <Dialog.Content
        class="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-xl flex flex-col"
        style="width: {config.form.width || 500}px; max-height: 90vh"
      >
        <div
          class="px-6 py-4 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between flex-shrink-0"
        >
          <Dialog.Title class="text-lg font-medium">
            {dialogMode === 'add'
              ? $t('table.addTitle', { title: titleText() })
              : $t('table.editTitle', { title: titleText() })}
          </Dialog.Title>
          <Dialog.Close class="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={16} />
          </Dialog.Close>
        </div>
        <div bind:this={formContentRef} class="p-6 space-y-4 overflow-y-auto flex-1">
          {#each config.form.fields as field}
            {@const fieldError = fieldErrors[field.field]}
            <div data-form-field={field.field}>
              <label
                for="form-{field.field}"
                class="block text-sm text-gray-600 dark:text-gray-400 mb-1.5"
              >
                {#if field.required}
                  <span class="text-red-500 mr-1">*</span>
                {/if}
                {labelText(field)}
              </label>
              {#if field.type === 'textarea'}
                <textarea
                  id="form-{field.field}"
                  bind:value={formData[field.field]}
                  oninput={() => clearFieldError(field.field)}
                  placeholder={placeholderText(field)}
                  rows={field.rows || 3}
                  maxlength={field.maxLength}
                  disabled={field.disabled}
                  aria-invalid={fieldError ? 'true' : 'false'}
                  aria-describedby={fieldError ? `form-${field.field}-error` : undefined}
                  class="w-full px-3 py-2 text-sm border {fieldError
                    ? 'border-[#f56c6c] focus:border-[#f56c6c] focus:ring-[#f56c6c]/20'
                    : 'border-gray-200 dark:border-gray-700 focus:border-[color:var(--color-primary)] focus:ring-[color:color-mix(in_srgb,var(--color-primary)_20%,transparent)]'} rounded-md bg-white dark:bg-gray-800 focus:outline-none focus:ring-1 resize-none disabled:opacity-50 disabled:cursor-not-allowed"
                ></textarea>
              {:else if field.type === 'number'}
                <input
                  id="form-{field.field}"
                  type="number"
                  bind:value={formData[field.field]}
                  oninput={() => clearFieldError(field.field)}
                  placeholder={placeholderText(field)}
                  disabled={field.disabled}
                  aria-invalid={fieldError ? 'true' : 'false'}
                  aria-describedby={fieldError ? `form-${field.field}-error` : undefined}
                  class="w-full h-9 px-3 text-sm border {fieldError
                    ? 'border-[#f56c6c] focus:border-[#f56c6c] focus:ring-[#f56c6c]/20'
                    : 'border-gray-200 dark:border-gray-700 focus:border-[color:var(--color-primary)] focus:ring-[color:color-mix(in_srgb,var(--color-primary)_20%,transparent)]'} rounded-md bg-white dark:bg-gray-800 focus:outline-none focus:ring-1 disabled:opacity-50 disabled:cursor-not-allowed"
                />
              {:else if isMenuIconField(field)}
                <MenuIconPicker
                  bind:value={formData[field.field] as string}
                  disabled={field.disabled}
                />
              {:else if field.type === 'select' && !isMenuPermissionField(field)}
                <FormSelect
                  bind:value={formData[field.field] as string | number}
                  options={getSelectOptions(field)}
                  placeholder={placeholderText(field) || $t('table.selectPlaceholder')}
                  disabled={field.disabled}
                />
              {:else if field.type === 'radio'}
                <div class="flex min-h-9 flex-wrap items-center gap-4">
                  {#each field.options || [] as option}
                    <label
                      class="inline-flex items-center gap-2 text-sm text-gray-600 dark:text-gray-300"
                    >
                      <input
                        type="radio"
                        name="form-{field.field}"
                        value={option.value}
                        checked={formData[field.field] === option.value}
                        disabled={field.disabled}
                        onchange={() => {
                          formData = { ...formData, [field.field]: option.value };
                          clearFieldError(field.field);
                        }}
                        class="h-4 w-4 accent-[color:var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-50"
                      />
                      <span>{labelText(option)}</span>
                    </label>
                  {/each}
                </div>
              {:else if field.type === 'checkbox' || isMenuPermissionField(field)}
                {@const checkboxOptions = getSelectOptions(field)}
                {#if checkboxOptions.length}
                  <div class="flex min-h-9 flex-wrap items-center gap-4">
                    {#each checkboxOptions as option}
                      <label
                        class="inline-flex items-center gap-2 text-sm text-gray-600 dark:text-gray-300"
                      >
                        <input
                          type="checkbox"
                          checked={isCheckboxValueSelected(field, option.value)}
                          disabled={field.disabled}
                          onchange={event =>
                            handleCheckboxFieldChange(
                              field,
                              option.value,
                              event.currentTarget.checked
                            )}
                          class="h-4 w-4 rounded accent-[color:var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-50"
                        />
                        <span>{labelText(option)}</span>
                      </label>
                    {/each}
                  </div>
                {:else if config.name === 'menus' && field.field === 'permissionCode'}
                  <div class="flex min-h-9 items-center text-sm text-gray-400">
                    请先填写页面模块，系统会自动生成查看 / 新增 / 编辑 / 删除权限
                  </div>
                {:else}
                  <label
                    class="inline-flex min-h-9 items-center gap-2 text-sm text-gray-600 dark:text-gray-300"
                  >
                    <input
                      type="checkbox"
                      checked={Boolean(formData[field.field])}
                      disabled={field.disabled}
                      onchange={event =>
                        handleBooleanFieldChange(field, event.currentTarget.checked)}
                      class="h-4 w-4 rounded accent-[color:var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-50"
                    />
                    <span>{labelText(field)}</span>
                  </label>
                {/if}
              {:else if field.type === 'switch'}
                <div class="flex min-h-9 items-center">
                  <Switch.Root
                    checked={Boolean(formData[field.field])}
                    disabled={field.disabled}
                    onCheckedChange={checked => {
                      clearFieldError(field.field);
                      formData = { ...formData, [field.field]: checked };
                    }}
                    class="peer inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--color-primary)] focus-visible:ring-offset-2 focus-visible:ring-offset-white disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-[color:var(--color-primary)] data-[state=unchecked]:bg-gray-200 dark:data-[state=unchecked]:bg-gray-700 dark:focus-visible:ring-offset-gray-950"
                  >
                    <Switch.Thumb
                      class="pointer-events-none block h-4 w-4 rounded-full bg-white shadow-lg ring-0 transition-transform data-[state=checked]:translate-x-4 data-[state=unchecked]:translate-x-0"
                    />
                  </Switch.Root>
                </div>
              {:else if field.type === 'tags'}
                <TagInput
                  bind:value={formData[field.field] as string}
                  placeholder={placeholderText(field) || '输入后按回车或逗号添加标签'}
                  disabled={field.disabled}
                />
              {:else if field.type === 'date'}
                <DatePicker
                  bind:value={formData[field.field] as string}
                  placeholder={placeholderText(field)}
                  disabled={field.disabled}
                />
              {:else if field.type === 'datetime'}
                <DatePicker
                  bind:value={formData[field.field] as string}
                  placeholder={placeholderText(field)}
                  disabled={field.disabled}
                />
              {:else}
                <input
                  id="form-{field.field}"
                  type="text"
                  bind:value={formData[field.field]}
                  oninput={() => clearFieldError(field.field)}
                  placeholder={placeholderText(field)}
                  maxlength={field.maxLength}
                  disabled={field.disabled}
                  aria-invalid={fieldError ? 'true' : 'false'}
                  aria-describedby={fieldError ? `form-${field.field}-error` : undefined}
                  class="w-full h-9 px-3 text-sm border {fieldError
                    ? 'border-[#f56c6c] focus:border-[#f56c6c] focus:ring-[#f56c6c]/20'
                    : 'border-gray-200 dark:border-gray-700 focus:border-[color:var(--color-primary)] focus:ring-[color:color-mix(in_srgb,var(--color-primary)_20%,transparent)]'} rounded-md bg-white dark:bg-gray-800 focus:outline-none focus:ring-1 disabled:opacity-50 disabled:cursor-not-allowed"
                />
              {/if}
              {#if fieldError}
                <p id="form-{field.field}-error" class="mt-1 text-xs text-[#f56c6c]">
                  {fieldError}
                </p>
              {/if}
              {#if getFieldTip(field)}
                <p class="mt-1 text-xs text-gray-400">
                  {resolveDynamicLabel(getFieldTip(field), field.tipI18n, $locale, $t)}
                </p>
              {/if}
            </div>
          {/each}
        </div>
        <div
          class="px-6 py-4 border-t border-gray-100 dark:border-gray-800 flex justify-end gap-2 flex-shrink-0"
        >
          <Dialog.Close
            class="h-9 px-4 border border-gray-200 dark:border-gray-700 text-sm rounded transition-colors hover:text-[color:var(--color-primary)] hover:border-[color:var(--color-primary)]"
          >
            {$t('common.cancel')}
          </Dialog.Close>
          <button
            onclick={handleSubmit}
            disabled={submitting}
            class="h-9 px-4 text-white text-sm rounded transition-colors disabled:opacity-50 hover:brightness-105 active:brightness-95"
            style="background-color: var(--color-primary);"
          >
            {submitting ? $t('table.submitting') : $t('common.confirm')}
          </button>
        </div>
      </Dialog.Content>
    </Dialog.Portal>
  </Dialog.Root>
{/if}

<style>
  .crud-checkbox {
    position: relative;
    width: 16px;
    height: 16px;
    margin: 0;
    display: inline-block;
    vertical-align: middle;
    cursor: pointer;
    appearance: none;
    -webkit-appearance: none;
    border: 1px solid #cbd5e1;
    border-radius: 5px;
    background: #fff;
    transition:
      background-color 0.15s ease,
      border-color 0.15s ease,
      box-shadow 0.15s ease;
  }

  .crud-checkbox:hover {
    border-color: var(--color-primary);
    background: #f8fbff;
  }

  .crud-checkbox:focus-visible {
    outline: none;
    box-shadow: 0 0 0 2px color-mix(in srgb, var(--color-primary) 24%, transparent);
  }

  .crud-checkbox:checked {
    border-color: var(--color-primary);
    background: #eef6ff;
    box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--color-primary) 6%, transparent);
  }

  .crud-checkbox:checked::after {
    content: '';
    position: absolute;
    left: 5px;
    top: 2px;
    width: 5px;
    height: 9px;
    border: solid var(--color-primary);
    border-width: 0 2px 2px 0;
    transform: rotate(45deg);
  }

  :global(.dark) .crud-checkbox {
    border-color: #475569;
    background: #111827;
  }

  :global(.dark) .crud-checkbox:checked {
    border-color: var(--color-primary);
    background: color-mix(in srgb, var(--color-primary) 14%, #111827);
  }
</style>
