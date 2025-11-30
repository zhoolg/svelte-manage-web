<script lang="ts">
  /**
   * 侧边栏组件 - 使用 Bits UI Collapsible + 权限控制
   * ============================================================
   *
   * 重构说明：
   * - 从自定义折叠动画迁移到 Bits UI Collapsible
   * - 添加基于权限的菜单项显示控制
   * - 保持原有样式和功能
   * - 提升可访问性和键盘导航
   */
  import { Collapsible } from 'bits-ui';
  import { menuConfig, hasActiveChild, type MenuItem } from '../config/menu';
  import { APP_CONFIG } from '../config';
  import { currentPath, navigate } from '../stores/routerStore';
  import { t } from '../lib/locales';
  import { permissionStore } from '../stores/permissionStore';

  export let collapsed: boolean = false;

  // 展开的菜单 key 列表
  let openKeys: string[] = [];

  // 初始化和路由变化时更新展开状态
  $: {
    const path = $currentPath;
    menuConfig.forEach((menu) => {
      if (menu.children && hasActiveChild(menu, path)) {
        if (!openKeys.includes(menu.label)) {
          openKeys = [...openKeys, menu.label];
        }
      }
    });
  }

  // 切换菜单展开状态
  function toggleMenu(key: string) {
    openKeys = openKeys.includes(key)
      ? openKeys.filter((k) => k !== key)
      : [...openKeys, key];
  }

  // 检查是否展开
  function isOpen(key: string): boolean {
    return openKeys.includes(key);
  }

  /**
   * 检查菜单项是否有权限访问
   * @param item 菜单项
   */
  function hasMenuPermission(item: MenuItem): boolean {
    // 如果菜单项没有配置权限，默认可以访问
    if (!item.permissions && !item.roles) {
      return true;
    }

    // 检查权限
    if (item.permissions && item.permissions.length > 0) {
      if (permissionStore.hasAnyPermission(item.permissions as string[])) {
        return true;
      }
    }

    // 检查角色
    if (item.roles && item.roles.length > 0) {
      if (item.roles.some(role => permissionStore.hasRole(role))) {
        return true;
      }
    }

    return false;
  }

  /**
   * 检查父菜单是否有任何可见的子菜单
   * @param item 父菜单项
   */
  function hasVisibleChildren(item: MenuItem): boolean {
    if (!item.children || item.children.length === 0) {
      return false;
    }
    return item.children.some(child => !child.hidden && hasMenuPermission(child));
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
          <i class="pi {APP_CONFIG.logoIcon} text-white text-[16px]"></i>
        </div>
        {#if !collapsed}
          <h1 class="text-[#fff] text-[14px] font-semibold whitespace-nowrap overflow-hidden">
            {APP_CONFIG.shortTitle}
          </h1>
        {/if}
      </div>
    </div>

    <!-- 菜单区域 -->
    <nav class="flex-1 scrollbar-thin {collapsed ? 'overflow-visible' : 'overflow-y-auto overflow-x-hidden'}">
      <ul class="py-1">
        {#each menuConfig as item}
          {#if !item.hidden && (hasMenuPermission(item) || hasVisibleChildren(item))}
            {#if item.children && item.children.length > 0 && hasVisibleChildren(item)}
              <!-- 有子菜单的父级菜单 - 使用 Bits UI Collapsible -->
              <li class={collapsed ? 'group relative' : ''}>
                <Collapsible.Root open={isOpen(item.label)} onOpenChange={() => !collapsed && toggleMenu(item.label)}>
                  <!-- 父级菜单按钮 -->
                  <Collapsible.Trigger
                    class="w-full flex items-center h-[50px] text-[14px] transition-colors {collapsed ? 'justify-center px-0' : 'justify-between px-5'} {hasActiveChild(item, $currentPath) ? 'text-white bg-[#263445]' : 'text-[#bfcbd9] hover:bg-[#263445]'}"
                    title={collapsed ? $t(item.label) : undefined}
                  >
                    <div class="flex items-center {collapsed ? 'justify-center w-full' : ''}">
                      <i class="{item.icon} text-[18px] {collapsed ? '' : 'mr-3'}"></i>
                      {#if !collapsed}
                        <span>{$t(item.label)}</span>
                      {/if}
                    </div>
                    {#if !collapsed}
                      <i class="pi pi-angle-down text-[12px] transition-transform duration-200 {isOpen(item.label) ? 'rotate-180' : ''}"></i>
                    {/if}
                  </Collapsible.Trigger>

                  <!-- 子菜单（非折叠状态） - 使用 Collapsible.Content -->
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
                                onclick={() => navigate(child.path!)}
                                class="w-full flex items-center h-[50px] text-[14px] transition-colors {$currentPath === child.path ? 'bg-[#409eff] text-white' : 'text-[#bfcbd9] hover:bg-[#263445]'}"
                                style="padding-left: 52px; padding-right: 20px"
                              >
                                <i class="{child.icon} text-[18px] mr-3"></i>
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
                  <div class="absolute left-full top-0 hidden group-hover:block z-50 pointer-events-none">
                    <div
                      class="py-2 min-w-[180px] rounded-lg shadow-xl border border-[#263445] pointer-events-auto"
                      style="background: #304156"
                    >
                      <div class="px-4 py-2.5 text-[12px] text-[#909399] border-b border-[#263445] font-medium">
                        {$t(item.label)}
                      </div>
                      {#each item.children as child}
                        {#if !child.hidden && child.path && hasMenuPermission(child)}
                          <button
                            type="button"
                            onclick={() => navigate(child.path!)}
                            class="w-full flex items-center px-4 py-3 text-[13px] transition-all {$currentPath === child.path ? 'bg-[#409eff] text-white' : 'text-[#bfcbd9] hover:bg-[#263445] hover:pl-5'}"
                          >
                            <i class="{child.icon} text-[16px] mr-3"></i>
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
                  onclick={() => navigate(item.path!)}
                  class="w-full flex items-center h-[50px] text-[14px] transition-colors {collapsed ? 'justify-center' : ''} {$currentPath === item.path ? 'bg-[#409eff] text-white' : 'text-[#bfcbd9] hover:bg-[#263445]'}"
                  style="padding-left: {collapsed ? 0 : 20}px; padding-right: {collapsed ? 0 : 20}px"
                  title={collapsed ? $t(item.label) : undefined}
                >
                  <i class="{item.icon} text-[18px] {collapsed ? '' : 'mr-3'}"></i>
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
