<script lang="ts">
  /**
   * 标签页导航组件
   * @zhoolg/svelte-admin-framework
   */
  import { ContextMenu } from 'bits-ui';
  import { X, RefreshCw, Minus, XCircle } from 'lucide-svelte';
  import { getI18n } from '../locales';

  interface Tag {
    path: string;
    title: string;
    closable: boolean;
  }

  interface Props {
    /** 当前路径 */
    currentPath: string;
    /** 路由名称映射 */
    routeNames: Record<string, string>;
    /** 导航函数 */
    onNavigate: (path: string) => void;
  }

  let { currentPath, routeNames, onNavigate }: Props = $props();

  const { t } = getI18n();

  let tags = $state<Tag[]>([{ path: '/', title: 'menu.home', closable: false }]);
  let contextMenuPath = $state('');

  // 监听路由变化，添加新标签
  $effect(() => {
    const path = currentPath;
    const cleanPath = path.split('?')[0].split('#')[0];
    const titleKey = routeNames[cleanPath];

    if (titleKey && !tags.find((tag) => tag.path === path)) {
      tags = [...tags, { path, title: titleKey, closable: path !== '/' }];
    }
  });

  function handleClose(path: string, e: MouseEvent) {
    e.stopPropagation();
    const newTags = tags.filter((tag) => tag.path !== path);
    tags = newTags;

    if (currentPath === path) {
      onNavigate(newTags[newTags.length - 1].path);
    }
  }

  function handleRefresh() {
    window.location.reload();
  }

  function handleCloseCurrent() {
    if (contextMenuPath !== '/') {
      const newTags = tags.filter((tag) => tag.path !== contextMenuPath);
      tags = newTags;
      if (currentPath === contextMenuPath) {
        onNavigate(newTags[newTags.length - 1].path);
      }
    }
  }

  function handleCloseOthers() {
    const currentTag = tags.find((tag) => tag.path === contextMenuPath);
    const homeTag = tags.find((tag) => tag.path === '/');
    const newTags = [homeTag!, currentTag!].filter(
      (tag, index, self) => tag && self.findIndex((t) => t?.path === tag.path) === index
    );
    tags = newTags;
    if (!newTags.find((tag) => tag.path === currentPath)) {
      onNavigate(contextMenuPath);
    }
  }

  function handleCloseAll() {
    tags = [{ path: '/', title: 'menu.home', closable: false }];
    onNavigate('/');
  }
</script>

<div
  class="h-[34px] bg-white dark:bg-[#141414] border-b border-[#d8dce5] dark:border-[#303030] flex items-center px-[10px] shadow-sm"
>
  <div class="flex-1 flex items-center gap-[4px] overflow-x-auto scrollbar-hide">
    {#each tags as tag}
      <ContextMenu.Root onOpenChange={() => (contextMenuPath = tag.path)}>
        <ContextMenu.Trigger
          onclick={() => onNavigate(tag.path)}
          class="group flex items-center gap-[6px] px-[8px] h-[26px] border rounded-[3px] cursor-pointer text-[12px] transition-all select-none {currentPath ===
          tag.path
            ? 'bg-[#409eff] text-white border-[#409eff]'
            : 'bg-white dark:bg-[#1d1d1d] text-[#495060] dark:text-[#ccc] border-[#e6e6e6] dark:border-[#303030] hover:border-[#409eff]'}"
        >
          {#if currentPath === tag.path}
            <span class="w-[8px] h-[8px] bg-white rounded-full"></span>
          {/if}
          <span>{$t(tag.title)}</span>
          {#if tag.closable}
            <button
              onclick={(e) => handleClose(tag.path, e)}
              class="w-[16px] h-[16px] rounded-full flex items-center justify-center transition-colors {currentPath ===
              tag.path
                ? 'hover:bg-white/20'
                : 'hover:bg-[#409eff] hover:text-white'}"
              aria-label="关闭标签"
            >
              <X size={10} />
            </button>
          {/if}
        </ContextMenu.Trigger>

        <ContextMenu.Portal>
          <ContextMenu.Content
            class="bg-white dark:bg-[#1d1d1d] rounded shadow-lg border border-[#e4e7ed] dark:border-[#303030] py-1 z-[9999] min-w-[120px]"
          >
            <ContextMenu.Item
              onselect={handleRefresh}
              class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <RefreshCw size={12} class="mr-2" />
              {$t('common.refresh')}
            </ContextMenu.Item>

            {#if tag.path !== '/'}
              <ContextMenu.Item
                onselect={handleCloseCurrent}
                class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
              >
                <X size={12} class="mr-2" />
                {$t('common.close')}
              </ContextMenu.Item>
            {/if}

            <ContextMenu.Item
              onselect={handleCloseOthers}
              class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <Minus size={12} class="mr-2" />
              关闭其他
            </ContextMenu.Item>

            <ContextMenu.Item
              onselect={handleCloseAll}
              class="w-full flex items-center px-4 py-2 text-[13px] text-[#606266] dark:text-[#ccc] hover:bg-[#ecf5ff] hover:text-[#409eff] transition-colors cursor-pointer outline-none"
            >
              <XCircle size={12} class="mr-2" />
              关闭全部
            </ContextMenu.Item>
          </ContextMenu.Content>
        </ContextMenu.Portal>
      </ContextMenu.Root>
    {/each}
  </div>
</div>
