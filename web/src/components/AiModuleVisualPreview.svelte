<script lang="ts">
  import { FileJson } from 'lucide-svelte';
  import type { FormField, ModuleConfig, SearchField, TableColumn } from '../config/module';
  import { toModuleConfig, type AppModule } from '../config/app.modules';
  import { humanizeIdentifier, locale, resolveDynamicLabel, t } from '../lib/locales';
  import CrudPage from './CrudPage.svelte';

  export let metadata: Record<string, unknown> | null | undefined = null;
  export let resetKey: string | number = '';
  export let generated = false;
  export let validationPassed: boolean | undefined = undefined;

  function buildModuleConfig(
    value: Record<string, unknown> | null | undefined
  ): ModuleConfig | null {
    if (!value || typeof value !== 'object' || !value.crud) {
      return null;
    }
    return toModuleConfig(value as unknown as AppModule);
  }

  $: moduleConfig = buildModuleConfig(metadata);
  $: previewRows = moduleConfig ? buildPreviewRows(moduleConfig, metadata) : [];
  $: previewResetKey = `${resetKey}:${moduleConfig?.name || ''}:${previewRows.length}`;

  function buildPreviewRows(
    config: ModuleConfig,
    value: Record<string, unknown> | null | undefined
  ): Record<string, unknown>[] {
    const workflow = workflowTransitions(value);
    return [0, 1, 2].map(index => {
      const row: Record<string, unknown> = {
        [config.table.rowKey || 'id']: index + 1,
      };

      for (const column of config.table.columns || []) {
        row[String(column.field)] = sampleColumnValue(column, index, workflow);
      }

      for (const field of config.form?.fields || []) {
        if (row[field.field] === undefined) {
          row[field.field] = sampleFieldValue(field, index, workflow);
        }
      }

      for (const field of config.search?.fields || []) {
        if (row[field.field] === undefined) {
          row[field.field] = sampleSearchValue(field, index);
        }
      }

      return row;
    });
  }

  function workflowTransitions(value: Record<string, unknown> | null | undefined) {
    const crud = value?.crud as Record<string, unknown> | undefined;
    const workflow = crud?.workflow;
    return Array.isArray(workflow) ? (workflow as Array<Record<string, unknown>>) : [];
  }

  function sampleColumnValue(
    column: TableColumn,
    rowIndex: number,
    workflow: Array<Record<string, unknown>>
  ): unknown {
    const field = String(column.field);
    const workflowValue = sampleWorkflowValue(field, rowIndex, workflow);
    if (workflowValue !== undefined) {
      return workflowValue;
    }

    if (column.format === 'status' && column.statusMap) {
      const keys = Object.keys(column.statusMap);
      return keys[Math.min(rowIndex, keys.length - 1)] || keys[0];
    }
    if (column.format === 'date') {
      return rowIndex === 0 ? '2026-06-01' : rowIndex === 1 ? '2026-06-08' : '2026-06-15';
    }
    if (column.format === 'datetime') {
      return rowIndex === 0
        ? '2026-06-01 09:30:00'
        : rowIndex === 1
          ? '2026-06-08 14:20:00'
          : '2026-06-15 16:45:00';
    }
    if (column.format === 'money') {
      return (rowIndex + 1) * 128;
    }
    if (column.format === 'percent') {
      return rowIndex === 0 ? 25 : rowIndex === 1 ? 68 : 92;
    }
    if (column.format === 'switch') {
      return rowIndex !== 1;
    }
    if (column.format === 'tags') {
      return rowIndex === 0 ? '重点,待处理' : rowIndex === 1 ? '已跟进' : '普通';
    }
    return previewSampleText(column, column.field, rowIndex);
  }

  function sampleFieldValue(
    field: FormField,
    rowIndex: number,
    workflow: Array<Record<string, unknown>>
  ): unknown {
    const workflowValue = sampleWorkflowValue(field.field, rowIndex, workflow);
    if (workflowValue !== undefined) {
      return workflowValue;
    }
    if (field.defaultValue !== undefined) {
      return field.defaultValue;
    }
    if (field.type === 'select' || field.type === 'radio') {
      return (
        field.options?.[Math.min(rowIndex, Math.max(field.options.length - 1, 0))]?.value ?? ''
      );
    }
    if (field.type === 'checkbox') {
      return field.options?.slice(0, Math.min(rowIndex + 1, 2)).map(option => option.value) || [];
    }
    if (field.type === 'switch') {
      return rowIndex !== 1;
    }
    if (field.type === 'number') {
      return rowIndex + 1;
    }
    if (field.type === 'date') {
      return rowIndex === 0 ? '2026-06-01' : '2026-06-08';
    }
    if (field.type === 'datetime') {
      return rowIndex === 0 ? '2026-06-01T09:30' : '2026-06-08T14:20';
    }
    if (field.type === 'tags') {
      return rowIndex === 0 ? '重点,AI' : '待跟进';
    }
    return previewSampleText(field, field.field, rowIndex);
  }

  function sampleSearchValue(field: SearchField, rowIndex: number): unknown {
    if (field.type === 'select' || field.type === 'radio') {
      return (
        field.options?.[Math.min(rowIndex, Math.max(field.options.length - 1, 0))]?.value ?? ''
      );
    }
    if (field.type === 'date') {
      return rowIndex === 0 ? '2026-06-01' : '2026-06-08';
    }
    if (field.type === 'number') {
      return rowIndex + 1;
    }
    return previewSampleText(field, field.field, rowIndex);
  }

  function sampleWorkflowValue(
    field: string,
    rowIndex: number,
    workflow: Array<Record<string, unknown>>
  ): unknown {
    const related = workflow.filter(item => String(item.statusField || 'status') === field);
    if (related.length === 0) {
      return undefined;
    }
    if (rowIndex === 0) {
      return related[0].from;
    }
    return related[Math.min(rowIndex - 1, related.length - 1)].to;
  }

  function previewLabel(
    item: { label?: string; labelI18n?: unknown },
    fallbackSource: unknown
  ): string {
    return resolveDynamicLabel(
      item.label,
      item.labelI18n,
      $locale,
      $t,
      humanizeIdentifier(fallbackSource)
    );
  }

  function previewSampleText(
    item: { label?: string; labelI18n?: unknown },
    fallbackSource: unknown,
    rowIndex: number
  ): string {
    const label = previewLabel(item, fallbackSource);
    const suffixes =
      $locale === 'en-US' ? ['Example', 'Record', 'Sample'] : ['示例', '记录', '样例'];
    return `${label} ${suffixes[Math.min(rowIndex, suffixes.length - 1)]}`;
  }

  function emptyPreviewMessage(): string {
    if (!generated) {
      return '生成模块后，这里会展示和应用后一致的图形界面';
    }
    if (validationPassed === false) {
      return '当前生成结果未通过校验，修复阻塞项后再展示图形界面';
    }
    return 'metadata 还未就绪，生成预览加载完成后会自动展示';
  }
</script>

<div class="space-y-3">
  <div
    class="flex items-center justify-between gap-3 rounded-lg border border-gray-100 bg-white px-4 py-3 shadow-sm dark:border-gray-800 dark:bg-[#1d1d1d]"
  >
    <div class="flex items-center gap-2">
      <FileJson size={16} class="text-emerald-500" />
      <span class="text-sm font-medium text-gray-800 dark:text-white">图形界面预览</span>
    </div>
    {#if moduleConfig}
      <span class="text-xs text-gray-400">{moduleConfig.api.list}</span>
    {/if}
  </div>

  {#if moduleConfig}
    <CrudPage config={moduleConfig} previewMode={true} {previewRows} {previewResetKey} />
  {:else}
    <div
      class="rounded-lg border border-gray-100 bg-white p-6 text-center text-sm text-gray-400 shadow-sm dark:border-gray-800 dark:bg-[#1d1d1d]"
    >
      {emptyPreviewMessage()}
    </div>
  {/if}
</div>
