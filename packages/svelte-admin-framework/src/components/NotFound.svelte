<script lang="ts">
  /**
   * 404 页面组件
   * @zhoolg/svelte-admin-framework
   */
  import { AlertCircle, ChevronRight, ArrowLeft, Home } from 'lucide-svelte';
  import { getI18n } from '../locales';

  interface Props {
    /** 当前访问的路径 */
    path?: string;
    /** 导航到首页的函数 */
    onGoHome?: () => void;
    /** 返回上一页的函数 */
    onGoBack?: () => void;
    /** 应用标题 */
    appTitle?: string;
    /** 支持邮箱 */
    supportEmail?: string;
  }

  let {
    path = '',
    onGoHome,
    onGoBack,
    appTitle = '',
    supportEmail = '',
  }: Props = $props();

  const { t } = getI18n();

  function goHome() {
    if (onGoHome) {
      onGoHome();
    } else if (typeof window !== 'undefined') {
      window.location.href = '/';
    }
  }

  function goBack() {
    if (onGoBack) {
      onGoBack();
    } else if (typeof window !== 'undefined') {
      window.history.back();
    }
  }
</script>

<div class="min-h-[calc(100vh-200px)] flex items-center justify-center p-4">
  <div class="max-w-2xl w-full text-center">
    <!-- 404 大标题 -->
    <div class="mb-8">
      <h1 class="text-9xl font-bold text-[#409eff] dark:text-[#66b1ff] mb-4 animate-pulse">404</h1>
      <div class="flex items-center justify-center gap-2 text-gray-400 dark:text-gray-500 mb-2">
        <div
          class="h-px flex-1 bg-gradient-to-r from-transparent to-gray-300 dark:to-gray-600"
        ></div>
        <AlertCircle size={24} />
        <div
          class="h-px flex-1 bg-gradient-to-l from-transparent to-gray-300 dark:to-gray-600"
        ></div>
      </div>
    </div>

    <!-- 错误信息 -->
    <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg p-8 mb-6">
      <h2 class="text-2xl font-semibold text-gray-800 dark:text-white mb-4">
        {$t('notFound.title')}
      </h2>
      <p class="text-gray-600 dark:text-gray-400 mb-6">
        {$t('notFound.subtitle')}
      </p>

      {#if path}
        <div class="bg-gray-50 dark:bg-[#2d2d2d] rounded-lg p-4 mb-6">
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-2">
            {$t('notFound.pathLabel')}
          </p>
          <code class="text-sm text-[#409eff] dark:text-[#66b1ff] font-mono break-all">
            {path}
          </code>
        </div>
      {/if}

      <!-- 可能的原因 -->
      <div class="text-left bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 mb-6">
        <p class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
          {$t('notFound.reasonTitle')}
        </p>
        <ul class="text-sm text-gray-600 dark:text-gray-400 space-y-1">
          <li class="flex items-start gap-2">
            <ChevronRight size={12} class="mt-1 flex-shrink-0" />
            <span>{$t('notFound.reason1')}</span>
          </li>
          <li class="flex items-start gap-2">
            <ChevronRight size={12} class="mt-1 flex-shrink-0" />
            <span>{$t('notFound.reason2')}</span>
          </li>
          <li class="flex items-start gap-2">
            <ChevronRight size={12} class="mt-1 flex-shrink-0" />
            <span>{$t('notFound.reason3')}</span>
          </li>
          <li class="flex items-start gap-2">
            <ChevronRight size={12} class="mt-1 flex-shrink-0" />
            <span>{$t('notFound.reason4')}</span>
          </li>
        </ul>
      </div>

      <!-- 操作按钮 -->
      <div class="flex flex-col sm:flex-row gap-3 justify-center">
        <button
          type="button"
          onclick={goBack}
          class="px-6 py-3 bg-gray-100 hover:bg-gray-200 dark:bg-[#2d2d2d] dark:hover:bg-[#3d3d3d] text-gray-700 dark:text-gray-300 rounded-lg transition-colors flex items-center justify-center gap-2"
        >
          <ArrowLeft size={16} />
          <span>{$t('notFound.back')}</span>
        </button>
        <button
          type="button"
          onclick={goHome}
          class="px-6 py-3 bg-[#409eff] hover:bg-[#66b1ff] text-white rounded-lg transition-colors flex items-center justify-center gap-2 shadow-lg shadow-blue-500/30"
        >
          <Home size={16} />
          <span>{$t('notFound.home')}</span>
        </button>
      </div>
    </div>

    <!-- 底部提示 -->
    {#if supportEmail}
      <p class="text-sm text-gray-500 dark:text-gray-400">
        {$t('notFound.footer')}
        <a href="mailto:{supportEmail}" class="text-[#409eff] hover:text-[#66b1ff] transition-colors">
          {$t('notFound.support')}
        </a>
      </p>
    {/if}
  </div>
</div>

<style>
  @keyframes pulse {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.7;
    }
  }

  .animate-pulse {
    animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
  }
</style>
