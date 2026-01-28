<script lang="ts">
  /**
   * 空状态组件
   * ============================================================
   *
   * 功能：
   * - 显示空状态提示
   * - 支持自定义图标、标题、描述
   * - 支持自定义操作按钮
   * - 多种预设类型（empty、developing、error、noPermission）
   */
  import { navigate } from '../stores/routerStore';
  import { t } from '$lib/locales';
  import Icon from './Icon.svelte';

  // 组件属性
  export let type: 'empty' | 'developing' | 'error' | 'noPermission' | 'custom' = 'empty';
  export let icon: string = '';
  export let title: string = '';
  export let description: string = '';
  export let showButton: boolean = true;
  export let buttonText: string = '';
  export let buttonIcon: string = 'home';
  export let onButtonClick: (() => void) | null = null;

  // 预设配置
  $: presets = {
    empty: {
      icon: 'inbox',
      iconColor: 'text-gray-400',
      title: $t('emptyState.empty.title'),
      description: $t('emptyState.empty.desc'),
    },
    developing: {
      icon: 'wrench',
      iconColor: 'text-yellow-500',
      title: $t('emptyState.developing.title'),
      description: $t('emptyState.developing.desc'),
    },
    error: {
      icon: 'alert-triangle',
      iconColor: 'text-red-500',
      title: $t('emptyState.error.title'),
      description: $t('emptyState.error.desc'),
    },
    noPermission: {
      icon: 'lock',
      iconColor: 'text-orange-500',
      title: $t('emptyState.noPermission.title'),
      description: $t('emptyState.noPermission.desc'),
    },
    custom: {
      icon: 'info',
      iconColor: 'text-blue-500',
      title: $t('emptyState.custom.title'),
      description: '',
    },
  };

  // 获取当前配置
  $: currentConfig = presets[type];
  $: finalIcon = icon || currentConfig.icon;
  $: finalTitle = title || currentConfig.title;
  $: finalDescription = description || currentConfig.description;
  $: finalButtonText = buttonText || $t('emptyState.buttonText');

  // 按钮点击处理
  function handleButtonClick() {
    if (onButtonClick) {
      onButtonClick();
    } else {
      navigate('/');
    }
  }
</script>

<div class="flex items-center justify-center min-h-[400px] p-8">
  <div class="text-center max-w-md">
    <!-- 图标 -->
    <div class="mb-6">
      <Icon name={finalIcon} size={64} class={currentConfig.iconColor} />
    </div>

    <!-- 标题 -->
    <h3 class="text-xl font-semibold text-gray-800 dark:text-white mb-3">
      {finalTitle}
    </h3>

    <!-- 描述 -->
    {#if finalDescription}
      <p class="text-gray-500 dark:text-gray-400 mb-6">
        {finalDescription}
      </p>
    {/if}

    <!-- 插槽：自定义内容 -->
    <slot />

    <!-- 操作按钮 -->
    {#if showButton}
      <button
        on:click={handleButtonClick}
        class="px-6 py-2.5 bg-[#409eff] hover:bg-[#66b1ff] text-white rounded-lg transition-colors inline-flex items-center gap-2"
      >
        <Icon name={buttonIcon} size={16} />
        <span>{finalButtonText}</span>
      </button>
    {/if}
  </div>
</div>
