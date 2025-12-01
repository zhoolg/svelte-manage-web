<script lang="ts">
  /**
   * 顶部导航栏组件 - 使用 Bits UI 优化
   * ============================================================
   *
   * 重构说明：
   * - 用户菜单：使用 Bits UI Dropdown Menu
   * - 通知面板：使用 Bits UI Popover
   * - 语言切换：使用 Bits UI Dropdown Menu
   * - 全局搜索：使用 Bits UI Dialog
   *
   * 优势：
   * - 自动焦点管理和键盘导航
   * - 完整的可访问性支持
   * - 自动定位和边界检测
   * - 移除手动的 clickOutside 逻辑
   */
  import { onMount } from 'svelte';
  import { DropdownMenu, Popover, Dialog, Button, Tooltip } from 'bits-ui';
  import { authStore } from '../stores/authStore';
  import { settingsStore } from '../stores/settingsStore';
  import { currentPath, navigate, routeNames } from '../stores/routerStore';
  import { menuConfig, getParentMenu, getFlatMenus } from '../config/menu';
  import { t, locale, setLocale, localeOptions } from '../lib/locales';

  export let onToggle: () => void;
  export const collapsed: boolean = false;

  let searchVisible = false;
  let searchQuery = '';
  let isFullscreen = false;
  let searchInputRef: HTMLInputElement;

  // 获取翻译函数
  $: translate = $t;
  $: user = $authStore.user;
  $: theme = $settingsStore.theme;

  // 获取当前路由名称
  $: currentRoute = $t(routeNames[$currentPath] || 'menu.home');
  $: parentMenu = getParentMenu($currentPath);

  // 模拟通知数据
  const notifications = [
    { id: 1, type: 'message', title: '新的代理商申请', content: '李四提交了代理商申请，请及时审核', time: '5分钟前', read: false },
    { id: 2, type: 'system', title: '系统更新通知', content: '系统将于今晚进行维护升级', time: '1小时前', read: false },
    { id: 3, type: 'task', title: '待办提醒', content: '您有3个待审核的申请', time: '2小时前', read: true },
  ];

  $: unreadCount = notifications.filter(n => !n.read).length;

  async function handleLogout() {
    const confirmed = await (window as any).confirm({
      title: translate('message.logoutConfirm'),
      content: translate('message.logoutConfirm'),
      type: 'warning',
    });

    if (!confirmed) return;

    authStore.logout();
    (window as any).toast?.success(translate('message.logoutSuccess'));
  }

  function toggleFullscreen() {
    if (document.fullscreenElement) {
      document.exitFullscreen();
      isFullscreen = false;
    } else {
      document.documentElement.requestFullscreen();
      isFullscreen = true;
    }
  }

  function handleSearch(e: Event) {
    e.preventDefault();
    if (searchQuery.trim()) {
      (window as any).toast?.info(translate('message.searchPlaceholder', { query: searchQuery }));
      searchQuery = '';
      searchVisible = false;
    }
  }

  function handleLanguageChange(lang: typeof localeOptions[0]) {
    console.log('[Header] Language change requested:', lang.value);
    setLocale(lang.value);
    console.log('[Header] Language changed to:', lang.value);
    (window as any).toast?.success(`${translate('settings.language')}: ${lang.label}`);
  }

  function handleKeyDown(e: KeyboardEvent) {
    // Ctrl/Cmd + K 打开搜索
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
      e.preventDefault();
      searchVisible = true;
    }
  }

  onMount(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  });

  // 搜索框显示时自动聚焦
  $: if (searchVisible && searchInputRef) {
    setTimeout(() => searchInputRef?.focus(), 0);
  }
</script>

<header class="sticky top-0 z-30 bg-white dark:bg-[#141414] h-[50px] border-b border-[#ebeef5] dark:border-[#303030]">
  <div class="flex items-center justify-between h-full px-4">
    <!-- 左侧：折叠按钮 + 面包屑 -->
    <div class="flex items-center">
      <Button.Root
        onclick={onToggle}
        class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
        aria-label="Toggle Menu"
      >
        <i class="pi pi-bars text-[18px] text-[#5a5e66] dark:text-[#ccc]"></i>
      </Button.Root>

      <!-- 面包屑导航 -->
      <nav class="flex items-center ml-4 text-[14px]">
        <Button.Root onclick={() => navigate('/')} class="text-[#97a8be] hover:text-[#409eff] cursor-pointer transition-colors">
          <i class="pi pi-home text-[12px] mr-1"></i>
          {translate('menu.home')}
        </Button.Root>
        {#if $currentPath !== '/'}
          {#if parentMenu}
            <span class="mx-2 text-[#97a8be]">/</span>
            <span class="text-[#97a8be]">
              {parentMenu.label}
            </span>
          {/if}
          <span class="mx-2 text-[#97a8be]">/</span>
          <span class="text-[#606266] dark:text-[#ccc]">
            {currentRoute}
          </span>
        {/if}
      </nav>
    </div>

    <!-- 右侧：工具栏 -->
    <div class="flex items-center">
      <!-- 搜索按钮 -->
      <Tooltip.Root>
        <Tooltip.Trigger asChild let:builder>
          <Button.Root
            builders={[builder]}
            onclick={() => searchVisible = true}
            class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
          >
            <i class="pi pi-search text-[18px] text-[#5a5e66] dark:text-[#ccc]"></i>
          </Button.Root>
        </Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            搜索 (Ctrl+K)
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 通知按钮 - 使用 Bits UI Popover -->
      <Tooltip.Root>
        <Popover.Root>
          <Tooltip.Trigger asChild let:builder>
            <Popover.Trigger
              builders={[builder]}
              class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors relative"
            >
              <i class="pi pi-bell text-[18px] text-[#5a5e66] dark:text-[#ccc]"></i>
              {#if unreadCount > 0}
                <span class="absolute top-2 right-2 w-4 h-4 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center">
                  {unreadCount}
                </span>
              {/if}
            </Popover.Trigger>
          </Tooltip.Trigger>

        <Popover.Portal>
          <Popover.Content
            class="w-[320px] bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg border border-[#ebeef5] dark:border-[#303030] z-[9999] overflow-hidden"
            sideOffset={8}
            align="end"
          >
            <div class="px-4 py-3 border-b border-[#ebeef5] dark:border-[#303030] flex items-center justify-between">
              <span class="font-medium text-gray-800 dark:text-white">{translate('common.more')}</span>
              <Button.Root class="text-xs text-[#409eff] hover:underline">{translate('common.all')}</Button.Root>
            </div>
            <div class="max-h-[300px] overflow-y-auto">
              {#each notifications as item}
                <div
                  class="px-4 py-3 border-b border-[#ebeef5] dark:border-[#303030] last:border-0 hover:bg-gray-50 dark:hover:bg-gray-800/50 cursor-pointer {!item.read ? 'bg-blue-50/50 dark:bg-blue-900/10' : ''}"
                >
                  <div class="flex items-start gap-3">
                    <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 {item.type === 'message' ? 'bg-blue-100 text-blue-600' : item.type === 'system' ? 'bg-orange-100 text-orange-600' : 'bg-green-100 text-green-600'}">
                      <i class="pi {item.type === 'message' ? 'pi-envelope' : item.type === 'system' ? 'pi-cog' : 'pi-check-circle'} text-sm"></i>
                    </div>
                    <div class="flex-1 min-w-0">
                      <p class="text-sm font-medium text-gray-800 dark:text-white truncate">{item.title}</p>
                      <p class="text-xs text-gray-500 mt-0.5 truncate">{item.content}</p>
                      <p class="text-xs text-gray-400 mt-1">{item.time}</p>
                    </div>
                    {#if !item.read}
                      <span class="w-2 h-2 bg-[#409eff] rounded-full flex-shrink-0 mt-2"></span>
                    {/if}
                  </div>
                </div>
              {/each}
            </div>
            <div class="px-4 py-2 border-t border-[#ebeef5] dark:border-[#303030] text-center">
              <Button.Root class="text-sm text-[#409eff] hover:underline">查看全部通知</Button.Root>
            </div>
          </Popover.Content>
        </Popover.Portal>
        </Popover.Root>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            通知
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 暗黑模式切换 -->
      <Tooltip.Root>
        <Tooltip.Trigger asChild let:builder>
          <Button.Root
            builders={[builder]}
            onclick={() => settingsStore.setTheme(theme === 'dark' ? 'light' : 'dark')}
            class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
          >
            <i class="pi {theme === 'dark' ? 'pi-sun' : 'pi-moon'} text-[18px] text-[#5a5e66] dark:text-[#ccc]"></i>
          </Button.Root>
        </Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            {theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'}
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 语言切换 - 使用 Bits UI Dropdown Menu -->
      <Tooltip.Root>
        <DropdownMenu.Root>
          <Tooltip.Trigger asChild let:builder>
            <DropdownMenu.Trigger
              builders={[builder]}
              class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
            >
              <i class="pi pi-globe text-[18px] text-[#5a5e66] dark:text-[#ccc]"></i>
            </DropdownMenu.Trigger>
          </Tooltip.Trigger>

        <DropdownMenu.Portal>
          <DropdownMenu.Content
            class="w-[160px] bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg border border-[#ebeef5] dark:border-[#303030] z-[9999] overflow-hidden py-1"
            sideOffset={8}
            align="end"
          >
            {#each localeOptions as lang}
              <DropdownMenu.Item
                onclick={() => handleLanguageChange(lang)}
                class="w-full px-4 py-2.5 text-left hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors flex items-center gap-3 cursor-pointer outline-none {$locale === lang.value ? 'bg-[#409eff]/10 text-[#409eff]' : 'text-gray-700 dark:text-gray-300'}"
              >
                <span class="text-xl">{lang.icon}</span>
                <span class="text-sm">{lang.label}</span>
                {#if $locale === lang.value}
                  <i class="pi pi-check ml-auto text-[#409eff]"></i>
                {/if}
              </DropdownMenu.Item>
            {/each}
          </DropdownMenu.Content>
        </DropdownMenu.Portal>
        </DropdownMenu.Root>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            {translate('settings.language')}
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 全屏按钮 -->
      <Tooltip.Root>
        <Tooltip.Trigger asChild let:builder>
          <Button.Root
            builders={[builder]}
            onclick={toggleFullscreen}
            class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
          >
            <i class="pi {isFullscreen ? 'pi-window-minimize' : 'pi-window-maximize'} text-[18px] text-[#5a5e66] dark:text-[#ccc]"></i>
          </Button.Root>
        </Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            {isFullscreen ? '退出全屏' : '全屏'}
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 用户信息 - 使用 Bits UI Dropdown Menu -->
      <DropdownMenu.Root>
        <DropdownMenu.Trigger
          class="flex items-center h-[50px] px-3 hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
        >
          <img
            src={user?.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${user?.username || 'admin'}`}
            alt="avatar"
            class="w-[30px] h-[30px] rounded-full"
          />
          <span class="ml-2 text-[14px] text-[#606266] dark:text-[#ccc] hidden sm:inline">
            {user?.name || user?.username || '管理员'}
          </span>
          <i class="pi pi-angle-down ml-1 text-[12px] text-[#606266] dark:text-[#ccc]"></i>
        </DropdownMenu.Trigger>

        <DropdownMenu.Portal>
          <DropdownMenu.Content
            class="w-[180px] bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg border border-[#ebeef5] dark:border-[#303030] py-1 z-[9999] overflow-hidden"
            sideOffset={8}
            align="end"
          >
            <!-- 用户信息头部 -->
            <div class="px-4 py-3 border-b border-[#ebeef5] dark:border-[#303030]">
              <p class="text-sm font-medium text-gray-800 dark:text-white">{user?.name || '管理员'}</p>
              <p class="text-xs text-gray-500 mt-0.5">超级管理员</p>
            </div>

            <DropdownMenu.Item
              onclick={() => navigate('/profile')}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <i class="pi pi-user mr-3 text-[14px]"></i>
              {translate('menu.profile')}
            </DropdownMenu.Item>

            <DropdownMenu.Item
              onclick={() => navigate('/settings')}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <i class="pi pi-cog mr-3 text-[14px]"></i>
              {translate('menu.settings')}
            </DropdownMenu.Item>

            <DropdownMenu.Separator class="my-1 border-t border-[#ebeef5] dark:border-[#303030]" />

            <DropdownMenu.Item
              onclick={handleLogout}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#fef0f0] hover:text-[#f56c6c] transition-colors cursor-pointer outline-none"
            >
              <i class="pi pi-sign-out mr-3 text-[14px]"></i>
              {translate('common.close')}
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu.Portal>
      </DropdownMenu.Root>
    </div>
  </div>
</header>

<!-- 全局搜索弹窗 - 使用 Bits UI Dialog -->
<Dialog.Root open={searchVisible} onOpenChange={(open) => searchVisible = open}>
  <Dialog.Portal>
    <Dialog.Overlay class="fixed inset-0 z-50 bg-black/50" />
    <Dialog.Content class="fixed left-1/2 top-20 z-50 w-full max-w-xl -translate-x-1/2 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-2xl overflow-hidden">
      <form onsubmit={handleSearch}>
        <div class="flex items-center px-4 border-b border-gray-100 dark:border-gray-800">
          <i class="pi pi-search text-gray-400"></i>
          <input
            bind:this={searchInputRef}
            type="text"
            bind:value={searchQuery}
            placeholder="{translate('common.search')}..."
            class="flex-1 h-14 px-4 text-base bg-transparent border-0 outline-none text-gray-800 dark:text-white placeholder-gray-400"
          />
          <kbd class="hidden sm:inline-flex items-center px-2 py-1 text-xs text-gray-400 bg-gray-100 dark:bg-gray-800 rounded">
            ESC
          </kbd>
        </div>
      </form>
      <div class="p-4 max-h-[400px] overflow-y-auto">
        {#if searchQuery.trim()}
          {@const filteredMenus = getFlatMenus().filter(m => !m.hidden && m.path && m.label.toLowerCase().includes(searchQuery.toLowerCase()))}
          {#if filteredMenus.length === 0}
            <div class="text-center py-8 text-gray-400">
              <i class="pi pi-search text-3xl mb-2"></i>
              <p class="text-sm">{translate('common.noData')}</p>
            </div>
          {:else}
            <p class="text-xs text-gray-400 mb-3">
              {translate('common.search')} ({filteredMenus.length})
            </p>
            <div class="grid grid-cols-2 gap-2">
              {#each filteredMenus as item}
                {@const parent = getParentMenu(item.path!)}
                <Button.Root
                  onclick={() => { searchVisible = false; searchQuery = ''; navigate(item.path!); }}
                  class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
                >
                  <div class="w-8 h-8 rounded-lg bg-[#409eff]/10 flex items-center justify-center flex-shrink-0">
                    <i class="{item.icon} text-[#409eff] text-sm"></i>
                  </div>
                  <div class="flex flex-col min-w-0">
                    <span class="text-sm text-gray-700 dark:text-gray-300 truncate">{translate(item.label)}</span>
                    {#if parent}
                      <span class="text-xs text-gray-400 truncate">{translate(parent.label)}</span>
                    {/if}
                  </div>
                </Button.Root>
              {/each}
            </div>
          {/if}
        {:else}
          {@const allMenus = getFlatMenus().filter(m => !m.hidden && m.path)}
          <p class="text-xs text-gray-400 mb-3">{translate('common.more')}</p>
          <div class="grid grid-cols-2 gap-2">
            {#each allMenus as item}
              {@const parent = getParentMenu(item.path!)}
              <Button.Root
                onclick={() => { searchVisible = false; navigate(item.path!); }}
                class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
              >
                <div class="w-8 h-8 rounded-lg bg-[#409eff]/10 flex items-center justify-center flex-shrink-0">
                  <i class="{item.icon} text-[#409eff] text-sm"></i>
                </div>
                <div class="flex flex-col min-w-0">
                  <span class="text-sm text-gray-700 dark:text-gray-300 truncate">{translate(item.label)}</span>
                  {#if parent}
                    <span class="text-xs text-gray-400 truncate">{translate(parent.label)}</span>
                  {/if}
                </div>
              </Button.Root>
            {/each}
          </div>
        {/if}
      </div>
    </Dialog.Content>
  </Dialog.Portal>
</Dialog.Root>
