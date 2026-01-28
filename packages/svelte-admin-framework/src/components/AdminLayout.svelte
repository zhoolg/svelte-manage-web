<script lang="ts">
  /**
   * 后台管理布局组件
   * @zhoolg/svelte-admin-framework
   */
  import { settingsStore } from '../stores/settingsStore';
  import Sidebar, { type MenuItem } from './Sidebar.svelte';
  import TagsView from './TagsView.svelte';

  interface Props {
    /** 菜单配置 */
    menuConfig: MenuItem[];
    /** 当前路径 */
    currentPath: string;
    /** 路由名称映射 */
    routeNames: Record<string, string>;
    /** 导航函数 */
    onNavigate: (path: string) => void;
    /** 应用标题 */
    appTitle?: string;
    /** Logo 图标 */
    logoIcon?: string;
    /** 是否显示标签页 */
    showTagsView?: boolean;
    /** 自定义 Header 组件 */
    children?: import('svelte').Snippet;
    /** Header 插槽 */
    header?: import('svelte').Snippet<[{ collapsed: boolean; onToggle: () => void }]>;
  }

  let {
    menuConfig,
    currentPath,
    routeNames,
    onNavigate,
    appTitle = '管理系统',
    logoIcon = 'briefcase',
    showTagsView = true,
    children,
    header,
  }: Props = $props();

  let collapsed = $state(false);

  function handleToggleSidebar() {
    collapsed = !collapsed;
  }

  // 监听设置变化，应用暗色模式
  $effect(() => {
    if (typeof document !== 'undefined') {
      const isDark =
        $settingsStore.theme === 'dark' ||
        ($settingsStore.theme === 'system' &&
          window.matchMedia('(prefers-color-scheme: dark)').matches);
      if (isDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    }
  });
</script>

<div class="min-h-screen bg-[#f5f7fa] dark:bg-[#0a0a0a] flex">
  <!-- 侧边栏 -->
  <Sidebar {collapsed} {menuConfig} {currentPath} {onNavigate} {appTitle} {logoIcon} />

  <!-- 主内容区域 -->
  <div
    class="flex-1 flex flex-col transition-all duration-300"
    style:margin-left={collapsed ? '64px' : '210px'}
  >
    <!-- 顶部导航（通过插槽自定义） -->
    {#if header}
      {@render header({ collapsed, onToggle: handleToggleSidebar })}
    {:else}
      <!-- 默认简单 Header -->
      <header
        class="sticky top-0 z-30 bg-white dark:bg-[#141414] h-[50px] border-b border-[#ebeef5] dark:border-[#303030] flex items-center px-4"
      >
        <button
          onclick={handleToggleSidebar}
          class="w-[40px] h-[40px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors rounded"
          aria-label="Toggle Menu"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="text-[#5a5e66] dark:text-[#ccc]"
          >
            <line x1="4" x2="20" y1="12" y2="12" />
            <line x1="4" x2="20" y1="6" y2="6" />
            <line x1="4" x2="20" y1="18" y2="18" />
          </svg>
        </button>
      </header>
    {/if}

    <!-- 标签页 -->
    {#if showTagsView}
      <TagsView {currentPath} {routeNames} {onNavigate} />
    {/if}

    <!-- 内容区域 -->
    <main class="flex-1 p-4 overflow-auto">
      {#if children}
        {@render children()}
      {:else}
        <slot />
      {/if}
    </main>
  </div>
</div>

<style>
  :global(html.dark) {
    color-scheme: dark;
  }
</style>
