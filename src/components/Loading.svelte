<script lang="ts">
  /**
   * 加载组件
   * 支持不同尺寸和全屏模式
   */
  import { t } from '$lib/locales';

  export let text: string = '';
  export let size: 'sm' | 'md' | 'lg' = 'md';
  export let fullscreen: boolean = false;

  const sizeClasses = {
    sm: 'text-lg',
    md: 'text-2xl',
    lg: 'text-4xl',
  };

  $: displayText = text || $t('common.loading');
</script>

{#if fullscreen}
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-white/80 dark:bg-[#141414]/80 backdrop-blur-sm">
    <div class="flex flex-col items-center justify-center gap-3">
      <i class="pi pi-spin pi-spinner {sizeClasses[size]} text-[#409eff]"></i>
      {#if displayText}
        <span class="text-sm text-gray-500 dark:text-gray-400">{displayText}</span>
      {/if}
    </div>
  </div>
{:else}
  <div class="flex items-center justify-center py-8">
    <div class="flex flex-col items-center justify-center gap-3">
      <i class="pi pi-spin pi-spinner {sizeClasses[size]} text-[#409eff]"></i>
      {#if displayText}
        <span class="text-sm text-gray-500 dark:text-gray-400">{displayText}</span>
      {/if}
    </div>
  </div>
{/if}
