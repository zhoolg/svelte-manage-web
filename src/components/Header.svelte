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
  import {
    Menu,
    Home,
    Search,
    Bell,
    BellOff,
    Sun,
    Moon,
    Globe,
    Maximize,
    Minimize,
    ChevronDown,
    Check,
    Settings,
    LogOut,
  } from 'lucide-svelte';
  import Icon from './Icon.svelte';
  import { authStore } from '../stores/authStore';
  import { settingsStore } from '../stores/settingsStore';
  import { currentPath, navigate, routeNames } from '../stores/routerStore';
  import { getParentMenu, getFlatMenus } from '../config/menu';
  import { t, locale, setLocale, localeOptions } from '$lib/locales';

  let { onToggle }: { onToggle: () => void } = $props();

  let searchVisible = $state(false);
  let searchQuery = $state('');
  let isFullscreen = $state(false);
  let searchInputRef: HTMLInputElement;

  // 获取翻译函数
  const translate = $derived($t);
  const user = $derived($authStore.user);
  const theme = $derived($settingsStore.theme);

  // 获取当前路由名称
  const currentRoute = $derived($t(routeNames[$currentPath] || 'menu.home'));
  const parentMenu = $derived(getParentMenu($currentPath));

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

  function handleLanguageChange(lang: (typeof localeOptions)[0]) {
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
  $effect(() => {
    if (searchVisible && searchInputRef) {
      setTimeout(() => searchInputRef?.focus(), 0);
    }
  });
</script>

<header
  class="sticky top-0 z-30 bg-white dark:bg-[#141414] h-[50px] border-b border-[#ebeef5] dark:border-[#303030]"
>
  <div class="flex items-center justify-between h-full px-4">
    <!-- 左侧：折叠按钮 + 面包屑 -->
    <div class="flex items-center">
      <Button.Root
        onclick={onToggle}
        class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
        aria-label="Toggle Menu"
      >
        <Menu size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
      </Button.Root>

      <!-- 面包屑导航 -->
      <nav class="flex items-center ml-4 text-[14px]">
        <Button.Root
          onclick={() => navigate('/')}
          class="flex items-center text-[#97a8be] hover:text-[#409eff] cursor-pointer transition-colors"
        >
          <Home size={12} class="mr-1" />
          {translate('menu.home')}
        </Button.Root>
        {#if $currentPath !== '/'}
          {#if parentMenu}
            <span class="mx-2 text-[#97a8be]">/</span>
            <span class="text-[#97a8be]">
              {parentMenu.label.startsWith('menu.')
                ? translate(parentMenu.label)
                : parentMenu.label}
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
        <Tooltip.Trigger
          onclick={() => (searchVisible = true)}
          class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
        >
          <Search size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
        </Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            {translate('header.searchShortcut')}
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 通知按钮 - 使用 Bits UI Popover -->
      <Popover.Root>
        <Tooltip.Root>
          <Tooltip.Trigger
            class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors relative"
          >
            <Popover.Trigger class="w-full h-full flex items-center justify-center">
              <Bell size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
            </Popover.Trigger>
          </Tooltip.Trigger>
          <Tooltip.Portal>
            <Tooltip.Content
              class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
              sideOffset={5}
            >
              {translate('header.notifications.title')}
            </Tooltip.Content>
          </Tooltip.Portal>
        </Tooltip.Root>

        <Popover.Portal>
          <Popover.Content
            class="w-[320px] bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg border border-[#ebeef5] dark:border-[#303030] z-[9999] overflow-hidden"
            sideOffset={8}
            align="end"
          >
            <div class="px-4 py-3 border-b border-[#ebeef5] dark:border-[#303030]">
              <span class="font-medium text-gray-800 dark:text-white"
                >{translate('header.notifications.title')}</span
              >
            </div>
            <div class="px-4 py-8 text-center">
              <BellOff size={36} class="text-gray-300 dark:text-gray-600 mx-auto mb-2" />
              <p class="text-sm text-gray-400">{translate('header.notifications.empty')}</p>
            </div>
          </Popover.Content>
        </Popover.Portal>
      </Popover.Root>

      <!-- 暗黑模式切换 -->
      <Tooltip.Root>
        <Tooltip.Trigger
          onclick={() => settingsStore.setTheme(theme === 'dark' ? 'light' : 'dark')}
          class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
        >
          {#if theme === 'dark'}
            <Sun size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
          {:else}
            <Moon size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
          {/if}
        </Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            {theme === 'dark'
              ? translate('header.theme.switchToLight')
              : translate('header.theme.switchToDark')}
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 语言切换 - 使用 Bits UI Dropdown Menu -->
      <DropdownMenu.Root>
        <Tooltip.Root>
          <Tooltip.Trigger
            class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
          >
            <DropdownMenu.Trigger class="w-full h-full flex items-center justify-center">
              <Globe size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
            </DropdownMenu.Trigger>
          </Tooltip.Trigger>
          <Tooltip.Portal>
            <Tooltip.Content
              class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
              sideOffset={5}
            >
              {translate('settings.language')}
            </Tooltip.Content>
          </Tooltip.Portal>
        </Tooltip.Root>

        <DropdownMenu.Portal>
          <DropdownMenu.Content
            class="w-[160px] bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg border border-[#ebeef5] dark:border-[#303030] z-[9999] overflow-hidden py-1"
            sideOffset={8}
            align="end"
          >
            {#each localeOptions as lang}
              <DropdownMenu.Item
                onclick={() => handleLanguageChange(lang)}
                class="w-full px-4 py-2.5 text-left hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors flex items-center gap-3 cursor-pointer outline-none {$locale ===
                lang.value
                  ? 'bg-[#409eff]/10 text-[#409eff]'
                  : 'text-gray-700 dark:text-gray-300'}"
              >
                <span class="text-xl">{lang.icon}</span>
                <span class="text-sm">{lang.label}</span>
                {#if $locale === lang.value}
                  <Check size={14} class="ml-auto text-[#409eff]" />
                {/if}
              </DropdownMenu.Item>
            {/each}
          </DropdownMenu.Content>
        </DropdownMenu.Portal>
      </DropdownMenu.Root>

      <!-- 全屏按钮 -->
      <Tooltip.Root>
        <Tooltip.Trigger
          onclick={toggleFullscreen}
          class="w-[40px] h-[50px] flex items-center justify-center hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
        >
          {#if isFullscreen}
            <Minimize size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
          {:else}
            <Maximize size={18} class="text-[#5a5e66] dark:text-[#ccc]" />
          {/if}
        </Tooltip.Trigger>
        <Tooltip.Portal>
          <Tooltip.Content
            class="px-3 py-1.5 text-xs bg-gray-900 dark:bg-gray-700 text-white rounded shadow-lg z-[9999]"
            sideOffset={5}
          >
            {isFullscreen
              ? translate('header.fullscreen.exit')
              : translate('header.fullscreen.enter')}
          </Tooltip.Content>
        </Tooltip.Portal>
      </Tooltip.Root>

      <!-- 用户信息 - 使用 Bits UI Dropdown Menu -->
      <DropdownMenu.Root>
        <DropdownMenu.Trigger
          class="flex items-center h-[50px] px-3 hover:bg-[#f6f6f6] dark:hover:bg-[#262626] transition-colors"
        >
          <img
            src={user?.avatar ||
              `https://api.dicebear.com/7.x/avataaars/svg?seed=${user?.username || 'admin'}`}
            alt="avatar"
            class="w-[30px] h-[30px] rounded-full"
          />
          <span class="ml-2 text-[14px] text-[#606266] dark:text-[#ccc] hidden sm:inline">
            {user?.name || user?.username || translate('header.user.defaultName')}
          </span>
          <ChevronDown size={12} class="ml-1 text-[#606266] dark:text-[#ccc]" />
        </DropdownMenu.Trigger>

        <DropdownMenu.Portal>
          <DropdownMenu.Content
            class="w-[180px] bg-white dark:bg-[#1d1d1d] rounded-lg shadow-lg border border-[#ebeef5] dark:border-[#303030] py-1 z-[9999] overflow-hidden"
            sideOffset={8}
            align="end"
          >
            <DropdownMenu.Item
              onclick={() => navigate('/settings')}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <Settings size={14} class="mr-3" />
              {translate('menu.settings')}
            </DropdownMenu.Item>

            <DropdownMenu.Separator class="my-1 border-t border-[#ebeef5] dark:border-[#303030]" />

            <DropdownMenu.Item
              onclick={handleLogout}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#fef0f0] hover:text-[#f56c6c] transition-colors cursor-pointer outline-none"
            >
              <LogOut size={14} class="mr-3" />
              {translate('common.close')}
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu.Portal>
      </DropdownMenu.Root>
    </div>
  </div>
</header>

<!-- 全局搜索弹窗 - 使用 Bits UI Dialog -->
<Dialog.Root open={searchVisible} onOpenChange={open => (searchVisible = open)}>
  <Dialog.Portal>
    <Dialog.Overlay class="fixed inset-0 z-50 bg-black/50" />
    <Dialog.Content
      class="fixed left-1/2 top-20 z-50 w-full max-w-xl -translate-x-1/2 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-2xl overflow-hidden"
    >
      <form onsubmit={handleSearch}>
        <div class="flex items-center px-4 border-b border-gray-100 dark:border-gray-800">
          <Search size={18} class="text-gray-400" />
          <input
            bind:this={searchInputRef}
            type="text"
            bind:value={searchQuery}
            placeholder="{translate('common.search')}..."
            class="flex-1 h-14 px-4 text-base bg-transparent border-0 outline-none text-gray-800 dark:text-white placeholder-gray-400"
          />
          <kbd
            class="hidden sm:inline-flex items-center px-2 py-1 text-xs text-gray-400 bg-gray-100 dark:bg-gray-800 rounded"
          >
            ESC
          </kbd>
        </div>
      </form>
      <div class="p-4 max-h-[400px] overflow-y-auto">
        {#if searchQuery.trim()}
          {@const filteredMenus = getFlatMenus().filter(
            m => !m.hidden && m.path && m.label.toLowerCase().includes(searchQuery.toLowerCase())
          )}
          {#if filteredMenus.length === 0}
            <div class="text-center py-8 text-gray-400">
              <Search size={32} class="mx-auto mb-2" />
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
                  onclick={() => {
                    searchVisible = false;
                    searchQuery = '';
                    navigate(item.path!);
                  }}
                  class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
                >
                  <div
                    class="w-8 h-8 rounded-lg bg-[#409eff]/10 flex items-center justify-center flex-shrink-0"
                  >
                    <Icon
                      name={item.icon?.replace('pi pi-', '') || 'file'}
                      size={14}
                      class="text-[#409eff]"
                    />
                  </div>
                  <div class="flex flex-col min-w-0">
                    <span class="text-sm text-gray-700 dark:text-gray-300 truncate"
                      >{translate(item.label)}</span
                    >
                    {#if parent}
                      <span class="text-xs text-gray-400 truncate"
                        >{parent.label.startsWith('menu.')
                          ? translate(parent.label)
                          : parent.label}</span
                      >
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
                onclick={() => {
                  searchVisible = false;
                  navigate(item.path!);
                }}
                class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
              >
                <div
                  class="w-8 h-8 rounded-lg bg-[#409eff]/10 flex items-center justify-center flex-shrink-0"
                >
                  <Icon
                    name={item.icon?.replace('pi pi-', '') || 'file'}
                    size={14}
                    class="text-[#409eff]"
                  />
                </div>
                <div class="flex flex-col min-w-0">
                  <span class="text-sm text-gray-700 dark:text-gray-300 truncate"
                    >{translate(item.label)}</span
                  >
                  {#if parent}
                    <span class="text-xs text-gray-400 truncate"
                      >{parent.label.startsWith('menu.')
                        ? translate(parent.label)
                        : parent.label}</span
                    >
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
