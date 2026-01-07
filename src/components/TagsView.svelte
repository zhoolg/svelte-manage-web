<script lang="ts">
  /**
   * 标签页导航组件 - 使用 Bits UI 优化
   * ============================================================
   *
   * 重构说明：
   * - 右键菜单：使用 Bits UI ContextMenu
   *
   * 优势：
   * - 自动焦点管理和键盘导航
   * - 完整的可访问性支持
   * - 自动定位和边界检测
   * - 移除手动的 clickOutside 逻辑
   */
  import { ContextMenu } from 'bits-ui';
  import { currentPath, navigate, routeNames } from '../stores/routerStore';
  import { t } from '../lib/locales';

  interface Tag {
    path: string;
    title: string;
    closable: boolean;
  }

  let tags: Tag[] = [
    { path: '/', title: 'menu.home', closable: false },
  ];

  let contextMenuPath = '';

  // 监听路由变化，添加新标签
  $: {
    const path = $currentPath;
    // 去掉查询参数,只保留路径部分
    const cleanPath = path.split('?')[0].split('#')[0];
    const titleKey = routeNames[cleanPath];

    if (titleKey && !tags.find(tag => tag.path === path)) {
      tags = [...tags, { path, title: titleKey, closable: path !== '/' }];
    }
  }

  function handleClose(path: string, e: MouseEvent) {
    e.stopPropagation();
    const newTags = tags.filter(tag => tag.path !== path);
    tags = newTags;

    if ($currentPath === path) {
      navigate(newTags[newTags.length - 1].path);
    }
  }

  function handleRefresh() {
    window.location.reload();
  }

  function handleCloseCurrent() {
    if (contextMenuPath !== '/') {
      const newTags = tags.filter(tag => tag.path !== contextMenuPath);
      tags = newTags;
      if ($currentPath === contextMenuPath) {
        navigate(newTags[newTags.length - 1].path);
      }
    }
  }

  function handleCloseOthers() {
    const currentTag = tags.find(tag => tag.path === contextMenuPath);
    const homeTag = tags.find(tag => tag.path === '/');
    const newTags = [homeTag!, currentTag!].filter((tag, index, self) =>
      tag && self.findIndex(t => t?.path === tag.path) === index
    );
    tags = newTags;
    if (!newTags.find(tag => tag.path === $currentPath)) {
      navigate(contextMenuPath);
    }
  }

  function handleCloseAll() {
    tags = [{ path: '/', title: 'menu.home', closable: false }];
    navigate('/');
  }
</script>

<div class="h-[34px] bg-white dark:bg-[#141414] border-b border-[#d8dce5] dark:border-[#303030] flex items-center px-[10px] shadow-sm">
  <div class="flex-1 flex items-center gap-[4px] overflow-x-auto scrollbar-hide">
    {#each tags as tag}
      <!-- 使用 Bits UI ContextMenu 包裹每个标签 -->
      <ContextMenu.Root onOpenChange={() => contextMenuPath = tag.path}>
        <ContextMenu.Trigger
          onclick={() => navigate(tag.path)}
          class="group flex items-center gap-[6px] px-[8px] h-[26px] border rounded-[3px] cursor-pointer text-[12px] transition-all select-none {$currentPath === tag.path ? 'bg-[#409eff] text-white border-[#409eff]' : 'bg-white dark:bg-[#1d1d1d] text-[#495060] dark:text-[#ccc] border-[#e6e6e6] dark:border-[#303030] hover:border-[#409eff]'}"
        >
          {#if $currentPath === tag.path}
            <span class="w-[8px] h-[8px] bg-white rounded-full"></span>
          {/if}
          <span>{$t(tag.title)}</span>
          {#if tag.closable}
            <button
              onclick={(e) => handleClose(tag.path, e)}
              class="w-[16px] h-[16px] rounded-full flex items-center justify-center transition-colors {$currentPath === tag.path ? 'hover:bg-white/20' : 'hover:bg-[#409eff] hover:text-white'}"
              aria-label={$t('tagsView.closeTag')}
            >
              <i class="pi pi-times text-[10px]"></i>
            </button>
          {/if}
        </ContextMenu.Trigger>

        <!-- 右键菜单内容 -->
        <ContextMenu.Portal>
          <ContextMenu.Content
            class="bg-white dark:bg-[#1d1d1d] rounded shadow-lg border border-[#e4e7ed] dark:border-[#303030] py-1 z-[9999] min-w-[120px]"
          >
            <ContextMenu.Item
              onselect={handleRefresh}
              class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <i class="pi pi-refresh mr-2 text-[12px]"></i>
              {$t('tagsView.refresh')}
            </ContextMenu.Item>

            {#if tag.path !== '/'}
              <ContextMenu.Item
                onselect={handleCloseCurrent}
                class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
              >
                <i class="pi pi-times mr-2 text-[12px]"></i>
                {$t('tagsView.closeCurrent')}
              </ContextMenu.Item>
            {/if}

            <ContextMenu.Item
              onselect={handleCloseOthers}
              class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <i class="pi pi-minus mr-2 text-[12px]"></i>
              {$t('tagsView.closeOthers')}
            </ContextMenu.Item>

            <ContextMenu.Item
              onselect={handleCloseAll}
              class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <i class="pi pi-times-circle mr-2 text-[12px]"></i>
              {$t('tagsView.closeAll')}
            </ContextMenu.Item>
          </ContextMenu.Content>
        </ContextMenu.Portal>
      </ContextMenu.Root>
    {/each}
  </div>
</div>
