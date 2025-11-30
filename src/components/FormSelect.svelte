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

  export let value: string | number = '';
  export let options: Array<{ label: string; value: string | number }> = [];
  export let placeholder: string = '请选择';
  export let disabled: boolean = false;

  // 找到当前选中的选项
  $: selectedOption = options.find(opt => opt.value === value);
  $: displayValue = selectedOption?.label || placeholder;
</script>

<Select.Root
  type="single"
  {disabled}
>
  <Select.Trigger
    class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus:outline-none focus:border-[#409eff] flex items-center justify-between disabled:opacity-50 disabled:cursor-not-allowed"
  >
    <span class="text-gray-900 dark:text-white {!selectedOption ? 'text-gray-400' : ''}">
      {displayValue}
    </span>
    <i class="pi pi-chevron-down text-xs text-gray-400"></i>
  </Select.Trigger>

  <Select.Portal>
    <Select.Content
      class="w-[var(--bits-select-trigger-width)] max-h-[300px] overflow-y-auto bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg mt-1 py-1 z-[9999]"
    >
      <!-- 占位符选项 -->
      <Select.Item
        value=""
        label={placeholder}
        class="px-3 py-2 text-sm text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer transition-colors data-[highlighted]:bg-gray-100 data-[highlighted]:dark:bg-gray-700"
      >
        {placeholder}
      </Select.Item>

      <!-- 选项列表 -->
      {#each options as option}
        <Select.Item
          value={String(option.value)}
          label={option.label}
          onclick={() => { value = option.value; }}
          class="px-3 py-2 text-sm text-gray-900 dark:text-white hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer transition-colors data-[highlighted]:bg-gray-100 data-[highlighted]:dark:bg-gray-700 data-[selected]:bg-[#409eff] data-[selected]:text-white flex items-center justify-between"
        >
          <span>{option.label}</span>
          {#if value === option.value}
            <i class="pi pi-check text-xs"></i>
          {/if}
        </Select.Item>
      {/each}
    </Select.Content>
  </Select.Portal>
</Select.Root>
