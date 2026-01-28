<script lang="ts">
  /**
   * 侧边栏组件
   * @zhoolg/svelte-admin-framework
   */
  import { Collapsible } from 'bits-ui';
  import { ChevronDown } from 'lucide-svelte';
  import { getI18n } from '../locales';
  import { permissionStore } from '../stores/permissionStore';
  import Icon from './Icon.svelte';

  export interface MenuItem {
    path?: string;
    label: string;
    icon?: string;
    hidden?: boolean;
    children?: MenuItem[];
    permissions?: string[];
    roles?: string[];
  }

  interface Props {
    /** 是否折叠 */
    collapsed?: boolean;
    /** 菜单配置 */
    menuConfig: MenuItem[];
    /** 当前路径 */
    currentPath: string;
    /** 导航函数 */
    onNavigate: (path: string) => void;
    /** 应用标题 */
    appTitle?: string;
    /** Logo 图标 */
    logoIcon?: string;
  }

  let {
    collapsed = false,
    menuConfig,
    currentPath,
    onNavigate,
    appTitle = '管理系统',
    logoIcon = 'briefcase',
  }: Props = $props();

  const { t } = getI18n();

  // 展开的菜单 key 列表
  let openKeys = $state<string[]>([]);

  // 检查是否有激活的子菜单
  function hasActiveChild(item: MenuItem, path: string): boolean {
    if (!item.children) return false;
    return item.children.some(
      (child) => child.path === path || (child.children && hasActiveChild(child, path))
    );
  }

  // 初始化和路由变化时更新展开状态
  $effect(() => {
    const path = currentPath;
    menuConfig.forEach((menu) => {
      if (menu.children && hasActiveChild(menu, path)) {
        if (!openKeys.includes(menu.label)) {
          openKeys = [...openKeys, menu.label];
        }
      }
    });
  });

  function toggleMenu(key: string) {
    openKeys = openKeys.includes(key) ? openKeys.filter((k) => k !== key) : [...openKeys, key];
  }

  function isOpen(key: string): boolean {
    return openKeys.includes(key);
  }

  function hasMenuPermission(item: MenuItem): boolean {
    if (!item.permissions && !item.roles) {
      return true;
    }

    if (item.permissions && item.permissions.length > 0) {
      if (permissionStore.hasAnyPermission(item.permissions)) {
        return true;
      }
    }

    if (item.roles && item.roles.length > 0) {
      if (item.roles.some((role) => permissionStore.hasRole(role))) {
        return true;
      }
    }

    return false;
  }

  function hasVisibleChildren(item: MenuItem): boolean {
    if (!item.children || item.children.length === 0) {
      return false;
    }
    return item.children.some((child) => !child.hidden && hasMenuPermission(child));
  }
</script>

<aside
  class="fixed top-0 left-0 h-screen transition-all duration-300 z-40"
  class:w-[54px]={collapsed}
  class:w-[210px]={!collapsed}
  style="background: #304156"
>
  <div class="flex flex-col h-full">
    <!-- Logo 区域 -->
    <div class="h-[50px] flex items-center px-4 overflow-hidden border-b border-[#263445]">
      <div class="flex items-center gap-2">
        <div class="w-8 h-8 bg-[#409eff] rounded flex items-center justify-center flex-shrink-0">
          <Icon name={logoIcon} size={16} class="text-white" />
        </div>
        {#if !collapsed}
          <h1 class="text-[#fff] text-[14px] font-semibold whitespace-nowrap overflow-hidden">
            {appTitle}
          </h1>
        {/if}
      </div>
    </div>

    <!-- 菜单区域 -->
    <nav
      class="flex-1 scrollbar-thin {collapsed
        ? 'overflow-visible'
        : 'overflow-y-auto overflow-x-hidden'}"
    >
      <ul class="py-1">
        {#each menuConfig as item}
          {#if !item.hidden && (hasMenuPermission(item) || hasVisibleChildren(item))}
            {#if item.children && item.children.length > 0 && hasVisibleChildren(item)}
              <!-- 有子菜单的父级菜单 -->
              <li class={collapsed ? 'group relative' : ''}>
                <Collapsible.Root
                  open={isOpen(item.label)}
                  onOpenChange={() => !collapsed && toggleMenu(item.label)}
                >
                  <Collapsible.Trigger
                    class="w-full flex items-center h-[50px] text-[14px] transition-colors {collapsed
                      ? 'justify-center px-0'
                      : 'justify-between px-5'} {hasActiveChild(item, currentPath)
                      ? 'text-white bg-[#263445]'
                      : 'text-[#bfcbd9] hover:bg-[#263445]'}"
                    title={collapsed ? $t(item.label) : undefined}
                  >
                    <div class="flex items-center {collapsed ? 'justify-center w-full' : ''}">
                      <Icon name={item.icon || 'file'} size={18} class={collapsed ? '' : 'mr-3'} />
                      {#if !collapsed}
                        <span>{$t(item.label)}</span>
                      {/if}
                    </div>
                    {#if !collapsed}
                      <ChevronDown
                        size={12}
                        class="transition-transform duration-200 {isOpen(item.label)
                          ? 'rotate-180'
                          : ''}"
                      />
                    {/if}
                  </Collapsible.Trigger>

                  {#if !collapsed}
                    <Collapsible.Content
                      class="overflow-hidden data-[state=open]:animate-collapsible-down data-[state=closed]:animate-collapsible-up"
                      style="background: #1f2d3d"
                    >
                      <ul>
                        {#each item.children as child}
                          {#if !child.hidden && child.path && hasMenuPermission(child)}
                            <li>
                              <button
                                type="button"
                                onclick={() => onNavigate(child.path!)}
                                class="w-full flex items-center h-[50px] text-[14px] transition-colors {currentPath ===
                                child.path
                                  ? 'bg-[#409eff] text-white'
                                  : 'text-[#bfcbd9] hover:bg-[#263445]'}"
                                style="padding-left: 52px; padding-right: 20px"
                              >
                                <Icon name={child.icon || 'file'} size={18} class="mr-3" />
                                <span>{$t(child.label)}</span>
                              </button>
                            </li>
                          {/if}
                        {/each}
                      </ul>
                    </Collapsible.Content>
                  {/if}
                </Collapsible.Root>

                <!-- 折叠状态下的悬浮子菜单 -->
                {#if collapsed}
                  <div
                    class="absolute left-full top-0 hidden group-hover:block z-50 pointer-events-none"
                  >
                    <div
                      class="py-2 min-w-[180px] rounded-lg shadow-xl border border-[#263445] pointer-events-auto"
                      style="background: #304156"
                    >
                      <div
                        class="px-4 py-2.5 text-[12px] text-[#909399] border-b border-[#263445] font-medium"
                      >
                        {$t(item.label)}
                      </div>
                      {#each item.children as child}
                        {#if !child.hidden && child.path && hasMenuPermission(child)}
                          <button
                            type="button"
                            onclick={() => onNavigate(child.path!)}
                            class="w-full flex items-center px-4 py-3 text-[13px] transition-all {currentPath ===
                            child.path
                              ? 'bg-[#409eff] text-white'
                              : 'text-[#bfcbd9] hover:bg-[#263445] hover:pl-5'}"
                          >
                            <Icon name={child.icon || 'file'} size={16} class="mr-3" />
                            <span>{$t(child.label)}</span>
                          </button>
                        {/if}
                      {/each}
                    </div>
                  </div>
                {/if}
              </li>
            {:else if item.path && hasMenuPermission(item)}
              <!-- 普通菜单项 -->
              <li>
                <button
                  type="button"
                  onclick={() => onNavigate(item.path!)}
                  class="w-full flex items-center h-[50px] text-[14px] transition-colors {collapsed
                    ? 'justify-center'
                    : ''} {currentPath === item.path
                    ? 'bg-[#409eff] text-white'
                    : 'text-[#bfcbd9] hover:bg-[#263445]'}"
                  style="padding-left: {collapsed ? 0 : 20}px; padding-right: {collapsed ? 0 : 20}px"
                  title={collapsed ? $t(item.label) : undefined}
                >
                  <Icon name={item.icon || 'file'} size={18} class={collapsed ? '' : 'mr-3'} />
                  {#if !collapsed}
                    <span>{$t(item.label)}</span>
                  {/if}
                </button>
              </li>
            {/if}
          {/if}
        {/each}
      </ul>
    </nav>
  </div>
</aside>
