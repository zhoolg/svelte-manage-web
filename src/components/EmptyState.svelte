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

  // 组件属性
  export let type: 'empty' | 'developing' | 'error' | 'noPermission' | 'custom' = 'empty';
  export let icon: string = '';
  export let title: string = '';
  export let description: string = '';
  export let showButton: boolean = true;
  export let buttonText: string = '返回首页';
  export let buttonIcon: string = 'pi-home';
  export let onButtonClick: (() => void) | null = null;

  // 预设配置
  const presets = {
    empty: {
      icon: 'pi-inbox',
      iconColor: 'text-gray-400',
      title: '暂无数据',
      description: '当前没有任何数据，请稍后再试。',
    },
    developing: {
      icon: 'pi-wrench',
      iconColor: 'text-yellow-500',
      title: '页面开发中',
      description: '该功能正在开发中，敬请期待！',
    },
    error: {
      icon: 'pi-exclamation-triangle',
      iconColor: 'text-red-500',
      title: '出错了',
      description: '页面加载失败，请刷新页面重试。',
    },
    noPermission: {
      icon: 'pi-lock',
      iconColor: 'text-orange-500',
      title: '无访问权限',
      description: '您没有权限访问此页面，请联系管理员。',
    },
    custom: {
      icon: 'pi-info-circle',
      iconColor: 'text-blue-500',
      title: '提示',
      description: '',
    },
  };

  // 获取当前配置
  $: currentConfig = presets[type];
  $: finalIcon = icon || currentConfig.icon;
  $: finalTitle = title || currentConfig.title;
  $: finalDescription = description || currentConfig.description;

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
      <i class="pi {finalIcon} text-6xl {currentConfig.iconColor}"></i>
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
        <i class="pi {buttonIcon}"></i>
        <span>{buttonText}</span>
      </button>
    {/if}
  </div>
</div>
