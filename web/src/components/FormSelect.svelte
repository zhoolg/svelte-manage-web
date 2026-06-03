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

  let triggerEl = $state<HTMLButtonElement | null>(null);
  let triggerWidth = $state(0);

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
  const contentStyle = $derived(
    triggerWidth ? `width: ${triggerWidth}px; min-width: ${triggerWidth}px;` : ''
  );

  function updateTriggerWidth() {
    triggerWidth = triggerEl?.getBoundingClientRect().width ?? 0;
  }

  $effect(() => {
    if (!triggerEl) return;

    updateTriggerWidth();
    const observer = new ResizeObserver(updateTriggerWidth);
    observer.observe(triggerEl);

    return () => observer.disconnect();
  });
</script>

<Select.Root type="single" {disabled}>
  <Select.Trigger
    bind:ref={triggerEl}
    class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-md bg-white dark:bg-gray-800 hover:border-[color:var(--color-primary)] focus:outline-none focus:border-[color:var(--color-primary)] focus:ring-1 focus:ring-[color:var(--color-primary)] flex items-center justify-between disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
  >
    <span class="truncate text-gray-900 dark:text-white {!selectedOption ? 'text-gray-400' : ''}">
      {displayValue}
    </span>
    <ChevronDown size={14} class="ml-2 shrink-0 text-gray-400 transition-transform duration-200" />
  </Select.Trigger>

  <Select.Portal>
    <Select.Content
      sideOffset={4}
      align="start"
      style={contentStyle}
      class="max-h-[240px] overflow-y-auto bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-md shadow-lg py-1 z-9999 animate-in fade-in-0 zoom-in-95 data-[side=bottom]:slide-in-from-top-1 data-[side=top]:slide-in-from-bottom-1"
    >
      <!-- 占位符选项 -->
      <Select.Item
        value=""
        label={displayPlaceholder}
        class="relative mx-1 flex h-9 items-center rounded-md px-3 text-sm text-gray-400 cursor-pointer transition-colors data-highlighted:bg-gray-50 data-highlighted:dark:bg-gray-700 data-highlighted:outline-none"
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
          class="relative mx-1 flex h-9 items-center rounded-md px-3 text-sm cursor-pointer transition-colors data-highlighted:bg-gray-50 data-highlighted:dark:bg-gray-700 data-highlighted:outline-none {value ===
          option.value
            ? 'bg-[color:var(--color-primary-subtle)] text-[color:var(--color-primary)] dark:bg-[color:color-mix(in_srgb,var(--color-primary)_18%,transparent)] font-medium'
            : 'text-gray-900 dark:text-white'}"
        >
          <span class="flex w-full items-center justify-between gap-3">
            <span class="truncate">{$t(option.label)}</span>
            {#if value === option.value}
              <Check size={14} class="text-[color:var(--color-primary)]" />
            {/if}
          </span>
        </Select.Item>
      {/each}
    </Select.Content>
  </Select.Portal>
</Select.Root>
