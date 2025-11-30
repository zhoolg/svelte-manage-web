<script lang="ts">
  /**
   * 空状态组件
   * 用于显示无数据状态
   */
  export let text: string = '暂无数据';
  export let icon: string = 'pi-inbox';
  export let description: string = '';
</script>

<div class="flex flex-col items-center justify-center py-12 text-gray-400 dark:text-gray-500">
  <i class="pi {icon} text-5xl mb-3 opacity-50"></i>
  <p class="text-sm">{text}</p>
  {#if description}
    <p class="text-xs mt-1 opacity-70">{description}</p>
  {/if}
</div>
