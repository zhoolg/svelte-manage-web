<script lang="ts">
  /**
   * 后台管理布局组件
   * 包含：侧边栏、顶部导航、标签页、内容区域
   */
  import { onMount } from 'svelte';
  import { settingsStore } from '../stores/settingsStore';
  import { authStore } from '../stores/authStore';
  import { currentPath } from '../stores/routerStore';
  import Sidebar from './Sidebar.svelte';
  import Header from './Header.svelte';
  import TagsView from './TagsView.svelte';

  let collapsed = false;
  let isMobile = false;
  let mobileSidebarOpen = false;
  let defaultPasswordToastShown = false;

  function handleToggleSidebar() {
    if (isMobile) {
      if (mobileSidebarOpen) {
        releaseMobileSidebarFocus();
      }
      mobileSidebarOpen = !mobileSidebarOpen;
      return;
    }

    collapsed = !collapsed;
  }

  function closeMobileSidebar() {
    releaseMobileSidebarFocus();
    mobileSidebarOpen = false;
  }

  function releaseMobileSidebarFocus() {
    if (typeof document === 'undefined') {
      return;
    }

    const activeElement = document.activeElement;
    const sidebar = document.querySelector('[data-admin-sidebar="true"]');
    if (
      activeElement instanceof HTMLElement &&
      sidebar instanceof HTMLElement &&
      sidebar.contains(activeElement)
    ) {
      activeElement.blur();
    }
  }

  onMount(() => {
    const mediaQuery = window.matchMedia('(max-width: 768px)');
    const syncViewport = () => {
      isMobile = mediaQuery.matches;
      if (!isMobile) {
        mobileSidebarOpen = false;
      }
    };

    syncViewport();
    mediaQuery.addEventListener('change', syncViewport);

    return () => {
      mediaQuery.removeEventListener('change', syncViewport);
    };
  });

  $: if (isMobile && $currentPath) {
    closeMobileSidebar();
  }

  // 监听设置变化，应用暗色模式
  $: if (typeof document !== 'undefined') {
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

  $: if ($authStore.user?.usingDefaultPassword && !defaultPasswordToastShown) {
    defaultPasswordToastShown = true;
    if (typeof window !== 'undefined') {
      (window as any).toast?.warning('您现在使用的是默认密码，建议前往个人中心修改密码。');
    }
  }
</script>

<div class="min-h-screen bg-[var(--color-bg-page)] flex">
  <!-- 侧边栏 -->
  <Sidebar
    collapsed={isMobile ? false : collapsed}
    mobile={isMobile}
    open={isMobile ? mobileSidebarOpen : true}
    onNavigate={closeMobileSidebar}
  />

  {#if isMobile && mobileSidebarOpen}
    <button
      type="button"
      class="fixed inset-0 z-40 bg-black/45 md:hidden"
      aria-label="关闭侧边栏"
      onclick={closeMobileSidebar}
    ></button>
  {/if}

  <!-- 主内容区域 -->
  <div
    class="min-w-0 flex-1 flex flex-col transition-all duration-300"
    style:margin-left={isMobile ? '0' : collapsed ? '54px' : '210px'}
  >
    <!-- 顶部导航 -->
    <Header onToggle={handleToggleSidebar} />

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
