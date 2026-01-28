<script lang="ts">
  /**
   * 加载组件
   * 支持不同尺寸和全屏模式
   */
  import { Loader2 } from 'lucide-svelte';
  import { t } from '$lib/locales';

  export let text: string = '';
  export let size: 'sm' | 'md' | 'lg' = 'md';
  export let fullscreen: boolean = false;

  const sizeMap = {
    sm: 18,
    md: 24,
    lg: 36,
  };

  $: displayText = text || $t('common.loading');
</script>

{#if fullscreen}
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-white/80 dark:bg-[#141414]/80 backdrop-blur-sm"
  >
    <div class="flex flex-col items-center justify-center gap-3">
      <Loader2 size={sizeMap[size]} class="text-[#409eff] animate-spin" />
      {#if displayText}
        <span class="text-sm text-gray-500 dark:text-gray-400">{displayText}</span>
      {/if}
    </div>
  </div>
{:else}
  <div class="flex items-center justify-center py-8">
    <div class="flex flex-col items-center justify-center gap-3">
      <Loader2 size={sizeMap[size]} class="text-[#409eff] animate-spin" />
      {#if displayText}
        <span class="text-sm text-gray-500 dark:text-gray-400">{displayText}</span>
      {/if}
    </div>
  </div>
{/if}
