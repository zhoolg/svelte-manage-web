<script lang="ts">
  import { DropdownMenu } from 'bits-ui';
  import { Check, ChevronDown, Search } from 'lucide-svelte';
  import Icon from './Icon.svelte';

  let {
    value = $bindable('home'),
    disabled = false,
  }: {
    value?: string;
    disabled?: boolean;
  } = $props();

  let open = $state(false);
  let keyword = $state('');

  type IconGroup = {
    title: string;
    icons: string[];
  };

  const iconGroups: IconGroup[] = [
    {
      title: '导航布局',
      icons: [
        'home',
        'menu',
        'bars',
        'layout',
        'sidebar',
        'th',
        'th-large',
        'columns',
        'rows',
        'list',
        'table',
        'sitemap',
        'project-diagram',
        'grip-vertical',
        'grip-horizontal',
        'ellipsis-h',
        'ellipsis-v',
        'external-link',
        'link',
      ],
    },
    {
      title: '系统操作',
      icons: [
        'search',
        'plus',
        'minus',
        'times',
        'check',
        'pencil',
        'edit',
        'trash',
        'trash-alt',
        'eye',
        'eye-slash',
        'refresh',
        'sync',
        'download',
        'upload',
        'copy',
        'clipboard',
        'save',
        'print',
        'filter',
        'sort-alt',
        'undo',
        'redo',
        'share',
        'reply',
      ],
    },
    {
      title: '用户权限',
      icons: [
        'user',
        'users',
        'user-plus',
        'user-minus',
        'user-edit',
        'user-check',
        'user-group',
        'user-friends',
        'users-cog',
        'user-cog',
        'user-shield',
        'user-lock',
        'user-clock',
        'user-tie',
        'user-tag',
        'user-circle',
        'id-card',
        'id-badge',
        'address-book',
        'address-card',
        'lock',
        'unlock',
        'key',
        'shield',
        'verified',
      ],
    },
    {
      title: '数据报表',
      icons: [
        'database',
        'server',
        'cloud',
        'cloud-upload',
        'cloud-download',
        'chart-bar',
        'chart-line',
        'chart-pie',
        'poll',
        'vote-yea',
        'clipboard-list',
        'clipboard-check',
        'tasks',
        'receipt',
        'qrcode',
        'barcode',
        'tag',
        'tags',
        'bookmark',
        'star',
        'heart',
      ],
    },
    {
      title: '业务财务',
      icons: [
        'building',
        'briefcase',
        'box',
        'shopping-cart',
        'shopping-bag',
        'credit-card',
        'wallet',
        'money-bill',
        'dollar',
        'percentage',
        'gift',
        'trophy',
        'crown',
        'gem',
        'certificate',
        'ribbon',
        'medal',
        'handshake',
        'hand-holding-usd',
      ],
    },
    {
      title: '文件内容',
      icons: [
        'file',
        'file-edit',
        'file-pdf',
        'file-excel',
        'file-word',
        'folder',
        'folder-open',
        'book',
        'book-open',
        'newspaper',
        'calendar',
        'calendar-plus',
        'calendar-minus',
        'calendar-times',
        'clock',
        'history',
        'stamp',
        'signature',
      ],
    },
    {
      title: '消息媒体',
      icons: [
        'bell',
        'bell-slash',
        'envelope',
        'inbox',
        'send',
        'comment',
        'comments',
        'rss',
        'podcast',
        'headphones',
        'music',
        'film',
        'tv',
        'video',
        'camera',
        'image',
        'images',
        'microphone',
        'volume-up',
        'volume-down',
        'volume-off',
      ],
    },
    {
      title: '工具开发',
      icons: [
        'cog',
        'sliders-h',
        'sliders-v',
        'code',
        'terminal',
        'bug',
        'eraser',
        'highlighter',
        'marker',
        'pen',
        'pencil-alt',
        'quill',
        'lightbulb',
        'magic',
        'sparkles',
        'wand-magic-sparkles',
        'robot',
        'user-robot',
        'android-alt',
        'puzzle-piece',
      ],
    },
    {
      title: '位置设备',
      icons: [
        'globe',
        'language',
        'map-marker',
        'map',
        'compass',
        'directions',
        'car',
        'truck',
        'plane',
        'rocket',
        'phone',
        'mobile',
        'wifi',
        'satellite-dish',
        'broadcast-tower',
        'space-shuttle',
      ],
    },
    {
      title: '状态提示',
      icons: [
        'info',
        'info-circle',
        'question',
        'question-circle',
        'exclamation-triangle',
        'exclamation-circle',
        'check-circle',
        'times-circle',
        'plus-circle',
        'minus-circle',
        'ban',
        'bolt',
        'power-off',
        'spinner',
        'circle-notch',
        'fire',
        'snowflake',
        'sun',
        'moon',
      ],
    },
    {
      title: '品牌平台',
      icons: [
        'github',
        'gitlab',
        'bitbucket',
        'slack',
        'discord',
        'twitter',
        'facebook',
        'instagram',
        'youtube',
        'linkedin',
        'google',
        'amazon',
        'paypal',
        'stripe',
        'bitcoin',
        'ethereum',
        'android',
        'apple',
        'windows',
        'linux',
      ],
    },
  ];

  const allIcons = $derived([...new Set(iconGroups.flatMap(group => group.icons))]);
  const visibleGroups = $derived.by(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) return iconGroups;
    return [
      {
        title: '搜索结果',
        icons: allIcons.filter(icon => icon.toLowerCase().includes(term)),
      },
    ];
  });

  function selectIcon(icon: string) {
    value = icon;
    open = false;
    keyword = '';
  }
</script>

<DropdownMenu.Root bind:open>
  <DropdownMenu.Trigger
    {disabled}
    class="flex h-9 w-full items-center justify-between rounded-md border border-gray-200 bg-white px-3 text-sm transition-colors hover:border-[color:var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-50 dark:border-gray-700 dark:bg-gray-800"
  >
    <span class="flex min-w-0 items-center gap-2">
      <span
        class="flex h-6 w-6 shrink-0 items-center justify-center rounded bg-[color:var(--color-primary-subtle)] text-[color:var(--color-primary)]"
      >
        <Icon name={value || 'home'} size={14} />
      </span>
      <span class="truncate text-gray-800 dark:text-gray-100">{value || 'home'}</span>
    </span>
    <ChevronDown size={14} class="ml-2 shrink-0 text-gray-400" />
  </DropdownMenu.Trigger>

  <DropdownMenu.Portal>
    <DropdownMenu.Content
      sideOffset={4}
      align="start"
      class="z-[9999] w-[560px] max-w-[calc(100vw-32px)] rounded-lg border border-gray-200 bg-white p-3 shadow-xl dark:border-gray-700 dark:bg-gray-800"
    >
      <div
        class="mb-3 flex h-9 items-center gap-2 rounded-md border border-gray-200 bg-gray-50 px-3 dark:border-gray-700 dark:bg-gray-900/40"
      >
        <Search size={14} class="shrink-0 text-gray-400" />
        <input
          type="text"
          bind:value={keyword}
          placeholder="搜索图标"
          class="min-w-0 flex-1 bg-transparent text-sm text-gray-800 outline-none placeholder:text-gray-400 dark:text-gray-100"
        />
      </div>

      <div class="max-h-[340px] overflow-y-auto pr-1">
        {#each visibleGroups as group}
          {#if group.icons.length}
            <div class="mb-3 last:mb-0">
              <div class="mb-2 text-xs font-medium text-gray-400">{group.title}</div>
              <div class="grid grid-cols-7 gap-2">
                {#each group.icons as icon}
                  <button
                    type="button"
                    onclick={() => selectIcon(icon)}
                    title={icon}
                    aria-label={`选择图标 ${icon}`}
                    class="group relative flex h-[58px] flex-col items-center justify-center gap-1 rounded-md border text-xs transition-colors {value ===
                    icon
                      ? 'border-[color:var(--color-primary)] bg-[color:var(--color-primary-subtle)] text-[color:var(--color-primary)]'
                      : 'border-gray-100 text-gray-600 hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] dark:border-gray-700 dark:text-gray-300'}"
                  >
                    <Icon name={icon} size={18} />
                    <span class="w-full truncate px-1 text-center">{icon}</span>
                    {#if value === icon}
                      <Check size={12} class="absolute right-1.5 top-1.5" />
                    {/if}
                  </button>
                {/each}
              </div>
            </div>
          {/if}
        {/each}
        {#if visibleGroups.every(group => group.icons.length === 0)}
          <div class="flex h-24 items-center justify-center text-sm text-gray-400">
            没有匹配的图标
          </div>
        {/if}
      </div>
    </DropdownMenu.Content>
  </DropdownMenu.Portal>
</DropdownMenu.Root>
