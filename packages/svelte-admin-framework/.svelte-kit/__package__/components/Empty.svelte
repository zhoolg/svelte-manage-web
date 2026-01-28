<script lang="ts">
  /**
   * 空状态组件
   * @zhoolg/svelte-admin-framework
   */
  import { getI18n } from '../locales';
  import Icon from './Icon.svelte';

  export let text: string = '';
  export let icon: string = 'inbox';
  export let description: string = '';

  const { t } = getI18n();
  $: displayText = text || $t('common.noData');
</script>

<div class="flex flex-col items-center justify-center py-12 text-gray-400 dark:text-gray-500">
  <Icon name={icon} size={48} class="mb-3 opacity-50" />
  <p class="text-sm">{displayText}</p>
  {#if description}
    <p class="text-xs mt-1 opacity-70">{description}</p>
  {/if}
</div>
