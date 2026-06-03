<script lang="ts">
  /**
   * 顶部导航栏组件 - 使用 Bits UI 优化
   * ============================================================
   *
   * 重构说明：
   * - 用户菜单：使用 Bits UI Dropdown Menu
   * - 语言切换：使用 Bits UI Dropdown Menu
   * - 命令面板：使用 Bits UI Dialog
   *
   * 优势：
   * - 自动焦点管理和键盘导航
   * - 完整的可访问性支持
   * - 自动定位和边界检测
   * - 移除手动的 clickOutside 逻辑
   */
  import { onMount } from 'svelte';
  import { DropdownMenu, Dialog, Button, Tooltip } from 'bits-ui';
  import {
    Menu,
    Home,
    Search,
    Sun,
    Moon,
    Globe,
    Maximize,
    Minimize,
    ChevronDown,
    Check,
    LogOut,
    RotateCw,
    UserCircle,
    Palette,
  } from 'lucide-svelte';
  import Icon from './Icon.svelte';
  import UserProfileModal from './UserProfileModal.svelte';
  import { authStore } from '../stores/authStore';
  import { settingsStore } from '../stores/settingsStore';
  import { currentPath, navigate, refreshCurrentRoute, routeNames } from '../stores/routerStore';
  import { modulesVersion } from '../config/app.modules';
  import { getParentMenu, getFlatMenus, getMenuByPath, type MenuItem } from '../config/menu';
  import { topVisitedPaths } from '../stores/visitHistoryStore';
  import {
    humanizeIdentifier,
    t,
    locale,
    resolveDynamicLabel,
    setLocale,
    localeOptions,
  } from '$lib/locales';
  import { getImageUrl } from '../utils/image';

  type CommandItem = {
    key: string;
    title: string;
    subtitle: string;
    icon: string;
    keywords: string;
    action: () => void | Promise<void>;
  };

  let { onToggle }: { onToggle: () => void } = $props();

  let searchVisible = $state(false);
  let profileVisible = $state(false);
  let searchQuery = $state('');
  let isFullscreen = $state(false);
  let searchInputRef: HTMLInputElement;

  // 获取翻译函数
  const translate = $derived($t);
  const user = $derived($authStore.user);
  const theme = $derived($settingsStore.theme);

  // 获取当前路由名称
  function getCurrentRouteLabel(path: string, version: number): string {
    void version;
    const menu = getMenuByPath(path);
    if (menu) {
      return getMenuTitle(menu);
    }
    return translate(routeNames[path] || 'menu.home');
  }

  function getCurrentParentMenu(path: string, version: number): MenuItem | undefined {
    void version;
    return getParentMenu(path);
  }

  const currentRoute = $derived(getCurrentRouteLabel($currentPath, $modulesVersion));
  const parentMenu = $derived(getCurrentParentMenu($currentPath, $modulesVersion));
  const avatarSrc = $derived(
    getImageUrl(user?.avatar) ||
      `https://api.dicebear.com/7.x/avataaars/svg?seed=${user?.username || 'admin'}`
  );
  let visibleCommands: CommandItem[] = $state([]);
  let recentCommands: CommandItem[] = $state([]);

  function getMenuTitle(item: MenuItem): string {
    return resolveDynamicLabel(
      item.label,
      item.labelI18n,
      $locale,
      translate,
      humanizeIdentifier(item.module || item.id || item.path)
    );
  }

  function getMenuSubtitle(item: MenuItem): string {
    if (!item.path) return translate('common.more');

    const parent = getParentMenu(item.path);
    return parent ? getMenuTitle(parent) : item.path;
  }

  function closeCommandPanel() {
    searchVisible = false;
    searchQuery = '';
  }

  function buildCommandItems(): CommandItem[] {
    const menuCommands = getFlatMenus()
      .filter(item => !item.hidden && item.path)
      .map(item => {
        const title = getMenuTitle(item);
        const subtitle = getMenuSubtitle(item);
        const path = item.path || '/';

        return {
          key: `menu:${path}`,
          title,
          subtitle,
          icon: item.icon?.replace('pi pi-', '') || 'file',
          keywords: `${title} ${subtitle} ${item.label} ${path}`.toLowerCase(),
          action: () => {
            navigate(path);
            closeCommandPanel();
          },
        };
      });

    return [
      {
        key: 'action:refresh',
        title: translate('tagsView.refresh'),
        subtitle: currentRoute,
        icon: 'refresh-cw',
        keywords: `${translate('tagsView.refresh')} refresh reload ${currentRoute}`.toLowerCase(),
        action: () => {
          refreshCurrentRoute();
          closeCommandPanel();
        },
      },
      {
        key: 'action:toggle-theme',
        title:
          theme === 'dark'
            ? translate('header.theme.switchToLight')
            : translate('header.theme.switchToDark'),
        subtitle: translate('menu.preferences'),
        icon: theme === 'dark' ? 'sun' : 'moon',
        keywords: `theme dark light ${translate('menu.preferences')}`.toLowerCase(),
        action: () => {
          settingsStore.setTheme(theme === 'dark' ? 'light' : 'dark');
          closeCommandPanel();
        },
      },
      {
        key: 'action:fullscreen',
        title: isFullscreen
          ? translate('header.fullscreen.exit')
          : translate('header.fullscreen.enter'),
        subtitle: currentRoute,
        icon: isFullscreen ? 'minimize' : 'maximize',
        keywords: `fullscreen full screen ${translate('header.fullscreen.enter')} ${translate(
          'header.fullscreen.exit'
        )}`.toLowerCase(),
        action: () => {
          toggleFullscreen();
          closeCommandPanel();
        },
      },
      {
        key: 'action:user-center',
        title: translate('menu.userCenter'),
        subtitle: user?.name || user?.username || translate('header.user.defaultName'),
        icon: 'user-circle',
        keywords:
          `${translate('menu.userCenter')} profile user center ${user?.name || ''} ${user?.username || ''}`.toLowerCase(),
        action: () => {
          profileVisible = true;
          closeCommandPanel();
        },
      },
      {
        key: 'action:settings',
        title: translate('menu.preferences'),
        subtitle: translate('common.more'),
        icon: 'palette',
        keywords: `${translate('menu.preferences')} preferences theme appearance`.toLowerCase(),
        action: () => {
          navigate('/preferences');
          closeCommandPanel();
        },
      },
      ...menuCommands,
      {
        key: 'action:logout',
        title: translate('common.logout'),
        subtitle: user?.name || user?.username || translate('header.user.defaultName'),
        icon: 'log-out',
        keywords: `${translate('common.logout')} logout sign out`.toLowerCase(),
        action: async () => {
          closeCommandPanel();
          await handleLogout();
        },
      },
    ];
  }

  function getFilteredCommands(): CommandItem[] {
    const query = searchQuery.trim().toLowerCase();
    const commands = buildCommandItems();
    if (!query) {
      return commands.slice(0, 10);
    }

    return commands.filter(command => command.keywords.includes(query)).slice(0, 12);
  }

  $effect(() => {
    searchVisible;
    searchQuery;
    currentRoute;
    $modulesVersion;
    user;
    visibleCommands = getFilteredCommands();
  });

  $effect(() => {
    $modulesVersion;
    const recentPaths = $topVisitedPaths
      .map(path => getFlatMenus().find(item => item.path === path))
      .filter((item): item is MenuItem => Boolean(item && !item.hidden && item.path));

    recentCommands = recentPaths.slice(0, 5).map(item => {
      const title = getMenuTitle(item);
      const subtitle = getMenuSubtitle(item);
      const path = item.path || '/';

      return {
        key: `recent:${path}`,
        title,
        subtitle,
        icon: item.icon?.replace('pi pi-', '') || 'file',
        keywords: `${title} ${subtitle} ${item.label} ${path}`.toLowerCase(),
        action: () => {
          navigate(path);
          closeCommandPanel();
        },
      };
    });
  });

  async function executeCommand(command: CommandItem) {
    await command.action();
  }

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
    const [firstCommand] = getFilteredCommands();
    if (firstCommand) {
      executeCommand(firstCommand);
    }
  }

  function handleLanguageChange(lang: (typeof localeOptions)[0]) {
    setLocale(lang.value);
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
  class="sticky top-0 z-30 h-[50px] border-b border-[var(--color-border)] bg-[var(--color-bg-surface)]"
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
          class="flex items-center text-[#97a8be] hover:text-[color:var(--color-primary)] cursor-pointer transition-colors"
        >
          <Home size={12} class="mr-1" />
          {translate('menu.home')}
        </Button.Root>
        {#if $currentPath !== '/'}
          {#if parentMenu}
            <span class="mx-2 text-[#97a8be]">/</span>
            <span class="text-[#97a8be]">
              {getMenuTitle(parentMenu)}
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
                  ? 'bg-[color:var(--color-primary-subtle)] text-[color:var(--color-primary)]'
                  : 'text-gray-700 dark:text-gray-300'}"
              >
                <span class="text-xl">{lang.icon}</span>
                <span class="text-sm">{lang.label}</span>
                {#if $locale === lang.value}
                  <Check size={14} class="ml-auto" style="color: var(--color-primary);" />
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
          <img src={avatarSrc} alt="avatar" class="w-[30px] h-[30px] rounded-full" />
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
              onclick={() => (profileVisible = true)}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[color:var(--color-primary)] transition-colors cursor-pointer outline-none"
            >
              <UserCircle size={14} class="mr-3" />
              {translate('menu.userCenter')}
            </DropdownMenu.Item>

            <DropdownMenu.Item
              onclick={() => navigate('/preferences')}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[color:var(--color-primary)] transition-colors cursor-pointer outline-none"
            >
              <Palette size={14} class="mr-3" />
              {translate('menu.preferences')}
            </DropdownMenu.Item>

            <DropdownMenu.Separator class="my-1 border-t border-[#ebeef5] dark:border-[#303030]" />

            <DropdownMenu.Item
              onclick={handleLogout}
              class="w-full flex items-center px-4 py-2.5 text-[14px] text-[#606266] dark:text-[#ccc] hover:bg-[#fef0f0] hover:text-[#f56c6c] transition-colors cursor-pointer outline-none"
            >
              <LogOut size={14} class="mr-3" />
              {translate('common.logout')}
            </DropdownMenu.Item>
          </DropdownMenu.Content>
        </DropdownMenu.Portal>
      </DropdownMenu.Root>
    </div>
  </div>
</header>

<UserProfileModal open={profileVisible} onOpenChange={open => (profileVisible = open)} />

<!-- 全局搜索弹窗 - 使用 Bits UI Dialog -->
<Dialog.Root open={searchVisible} onOpenChange={open => (searchVisible = open)}>
  <Dialog.Portal>
    <Dialog.Overlay class="fixed inset-0 z-50 bg-black/50" />
    <Dialog.Content
      class="fixed left-1/2 top-20 z-50 w-full max-w-xl -translate-x-1/2 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-2xl overflow-hidden data-[state=open]:animate-[slideDown_0.2s_ease-out] data-[state=closed]:animate-[slideUp_0.15s_ease-in]"
    >
      <form onsubmit={handleSearch}>
        <div
          class="flex items-center px-4 border-b border-gray-100 dark:border-gray-800 focus-within:border-[color:var(--color-primary)] transition-colors"
        >
          <Search size={18} class="text-gray-400 shrink-0" />
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
      <div class="p-4 max-h-[420px] overflow-y-auto">
        {#if !searchQuery.trim() && recentCommands.length > 0}
          <div class="mb-4">
            <div class="mb-2 text-xs font-medium text-gray-400">
              {$t('dashboard.recentActivity')}
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {#each recentCommands as command}
                <Button.Root
                  onclick={() => executeCommand(command)}
                  class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
                >
                  <div
                    class="w-8 h-8 rounded-lg bg-[#409eff]/10 flex items-center justify-center flex-shrink-0"
                  >
                    <Icon name={command.icon} size={14} style="color: var(--color-primary);" />
                  </div>
                  <div class="flex flex-col min-w-0">
                    <span class="text-sm text-gray-700 dark:text-gray-300 truncate"
                      >{command.title}</span
                    >
                    <span class="text-xs text-gray-400 truncate">{command.subtitle}</span>
                  </div>
                </Button.Root>
              {/each}
            </div>
          </div>
        {/if}
        {#if visibleCommands.length === 0}
          <div class="text-center py-8 text-gray-400">
            <Search size={32} class="mx-auto mb-2" />
            <p class="text-sm">{translate('common.noData')}</p>
          </div>
        {:else}
          <p class="text-xs text-gray-400 mb-3">
            {searchQuery.trim() ? translate('common.search') : translate('common.more')} ({visibleCommands.length})
          </p>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {#each visibleCommands as command}
              <Button.Root
                onclick={() => executeCommand(command)}
                class="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors text-left"
              >
                <div
                  class="w-8 h-8 rounded-lg bg-[#409eff]/10 flex items-center justify-center flex-shrink-0"
                >
                  {#if command.key === 'action:refresh'}
                    <RotateCw size={14} style="color: var(--color-primary);" />
                  {:else}
                    <Icon name={command.icon} size={14} style="color: var(--color-primary);" />
                  {/if}
                </div>
                <div class="flex flex-col min-w-0">
                  <span class="text-sm text-gray-700 dark:text-gray-300 truncate"
                    >{command.title}</span
                  >
                  <span class="text-xs text-gray-400 truncate">{command.subtitle}</span>
                </div>
              </Button.Root>
            {/each}
          </div>
        {/if}
      </div>
    </Dialog.Content>
  </Dialog.Portal>
</Dialog.Root>
