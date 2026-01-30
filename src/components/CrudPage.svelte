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
  import { onMount } from 'svelte';
  import { Dialog, Button, Checkbox, Switch } from 'bits-ui';
  import {
    Search,
    Plus,
    Trash2,
    Download,
    RefreshCw,
    ChevronLeft,
    ChevronRight,
    X,
    Loader2,
    Image,
  } from 'lucide-svelte';
  import { toast } from '../utils/toast';
  import { confirm } from '../utils/confirm';
  import { get, post } from '../api/request';
  import { hasPermission, permissionStore } from '../stores/permissionStore';
  import { t, getTranslator } from '../lib/locales';
  import type { ModuleConfig, TableColumn, SearchField, FormField } from '../config/module';
  import FormSelect from './FormSelect.svelte';
  import TagInput from './TagInput.svelte';
  import DatePicker from './DatePicker.svelte';
  import DateRangePicker from './DateRangePicker.svelte';
  import Icon from './Icon.svelte';
  import { exportToXlsx } from '../utils/export';

  // 获取翻译函数（用于函数调用）
  const translate = getTranslator();

  // Props
  export let config: ModuleConfig;
  export let onAction: ((action: string, row: Record<string, unknown>) => void) | undefined =
    undefined;
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
  let jumpToPage = '';
  let isExporting = false;

  $: totalPages = Math.ceil(total / pageSize);
  $: rowKey = config.table.rowKey || 'id';
  $: moduleName = config.name;

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
    // 从 config 获取 actionPermissions（如果存在）
    const actionPerms = (config as any).actionPermissions;
    if (actionPerms && actionPerms[action]) {
      return actionPerms[action];
    }
    // 否则使用默认的模块名称格式
    return `${moduleName}:${action}`;
  }

  // 权限检查
  $: canAdd = config.toolbar?.showAdd && $hasPermission(getPermission('add'));
  $: canEdit = $hasPermission(getPermission('edit'));
  $: canDelete = $hasPermission(getPermission('delete'));
  $: canExport = config.toolbar?.showExport && $hasPermission(getPermission('export'));

  // 检查操作按钮权限
  function hasActionPermission(action: any): boolean {
    // 如果操作配置了 permission，检查该权限
    if (action.permission) {
      return permissionStore.hasPermission(action.permission);
    }
    // 如果操作配置了 role，检查该角色
    if (action.role) {
      return permissionStore.hasRole(action.role);
    }
    // 没有配置权限，默认有权限
    return true;
  }

  $: isAllSelected = data.length > 0 && selectedRows.length === data.length;

  // 获取列表数据
  async function fetchData() {
    loading = true;
    try {
      const params: Record<string, unknown> = {
        pageNum: currentPage,
        pageSize,
        ...searchForm,
      };
      // 过滤空值
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === undefined) {
          delete params[key];
        }
      });

      const res = await get<{ list: Record<string, unknown>[]; total: number }>(
        config.api.list,
        params
      );
      data = res.data?.list || [];
      total = res.data?.total || 0;
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('common.failed'));
    } finally {
      loading = false;
    }
  }

  // 监听 config 变化，当路由切换时重新获取数据
  $: if (config?.api?.list) {
    // 重置状态
    currentPage = 1;
    searchForm = {};
    selectedRows = [];
    // 重新获取数据
    fetchData();
  }

  onMount(() => {
    fetchData();
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

  // 导出 CSV
  function handleExportCsv() {
    const exportData = selectedRows.length > 0 ? selectedRows : data;
    if (exportData.length === 0) {
      toast.warning(translate('common.noData'));
      return;
    }

    const columns = config.table.columns;
    const headers = columns.map(col => col.label);
    const rows = exportData.map(row =>
      columns.map(col => {
        const value = row[col.field as string];
        return typeof value === 'string' ? `"${value.replace(/"/g, '""')}"` : String(value ?? '');
      })
    );

    const csv = [headers.join(','), ...rows.map(row => row.join(','))].join('\n');
    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${config.title}_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    toast.success(translate('common.success'));
  }

  // 导出 Excel
  // 获取导出用的单元格值（处理各种格式）
  function getExportCellValue(row: Record<string, unknown>, col: TableColumn): string {
    const rawValue = row[col.field as string];

    // status：使用 statusMap 的文案
    if (col.format === 'status' && col.statusMap) {
      const status = col.statusMap[String(rawValue)];
      return status?.label || status?.text || String(rawValue ?? '');
    }

    // switch：导出为"开启/关闭"或自定义文案
    if (col.format === 'switch' && col.switchConfig) {
      const activeValue = col.switchConfig.activeValue ?? true;
      const inactiveValue = col.switchConfig.inactiveValue ?? false;
      const activeText = col.switchConfig.activeText || '开启';
      const inactiveText = col.switchConfig.inactiveText || '关闭';
      return rawValue === activeValue ? activeText : inactiveText;
    }

    // image：导出为 URL 文本，支持对象数组
    if (col.format === 'image') {
      if (Array.isArray(rawValue)) {
        return rawValue
          .map(v => (typeof v === 'object' && v !== null ? (v as any).url || '' : String(v)))
          .filter(Boolean)
          .join(', ');
      }
      if (typeof rawValue === 'object' && rawValue !== null) {
        return (rawValue as any).url || '';
      }
      return String(rawValue ?? '');
    }

    // tags：导出为逗号分隔的文本，支持对象数组
    if (col.format === 'tags') {
      if (Array.isArray(rawValue)) {
        return rawValue
          .map(v =>
            typeof v === 'object' && v !== null
              ? (v as any).label || (v as any).name || String(v)
              : String(v)
          )
          .filter(Boolean)
          .join(', ');
      }
      if (typeof rawValue === 'string') {
        return rawValue
          .split(',')
          .map(t => t.trim())
          .filter(Boolean)
          .join(', ');
      }
      return String(rawValue ?? '');
    }

    // datetime：使用 renderCellValue 的格式化逻辑
    if (col.format === 'datetime') {
      return renderCellValue(col, rawValue, row);
    }

    // 默认：原值或空字符串
    return rawValue != null ? String(rawValue) : '';
  }

  async function handleExportExcel() {
    if (isExporting) return;

    const exportData = selectedRows.length > 0 ? selectedRows : data;
    if (exportData.length === 0) {
      toast.warning(translate('common.noData'));
      return;
    }

    isExporting = true;
    try {
      const columns = config.table.columns;
      const headers = columns.map(col => col.label);
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
      const ids = selectedRows.map(row => row[rowKey]);
      await post(`${config.api.delete}/batch`, { ids });
      toast.success(translate('table.batchDeleteSuccess'));
      selectedRows = [];
      fetchData();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('table.batchDeleteFailed'));
    }
  }

  // 选择行
  function handleSelectRow(row: Record<string, unknown>, checked: boolean) {
    if (checked) {
      selectedRows = [...selectedRows, row];
    } else {
      selectedRows = selectedRows.filter(r => r[rowKey] !== row[rowKey]);
    }
  }

  // 全选
  function handleSelectAll(checked: boolean) {
    if (checked) {
      selectedRows = [...data];
    } else {
      selectedRows = [];
    }
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
    return selectedRows.some(r => r[rowKey] === row[rowKey]);
  }

  // 新增
  function handleAdd() {
    dialogMode = 'add';
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
      await get(`${config.api.delete}/${row[rowKey]}`);
      toast.success(translate('table.deleteSuccess'));
      fetchData();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('table.deleteFailed'));
    }
  }

  // 提交表单
  async function handleSubmit() {
    // 验证必填字段
    const requiredFields = config.form?.fields.filter(f => f.required) || [];
    for (const field of requiredFields) {
      if (!formData[field.field]) {
        toast.warning(translate('table.pleaseFill', { field: field.label }));
        return;
      }
    }

    submitting = true;
    try {
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
      fetchData();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : translate('common.failed'));
    } finally {
      submitting = false;
    }
  }

  // 操作按钮点击（带确认）
  async function handleActionClick(
    action: { label: string; confirm?: string },
    row: Record<string, unknown>
  ) {
    if (action.confirm) {
      const confirmed = await confirm({
        title: translate('table.operationConfirmTitle'),
        content: action.confirm,
        type: 'warning',
      });
      if (!confirmed) return;
    }
    handleAction(action.label, row);
  }

  // 操作按钮点击
  function handleAction(action: string, row: Record<string, unknown>) {
    if (action === '编辑' || action === 'Edit') {
      handleEdit(row);
    } else if (action === '删除' || action === 'Delete') {
      handleDelete(row);
    } else if (onAction) {
      onAction(action, row);
    }
  }

  // 获取操作按钮颜色
  function getActionColor(type?: string) {
    switch (type) {
      case 'primary':
        return 'text-[#409eff]';
      case 'success':
        return 'text-[#67c23a]';
      case 'warning':
        return 'text-[#e6a23c]';
      case 'danger':
        return 'text-[#f56c6c]';
      default:
        return 'text-[#409eff]';
    }
  }

  // 渲染单元格内容
  function renderCellValue(col: TableColumn, value: unknown, row: Record<string, unknown>): string {
    if (col.format === 'datetime') {
      if (!value) return '-';
      try {
        const date = new Date(String(value));
        return isNaN(date.getTime()) ? String(value) : date.toLocaleString();
      } catch (e) {
        return String(value);
      }
    }
    return value != null ? String(value) : '-';
  }

  // 获取状态配置
  function getStatusConfig(col: TableColumn, value: unknown) {
    if (col.format === 'status' && col.statusMap) {
      const status = col.statusMap[value as string | number];
      if (status) {
        const colorClass: Record<string, string> = {
          success: 'bg-green-100 text-green-800',
          warning: 'bg-yellow-100 text-yellow-800',
          danger: 'bg-red-100 text-red-800',
          info: 'bg-blue-100 text-blue-800',
        };
        return {
          label: status.label,
          class: colorClass[status.color] || 'bg-gray-100 text-gray-800',
        };
      }
    }
    return null;
  }

  // 分页
  function setPage(page: number) {
    currentPage = page;
    fetchData();
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
  <!-- 搜索区域 -->
  {#if config.search?.fields.length}
    <div class="el-card bg-white dark:bg-[#1d1d1d] rounded-lg p-5">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {#each config.search.fields as field}
          <div>
            <label
              for="search-{field.field}"
              class="block text-sm text-gray-600 dark:text-gray-400 mb-1.5"
            >
              {$t(field.label)}
            </label>
            {#if field.type === 'select'}
              <FormSelect
                bind:value={searchForm[field.field] as string | number}
                options={field.options || []}
                placeholder={field.placeholder
                  ? $t(field.placeholder)
                  : $t('table.selectPlaceholder')}
              />
            {:else if field.type === 'date'}
              <DatePicker
                bind:value={searchForm[field.field] as string}
                placeholder={field.placeholder ? $t(field.placeholder) : ''}
              />
            {:else if field.type === 'dateRange'}
              <DateRangePicker
                bind:startValue={searchForm[`${field.field}Start`] as string}
                bind:endValue={searchForm[`${field.field}End`] as string}
                placeholder={field.placeholder ? $t(field.placeholder) : ''}
              />
            {:else}
              <input
                id="search-{field.field}"
                type="text"
                bind:value={searchForm[field.field]}
                placeholder={field.placeholder ? $t(field.placeholder) : ''}
                class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus:outline-none focus:border-[#409eff]"
              />
            {/if}
          </div>
        {/each}
        <div class="flex items-end gap-2">
          <Button.Root
            onclick={handleSearch}
            class="h-9 px-4 bg-[#409eff] hover:bg-[#66b1ff] text-white text-sm rounded transition-colors flex items-center gap-1"
          >
            <Search size={12} />
            {$t('common.search')}
          </Button.Root>
          <Button.Root
            onclick={handleReset}
            class="h-9 px-4 border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff] text-sm rounded transition-colors"
          >
            {$t('common.reset')}
          </Button.Root>
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
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">
              {$t('table.selected', { count: selectedRows.length })}
            </span>
            {#if canDelete && config.api.delete}
              <Button.Root
                onclick={handleBatchDelete}
                class="h-8 px-3 bg-[#f56c6c] hover:bg-[#f78989] text-white text-sm rounded transition-colors flex items-center gap-1"
              >
                <Trash2 size={12} />
                {$t('table.batchDelete')}
              </Button.Root>
            {/if}
            <Button.Root
              onclick={() => (selectedRows = [])}
              class="h-8 px-3 border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff] text-sm rounded transition-colors"
            >
              {$t('table.cancelSelect')}
            </Button.Root>
          </div>
        {:else}
          <div class="text-sm text-gray-500">
            {$t('table.totalRecords', { total })}
          </div>
        {/if}
      </div>
      <div class="flex items-center gap-2">
        {#if config.toolbar?.showAdd && config.api.add && canAdd}
          <Button.Root
            onclick={handleAdd}
            class="h-8 px-3 bg-[#409eff] hover:bg-[#66b1ff] text-white text-sm rounded transition-colors flex items-center gap-1"
          >
            <Plus size={12} />
            {config.toolbar.addText || $t('common.add')}
          </Button.Root>
        {/if}
        {#if canExport}
          <div class="relative group">
            <Button.Root
              disabled={isExporting}
              class="h-8 px-3 border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff] text-sm rounded transition-colors flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {#if isExporting}
                <Loader2 size={12} class="animate-spin" />
                {$t('common.exporting')}
              {:else}
                <Download size={12} />
                {$t('common.export')}
              {/if}
            </Button.Root>
            {#if !isExporting}
              <div
                class="absolute right-0 top-full mt-1 w-32 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-1 hidden group-hover:block z-10"
              >
                <Button.Root
                  onclick={handleExportCsv}
                  class="w-full px-4 py-2 text-left text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800"
                >
                  {$t('table.exportCsv')}
                </Button.Root>
                <Button.Root
                  onclick={handleExportExcel}
                  class="w-full px-4 py-2 text-left text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800"
                >
                  {$t('table.exportExcel')}
                </Button.Root>
              </div>
            {/if}
          </div>
        {/if}
        {#if config.toolbar?.showRefresh}
          <Button.Root
            onclick={() => {
              selectedRows = [];
              fetchData();
            }}
            class="h-8 px-3 border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff] text-sm rounded transition-colors flex items-center gap-1"
          >
            <RefreshCw size={12} />
            {$t('common.refresh')}
          </Button.Root>
        {/if}
      </div>
    </div>

    <!-- 表格 -->
    <div class="overflow-x-auto">
      <table class="w-full">
        <thead class="bg-gray-50 dark:bg-gray-800/50">
          <tr>
            {#if config.table.showSelection}
              <th class="w-12 px-4 py-3">
                <Checkbox.Root
                  checked={isAllSelected}
                  onCheckedChange={checked => handleSelectAll(!!checked)}
                  class="w-4 h-4 text-[#409eff] border-gray-300 rounded focus:ring-[#409eff]"
                />
              </th>
            {/if}
            {#each config.table.columns as col}
              <th
                class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider"
                style:width={col.width ? `${col.width}px` : undefined}
                style:min-width={col.minWidth ? `${col.minWidth}px` : undefined}
              >
                {$t(col.label)}
              </th>
            {/each}
            {#if config.table.actions && config.table.actions.length > 0}
              <th
                class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider"
                style:width="{config.table.actionWidth || 150}px"
              >
                {$t('common.action')}
              </th>
            {/if}
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
          {#if loading}
            <tr>
              <td
                colspan={config.table.columns.length +
                  (config.table.actions?.length ? 1 : 0) +
                  (config.table.showSelection ? 1 : 0)}
                class="px-4 py-8 text-center text-gray-500"
              >
                <Loader2 size={16} class="inline-block mr-2 animate-spin" />
                {$t('common.loading')}
              </td>
            </tr>
          {:else if data.length === 0}
            <tr>
              <td
                colspan={config.table.columns.length +
                  (config.table.actions?.length ? 1 : 0) +
                  (config.table.showSelection ? 1 : 0)}
                class="px-4 py-8 text-center text-gray-500"
              >
                {$t('common.noData')}
              </td>
            </tr>
          {:else}
            {#each data as row, index (row[rowKey] || index)}
              <tr
                class="hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-colors {isRowSelected(
                  row
                )
                  ? 'bg-blue-50/50 dark:bg-blue-900/10'
                  : ''}"
              >
                {#if config.table.showSelection}
                  <td class="px-4 py-3">
                    <Checkbox.Root
                      checked={isRowSelected(row)}
                      onCheckedChange={checked => handleSelectRow(row, !!checked)}
                      class="w-4 h-4 text-[#409eff] border-gray-300 rounded focus:ring-[#409eff]"
                    />
                  </td>
                {/if}
                {#each config.table.columns as col}
                  <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-300">
                    {#if getStatusConfig(col, row[col.field as string])}
                      {@const status = getStatusConfig(col, row[col.field as string])}
                      <span class="px-2 py-1 text-xs rounded {status?.class}">
                        {status?.label}
                      </span>
                    {:else if col.format === 'switch' && col.switchConfig}
                      <div class="flex items-center">
                        <Switch.Root
                          checked={row[col.field as string] ===
                            (col.switchConfig.activeValue ?? true)}
                          onCheckedChange={checked => handleSwitchChange(row, col, checked)}
                          class="peer inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#409eff] focus-visible:ring-offset-2 focus-visible:ring-offset-white disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-[#409eff] data-[state=unchecked]:bg-gray-200 dark:data-[state=unchecked]:bg-gray-700 dark:focus-visible:ring-offset-gray-950"
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
                            alt={col.label}
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
                  <td class="px-4 py-3 text-sm">
                    <div class="flex items-center gap-2">
                      {#each config.table.actions as action}
                        {@const show =
                          typeof action.show === 'function'
                            ? action.show(row)
                            : action.show !== false}
                        {@const hasPermission = hasActionPermission(action)}
                        {#if show && hasPermission}
                          <Button.Root
                            onclick={() => handleActionClick(action, row)}
                            class="text-sm hover:underline {getActionColor(action.type)}"
                          >
                            {#if action.icon}
                              <Icon
                                name={action.icon.replace('pi pi-', '')}
                                size={12}
                                class="inline-block mr-1"
                              />
                            {/if}
                            {action.label}
                          </Button.Root>
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
        <div class="flex items-center gap-1">
          <Button.Root
            onclick={() => setPage(Math.max(1, currentPage - 1))}
            disabled={currentPage === 1}
            class="w-8 h-8 flex items-center justify-center rounded border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            aria-label="上一页"
          >
            <ChevronLeft size={14} />
          </Button.Root>
          {#each pageButtons as page}
            <Button.Root
              onclick={() => setPage(page)}
              class="w-8 h-8 flex items-center justify-center rounded text-sm transition-colors {currentPage ===
              page
                ? 'bg-[#409eff] text-white'
                : 'border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff]'}"
            >
              {page}
            </Button.Root>
          {/each}
          <Button.Root
            onclick={() => setPage(Math.min(totalPages, currentPage + 1))}
            disabled={currentPage === totalPages}
            class="w-8 h-8 flex items-center justify-center rounded border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            aria-label="下一页"
          >
            <ChevronRight size={14} />
          </Button.Root>
          <span class="ml-2 text-sm text-gray-500">跳至</span>
          <input
            type="number"
            bind:value={jumpToPage}
            onkeydown={handleJumpKeydown}
            min="1"
            max={totalPages}
            class="w-12 h-8 px-2 text-sm text-center border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus:outline-none focus:border-[#409eff] [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
            placeholder=""
          />
          <span class="text-sm text-gray-500">页，共 {totalPages} 页</span>
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
              ? $t('table.addTitle', { title: $t(config.title) })
              : $t('table.editTitle', { title: $t(config.title) })}
          </Dialog.Title>
          <Dialog.Close class="text-gray-400 hover:text-gray-600 transition-colors">
            <X size={16} />
          </Dialog.Close>
        </div>
        <div class="p-6 space-y-4 overflow-y-auto flex-1">
          {#each config.form.fields as field}
            <div>
              <label
                for="form-{field.field}"
                class="block text-sm text-gray-600 dark:text-gray-400 mb-1.5"
              >
                {#if field.required}
                  <span class="text-red-500 mr-1">*</span>
                {/if}
                {$t(field.label)}
              </label>
              {#if field.type === 'textarea'}
                <textarea
                  id="form-{field.field}"
                  bind:value={formData[field.field]}
                  placeholder={field.placeholder ? $t(field.placeholder) : ''}
                  rows={field.rows || 3}
                  maxlength={field.maxLength}
                  disabled={field.disabled}
                  class="w-full px-3 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus:outline-none focus:border-[#409eff] resize-none"
                ></textarea>
              {:else if field.type === 'number'}
                <input
                  id="form-{field.field}"
                  type="number"
                  bind:value={formData[field.field]}
                  placeholder={field.placeholder ? $t(field.placeholder) : ''}
                  disabled={field.disabled}
                  class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus:outline-none focus:border-[#409eff]"
                />
              {:else if field.type === 'select'}
                <FormSelect
                  bind:value={formData[field.field] as string | number}
                  options={field.options || []}
                  placeholder={field.placeholder
                    ? $t(field.placeholder)
                    : $t('table.selectPlaceholder')}
                  disabled={field.disabled}
                />
              {:else if field.type === 'tags'}
                <TagInput
                  bind:value={formData[field.field] as string}
                  placeholder={field.placeholder
                    ? $t(field.placeholder)
                    : '输入后按回车或逗号添加标签'}
                  disabled={field.disabled}
                />
              {:else if field.type === 'date'}
                <DatePicker
                  bind:value={formData[field.field] as string}
                  placeholder={field.placeholder ? $t(field.placeholder) : ''}
                  disabled={field.disabled}
                />
              {:else if field.type === 'datetime'}
                <DatePicker
                  bind:value={formData[field.field] as string}
                  placeholder={field.placeholder ? $t(field.placeholder) : ''}
                  disabled={field.disabled}
                />
              {:else}
                <input
                  id="form-{field.field}"
                  type="text"
                  bind:value={formData[field.field]}
                  placeholder={field.placeholder ? $t(field.placeholder) : ''}
                  maxlength={field.maxLength}
                  disabled={field.disabled}
                  class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus:outline-none focus:border-[#409eff]"
                />
              {/if}
              {#if field.tip}
                <p class="mt-1 text-xs text-gray-400">{$t(field.tip)}</p>
              {/if}
            </div>
          {/each}
        </div>
        <div
          class="px-6 py-4 border-t border-gray-100 dark:border-gray-800 flex justify-end gap-2 flex-shrink-0"
        >
          <Dialog.Close
            class="h-9 px-4 border border-gray-200 dark:border-gray-700 hover:border-[#409eff] hover:text-[#409eff] text-sm rounded transition-colors"
          >
            {$t('common.cancel')}
          </Dialog.Close>
          <button
            onclick={handleSubmit}
            disabled={submitting}
            class="h-9 px-4 bg-[#409eff] hover:bg-[#66b1ff] text-white text-sm rounded transition-colors disabled:opacity-50"
          >
            {submitting ? $t('table.submitting') : $t('common.confirm')}
          </button>
        </div>
      </Dialog.Content>
    </Dialog.Portal>
  </Dialog.Root>
{/if}
