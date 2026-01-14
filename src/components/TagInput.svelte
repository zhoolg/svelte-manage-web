<script lang="ts">
  /**
   * 标签输入组件
   * ============================================================
   *
   * 功能：
   * - 支持添加/删除标签
   * - 回车或逗号添加标签
   * - 点击删除标签
   * - 自动去除空白
   */

  export let value: string = '';
  export let placeholder: string = '输入后按回车或逗号添加标签';
  export let disabled: boolean = false;

  let inputValue = '';
  let tags: string[] = [];

  // 初始化标签列表
  $: {
    if (value) {
      tags = value.split(',').map(t => t.trim()).filter(t => t);
    } else {
      tags = [];
    }
  }

  // 更新 value
  function updateValue() {
    value = tags.join(',');
  }

  // 添加标签
  function addTag() {
    const newTag = inputValue.trim();
    if (newTag && !tags.includes(newTag)) {
      tags = [...tags, newTag];
      updateValue();
    }
    inputValue = '';
  }

  // 删除标签
  function removeTag(index: number) {
    tags = tags.filter((_, i) => i !== index);
    updateValue();
  }

  // 处理键盘事件
  function handleKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addTag();
    } else if (e.key === 'Backspace' && !inputValue && tags.length > 0) {
      // 如果输入框为空且按下退格键，删除最后一个标签
      removeTag(tags.length - 1);
    }
  }

  // 处理输入事件（自动检测逗号）
  function handleInput(e: Event) {
    const target = e.target as HTMLInputElement;
    if (target.value.includes(',')) {
      inputValue = target.value.replace(',', '');
      addTag();
    }
  }
</script>

<div class="w-full">
  <div class="min-h-[36px] px-3 py-1.5 text-sm border border-gray-200 dark:border-gray-700 rounded bg-white dark:bg-gray-800 focus-within:border-[#409eff] flex flex-wrap gap-1.5 items-center {disabled ? 'opacity-50 cursor-not-allowed' : ''}">
    {#each tags as tag, index}
      <span class="inline-flex items-center gap-1 px-2 py-0.5 text-xs rounded bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-800">
        {tag}
        {#if !disabled}
          <button
            type="button"
            onclick={() => removeTag(index)}
            class="hover:text-blue-800 dark:hover:text-blue-200 transition-colors"
          >
            <i class="pi pi-times text-[10px]"></i>
          </button>
        {/if}
      </span>
    {/each}
    {#if !disabled}
      <input
        type="text"
        bind:value={inputValue}
        onkeydown={handleKeydown}
        oninput={handleInput}
        placeholder={placeholder}
        class="flex-1 min-w-[120px] outline-none bg-transparent text-gray-900 dark:text-white placeholder:text-gray-400"
      />
    {/if}
  </div>
  <p class="mt-1 text-xs text-gray-400">按回车或逗号添加标签，点击标签上的 × 删除</p>
</div>
