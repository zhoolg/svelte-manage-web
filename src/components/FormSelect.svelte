<script lang="ts">
  /**
   * 表单选择框组件 - 使用 Bits UI Select
   * ============================================================
   *
   * 功能：
   * - 下拉选择框
   * - 支持占位符
   * - 支持选项列表
   * - 支持禁用状态
   * - 完整的可访问性支持
   */
  import { Select } from 'bits-ui';
  import { ChevronDown, Check } from 'lucide-svelte';
  import { t } from '$lib/locales';

  let {
    value = $bindable<string | number>(),
    options = [] as Array<{ label: string; value: string | number }>,
    placeholder = '',
    disabled = false,
  }: {
    value?: string | number;
    options?: Array<{ label: string; value: string | number }>;
    placeholder?: string;
    disabled?: boolean;
  } = $props();

  // 找到当前选中的选项
  const displayPlaceholder = $derived(placeholder || $t('table.selectPlaceholder'));
  const selectedOption = $derived(options.find(opt => opt.value === value));
  const displayValue = $derived(selectedOption ? $t(selectedOption.label) : displayPlaceholder);
</script>

<Select.Root type="single" {disabled}>
  <Select.Trigger
    class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 hover:border-[#409eff] focus:outline-none focus:border-[#409eff] focus:ring-1 focus:ring-[#409eff]/20 flex items-center justify-between disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
  >
    <span class="truncate text-gray-900 dark:text-white {!selectedOption ? 'text-gray-400' : ''}">
      {displayValue}
    </span>
    <ChevronDown size={14} class="ml-2 shrink-0 text-gray-400 transition-transform duration-200" />
  </Select.Trigger>

  <Select.Portal>
    <Select.Content
      sideOffset={4}
      class="w-(--bits-select-trigger-width) min-w-30 max-h-75 overflow-y-auto bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-xl py-1.5 z-9999 animate-in fade-in-0 zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=top]:slide-in-from-bottom-2"
    >
      <!-- 占位符选项 -->
      <Select.Item
        value=""
        label={displayPlaceholder}
        class="relative px-3 py-2 text-sm text-gray-400 cursor-pointer rounded-md mx-1 transition-colors data-highlighted:bg-gray-100 data-highlighted:dark:bg-gray-700 data-highlighted:outline-none"
      >
        {displayPlaceholder}
      </Select.Item>

      <!-- 选项列表 -->
      {#each options as option}
        <Select.Item
          value={String(option.value)}
          label={$t(option.label)}
          onclick={() => {
            value = option.value;
          }}
          class="relative px-3 py-2 text-sm cursor-pointer rounded-md mx-1 transition-colors data-highlighted:bg-gray-100 data-highlighted:dark:bg-gray-700 data-highlighted:outline-none {value === option.value
            ? 'bg-[#409eff]/10 text-[#409eff] dark:bg-[#409eff]/20 dark:text-[#409eff] font-medium'
            : 'text-gray-900 dark:text-white'}"
        >
          <span class="flex items-center justify-between">
            <span>{$t(option.label)}</span>
            {#if value === option.value}
              <Check size={14} class="text-[#409eff]" />
            {/if}
          </span>
        </Select.Item>
      {/each}
    </Select.Content>
  </Select.Portal>
</Select.Root>
