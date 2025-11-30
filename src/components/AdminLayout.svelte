<script lang="ts">
  /**
   * 后台管理布局组件
   * 包含：侧边栏、顶部导航、标签页、内容区域
   */
  import { settingsStore } from '../stores/settingsStore';
  import Sidebar from './Sidebar.svelte';
  import Header from './Header.svelte';
  import TagsView from './TagsView.svelte';

  let collapsed = false;

  function handleToggleSidebar() {
    collapsed = !collapsed;
  }

  // 监听设置变化，应用暗色模式
  $: if (typeof document !== 'undefined') {
    const isDark = $settingsStore.theme === 'dark' ||
      ($settingsStore.theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }
</script>

<div class="min-h-screen bg-[#f5f7fa] dark:bg-[#0a0a0a] flex">
  <!-- 侧边栏 -->
  <Sidebar {collapsed} />

  <!-- 主内容区域 -->
  <div
    class="flex-1 flex flex-col transition-all duration-300"
    style:margin-left="{collapsed ? '64px' : '210px'}"
  >
    <!-- 顶部导航 -->
    <Header {collapsed} onToggle={handleToggleSidebar} />

    <!-- 标签页 -->
    <TagsView />

    <!-- 内容区域 -->
    <main class="flex-1 p-4 overflow-auto">
      <slot />
    </main>
  </div>
</div>

<style>
  :global(html.dark) {
    color-scheme: dark;
  }
</style>
