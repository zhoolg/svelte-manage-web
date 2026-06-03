<script lang="ts">
  /**
   * 系统设置页面 - 使用 Bits UI Tabs
   * ============================================================
   *
   * 重构说明：
   * - 从自定义标签页切换迁移到 Bits UI Tabs
   * - 保持原有功能和样式
   * - 提升可访问性和键盘导航
   */
  import { Tabs, Switch } from 'bits-ui';
  import {
    Palette,
    Settings as SettingsIcon,
    Cog,
    Sun,
    Moon,
    Monitor,
    Check,
    Bookmark,
    LayoutDashboard,
    ArrowUpDown,
    Minus,
    Trash2,
    RefreshCw,
    Info,
    Box,
    Tag,
    Code,
    Paintbrush,
    Zap,
    Database,
  } from 'lucide-svelte';
  import { settingsStore } from '../stores/settingsStore';
  import { t, locale, setLocale, localeOptions } from '../lib/locales';
  import { APP_CONFIG } from '../config';
  import Icon from './Icon.svelte';

  async function handleReset() {
    const confirmed = await (window as any).confirm({
      title: $t('settings.resetSettings'),
      content: $t('settings.resetSettingsDesc'),
      type: 'warning',
    });
    if (confirmed) {
      settingsStore.resetSettings();
      (window as any).toast?.success($t('settings.resetSuccess'));
    }
  }

  async function handleClearCache() {
    const confirmed = await (window as any).confirm({
      title: $t('settings.clearCache'),
      content: $t('settings.clearCacheDesc'),
      type: 'warning',
    });
    if (confirmed) {
      localStorage.removeItem('menu_visit_history');
      localStorage.removeItem('tags-storage');
      (window as any).toast?.success($t('settings.clearCacheSuccess'));
    }
  }

  const tabs = [
    { value: 'theme', label: $t('settings.theme'), icon: Palette },
    { value: 'interface', label: $t('settings.interface'), icon: SettingsIcon },
    { value: 'other', label: $t('settings.other'), icon: Cog },
  ];

  const primaryColorPresets = [
    {
      value: 'pure' as const,
      label: '纯净原生蓝',
      desc: '极致纯粹透亮的系统级原生蓝 (默认)',
      color: '#007AFF',
    },
    {
      value: 'stripe' as const,
      label: 'Stripe 蓝',
      desc: '顶尖金融科技的极致科技感',
      color: '#635BFF',
    },
    {
      value: 'linear' as const,
      label: 'Linear 紫',
      desc: '硅谷极简设计的高阶深紫',
      color: '#5E6AD2',
    },
    {
      value: 'emerald' as const,
      label: '现代翠绿',
      desc: '充满活力与生机的清爽现代绿',
      color: '#10B981',
    },
    {
      value: 'sunset' as const,
      label: '日落活力橙',
      desc: '吸睛且极具高互动的破晓橙色',
      color: '#FF4500',
    },
    {
      value: 'rose' as const,
      label: '高端玫瑰红',
      desc: '克制且充满感性张力的柔和玫瑰色',
      color: '#F43F5E',
    },
  ];
</script>

<div class="fade-in">
  <!-- 页面标题 -->
  <div class="mb-6">
    <h1 class="text-xl font-semibold text-gray-800 dark:text-white">{$t('settings.title')}</h1>
    <p class="text-sm text-gray-500 mt-1">{$t('settings.subtitle')}</p>
  </div>

  <div class="flex gap-6">
    <!-- 使用 Bits UI Tabs -->
    <Tabs.Root value="theme" class="flex gap-6 w-full">
      <!-- 左侧标签导航 -->
      <Tabs.List
        class="w-48 flex-shrink-0 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-2 h-fit"
      >
        {#each tabs as tab}
          <Tabs.Trigger
            value={tab.value}
            class="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm transition-colors data-[state=active]:text-white data-[state=inactive]:text-gray-600 data-[state=inactive]:dark:text-gray-400 data-[state=inactive]:bg-[color:var(--color-bg-surface)] data-[state=inactive]:hover:bg-gray-100 data-[state=inactive]:dark:hover:bg-gray-800 data-[state=active]:bg-[color:var(--color-primary)]"
          >
            <svelte:component this={tab.icon} size={16} />
            <span>{tab.label}</span>
          </Tabs.Trigger>
        {/each}
      </Tabs.List>

      <!-- 右侧内容区 -->
      <div class="flex-1 space-y-6">
        <!-- 主题设置标签页 -->
        <Tabs.Content value="theme" class="space-y-6">
          <!-- 主题模式 -->
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-6">
            <h3 class="text-base font-medium text-gray-800 dark:text-white mb-4">
              {$t('settings.themeMode')}
            </h3>
            <div class="grid grid-cols-3 gap-4">
              {#each [{ value: 'light' as const, label: $t('settings.light'), iconComponent: Sun, preview: 'bg-white border-gray-200' }, { value: 'dark' as const, label: $t('settings.dark'), iconComponent: Moon, preview: 'bg-gray-800 border-gray-700' }, { value: 'system' as const, label: $t('settings.system'), iconComponent: Monitor, preview: 'bg-gradient-to-r from-white to-gray-800 border-gray-300' }] as item}
                <button
                  type="button"
                  onclick={() => settingsStore.setTheme(item.value)}
                  class="relative flex flex-col items-center gap-3 p-4 rounded-xl border-2 transition-all {$settingsStore.theme ===
                  item.value
                    ? 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_5%,transparent)]'
                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
                >
                  <div class="w-full h-16 rounded-lg border {item.preview}">
                    <div class="h-3 bg-gray-300 dark:bg-gray-600 rounded-t-lg"></div>
                    <div class="flex h-[calc(100%-12px)]">
                      <div class="w-1/4 bg-gray-200 dark:bg-gray-700"></div>
                      <div class="flex-1 p-1">
                        <div class="w-full h-2 bg-gray-100 dark:bg-gray-600 rounded mb-1"></div>
                        <div class="w-2/3 h-2 bg-gray-100 dark:bg-gray-600 rounded"></div>
                      </div>
                    </div>
                  </div>
                  <div class="flex items-center gap-2">
                    <svelte:component
                      this={item.iconComponent}
                      size={16}
                      class={$settingsStore.theme === item.value
                        ? 'text-[color:var(--color-primary)]'
                        : 'text-gray-400'}
                    />
                    <span
                      class="text-sm {$settingsStore.theme === item.value
                        ? 'text-[color:var(--color-primary)] font-medium'
                        : 'text-gray-600 dark:text-gray-400'}"
                    >
                      {item.label}
                    </span>
                  </div>
                  {#if $settingsStore.theme === item.value}
                    <div
                      class="absolute top-2 right-2 w-5 h-5 rounded-full flex items-center justify-center"
                      style="background-color: var(--color-primary);"
                    >
                      <Check size={12} class="text-white" />
                    </div>
                  {/if}
                </button>
              {/each}
            </div>
          </div>

          <!-- 主题色预设 -->
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-6">
            <h3 class="text-base font-medium text-gray-800 dark:text-white mb-2">主题主色</h3>
            <p class="text-sm text-gray-500 mb-4">
              选择一套主色，用于按钮、高亮文本、菜单选中状态等关键视觉元素。
            </p>
            <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
              {#each primaryColorPresets as preset}
                <button
                  type="button"
                  onclick={() => settingsStore.setPrimaryColor(preset.value)}
                  class="flex items-center gap-3 p-4 rounded-xl border-2 transition-all text-left {$settingsStore.primaryColor ===
                  preset.value
                    ? 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_5%,transparent)]'
                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
                >
                  <div
                    class="w-8 h-8 rounded-full flex items-center justify-center shadow-sm"
                    style={`background:${preset.color}`}
                  >
                    <Paintbrush size={16} class="text-white" />
                  </div>
                  <div class="flex-1 space-y-1">
                    <div class="flex items-center gap-2">
                      <span
                        class="text-sm font-medium {$settingsStore.primaryColor === preset.value
                          ? 'text-[color:var(--color-primary)]'
                          : 'text-gray-800 dark:text-gray-100'}"
                      >
                        {preset.label}
                      </span>
                      {#if $settingsStore.primaryColor === preset.value}
                        <Check size={14} style="color: var(--color-primary);" />
                      {/if}
                    </div>
                    <p class="text-xs text-gray-500 dark:text-gray-400">
                      {preset.desc}
                    </p>
                  </div>
                </button>
              {/each}
            </div>
          </div>

          <!-- 语言设置 -->
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-6">
            <h3 class="text-base font-medium text-gray-800 dark:text-white mb-2">
              {$t('settings.language')}
            </h3>
            <p class="text-sm text-gray-500 mb-4">{$t('settings.languageDesc')}</p>
            <div class="grid grid-cols-2 gap-4">
              {#each localeOptions as lang}
                <button
                  type="button"
                  onclick={() => setLocale(lang.value)}
                  class="flex items-center gap-3 p-4 rounded-xl border-2 transition-all {$locale ===
                  lang.value
                    ? 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_5%,transparent)]'
                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
                >
                  <span class="text-2xl">{lang.icon}</span>
                  <span
                    class="text-sm {$locale === lang.value
                      ? 'text-[color:var(--color-primary)] font-medium'
                      : 'text-gray-600 dark:text-gray-400'}"
                  >
                    {lang.label}
                  </span>
                  {#if $locale === lang.value}
                    <Check size={14} class="ml-auto" style="color: var(--color-primary);" />
                  {/if}
                </button>
              {/each}
            </div>
          </div>
        </Tabs.Content>

        <!-- 界面设置标签页 -->
        <Tabs.Content value="interface" class="space-y-6">
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm">
            <div class="divide-y divide-gray-100 dark:divide-gray-800">
              {#each [{ key: 'showTagsView', label: $t('settings.showTagsView'), desc: $t('settings.showTagsViewDesc'), value: $settingsStore.showTagsView, onChange: settingsStore.setShowTagsView, iconComponent: Bookmark }, { key: 'showBreadcrumb', label: $t('settings.showBreadcrumb'), desc: $t('settings.showBreadcrumbDesc'), value: $settingsStore.showBreadcrumb, onChange: settingsStore.setShowBreadcrumb, iconComponent: LayoutDashboard }, { key: 'fixedHeader', label: $t('settings.fixedHeader'), desc: $t('settings.fixedHeaderDesc'), value: $settingsStore.fixedHeader, onChange: settingsStore.setFixedHeader, iconComponent: ArrowUpDown }, { key: 'showFooter', label: $t('settings.showFooter'), desc: $t('settings.showFooterDesc'), value: $settingsStore.showFooter, onChange: settingsStore.setShowFooter, iconComponent: Minus }] as item}
                <div
                  class="flex items-center justify-between p-5 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                >
                  <div class="flex items-center gap-4">
                    <div
                      class="w-10 h-10 rounded-lg bg-[#409eff]/10 flex items-center justify-center"
                    >
                      <svelte:component
                        this={item.iconComponent}
                        size={18}
                        style="color: var(--color-primary);"
                      />
                    </div>
                    <div>
                      <p class="text-sm font-medium text-gray-700 dark:text-gray-300">
                        {item.label}
                      </p>
                      <p class="text-xs text-gray-500 mt-0.5">{item.desc}</p>
                    </div>
                  </div>
                  <Switch.Root
                    checked={item.value}
                    onCheckedChange={checked => item.onChange(!!checked)}
                    class="relative inline-flex items-center h-6 w-12 rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[color:var(--color-primary)] focus-visible:ring-offset-2 data-[state=checked]:bg-[color:var(--color-primary)] data-[state=unchecked]:bg-gray-200 dark:data-[state=unchecked]:bg-gray-700"
                  >
                    <Switch.Thumb
                      class="pointer-events-none block h-5 w-5 rounded-full bg-white shadow-sm transition-transform data-[state=checked]:translate-x-6 data-[state=unchecked]:translate-x-[2px]"
                    />
                  </Switch.Root>
                </div>
              {/each}
            </div>
          </div>

          <!-- 界面密度设置 -->
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-6">
            <h3 class="text-base font-medium text-gray-800 dark:text-white mb-2">界面密度</h3>
            <p class="text-sm text-gray-500 mb-4">
              控制表格、列表等区域的整体疏密程度，适配不同信息量场景。
            </p>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                type="button"
                onclick={() => settingsStore.setDensity('comfortable')}
                class="flex items-center gap-3 p-4 rounded-xl border-2 text-left transition-all {$settingsStore.density ===
                'comfortable'
                  ? 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_5%,transparent)]'
                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
              >
                <div
                  class="w-9 h-9 rounded-lg bg-gray-100 dark:bg-gray-800 flex flex-col justify-center px-1 gap-1"
                >
                  <div class="h-[6px] rounded bg-gray-300 dark:bg-gray-700"></div>
                  <div class="h-[6px] rounded bg-gray-300 dark:bg-gray-700"></div>
                  <div class="h-[6px] rounded bg-gray-300 dark:bg-gray-700"></div>
                </div>
                <div class="flex-1 space-y-0.5">
                  <div class="flex items-center gap-2">
                    <span
                      class="text-sm font-medium {$settingsStore.density === 'comfortable'
                        ? 'text-[color:var(--color-primary)]'
                        : 'text-gray-800 dark:text-gray-100'}"
                    >
                      舒适模式
                    </span>
                    {#if $settingsStore.density === 'comfortable'}
                      <Check size={14} style="color: var(--color-primary);" />
                    {/if}
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    行高更大、留白更多，适合管理端日常使用。
                  </p>
                </div>
              </button>

              <button
                type="button"
                onclick={() => settingsStore.setDensity('compact')}
                class="flex items-center gap-3 p-4 rounded-xl border-2 text-left transition-all {$settingsStore.density ===
                'compact'
                  ? 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_5%,transparent)]'
                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
              >
                <div
                  class="w-9 h-9 rounded-lg bg-gray-100 dark:bg-gray-800 flex flex-col justify-center px-1 gap-[2px]"
                >
                  <div class="h-[4px] rounded bg-gray-300 dark:bg-gray-700"></div>
                  <div class="h-[4px] rounded bg-gray-300 dark:bg-gray-700"></div>
                  <div class="h-[4px] rounded bg-gray-300 dark:bg-gray-700"></div>
                  <div class="h-[4px] rounded bg-gray-300 dark:bg-gray-700"></div>
                </div>
                <div class="flex-1 space-y-0.5">
                  <div class="flex items-center gap-2">
                    <span
                      class="text-sm font-medium {$settingsStore.density === 'compact'
                        ? 'text-[color:var(--color-primary)]'
                        : 'text-gray-800 dark:text-gray-100'}"
                    >
                      紧凑模式
                    </span>
                    {#if $settingsStore.density === 'compact'}
                      <Check size={14} style="color: var(--color-primary);" />
                    {/if}
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    在有限空间中展示更多行数据，适合高密度业务场景。
                  </p>
                </div>
              </button>
            </div>
          </div>

          <!-- 圆角风格设置 -->
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-6">
            <h3 class="text-base font-medium text-gray-800 dark:text-white mb-2">界面圆角</h3>
            <p class="text-sm text-gray-500 mb-4">调整卡片等组件的圆角风格，适配不同品牌气质。</p>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                type="button"
                onclick={() => settingsStore.setCornerStyle('default')}
                class="flex items-center gap-3 p-4 rounded-xl border-2 text-left transition-all {$settingsStore.cornerStyle ===
                'default'
                  ? 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_5%,transparent)]'
                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
              >
                <div
                  class="w-9 h-9 rounded-[6px] bg-gray-100 dark:bg-gray-800 flex items-center justify-center"
                >
                  <LayoutDashboard size={16} class="text-gray-500" />
                </div>
                <div class="flex-1 space-y-0.5">
                  <div class="flex items-center gap-2">
                    <span
                      class="text-sm font-medium {$settingsStore.cornerStyle === 'default'
                        ? 'text-[color:var(--color-primary)]'
                        : 'text-gray-800 dark:text-gray-100'}"
                    >
                      标准圆角
                    </span>
                    {#if $settingsStore.cornerStyle === 'default'}
                      <Check size={14} style="color: var(--color-primary);" />
                    {/if}
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    兼顾稳重与现代感，适合大多数场景。
                  </p>
                </div>
              </button>

              <button
                type="button"
                onclick={() => settingsStore.setCornerStyle('rounded')}
                class="flex items-center gap-3 p-4 rounded-xl border-2 text-left transition-all {$settingsStore.cornerStyle ===
                'rounded'
                  ? 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_5%,transparent)]'
                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
              >
                <div
                  class="w-9 h-9 rounded-[999px] bg-gray-100 dark:bg-gray-800 flex items-center justify-center"
                >
                  <Paintbrush size={16} class="text-gray-500" />
                </div>
                <div class="flex-1 space-y-0.5">
                  <div class="flex items-center gap-2">
                    <span
                      class="text-sm font-medium {$settingsStore.cornerStyle === 'rounded'
                        ? 'text-[color:var(--color-primary)]'
                        : 'text-gray-800 dark:text-gray-100'}"
                    >
                      柔和圆角
                    </span>
                    {#if $settingsStore.cornerStyle === 'rounded'}
                      <Check size={14} style="color: var(--color-primary);" />
                    {/if}
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    更圆润的视觉风格，适合品牌化、年轻化的后台。
                  </p>
                </div>
              </button>
            </div>
          </div>
        </Tabs.Content>

        <!-- 其他设置标签页 -->
        <Tabs.Content value="other" class="space-y-6">
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm">
            <div class="divide-y divide-gray-100 dark:divide-gray-800">
              <!-- 清除缓存 -->
              <div class="flex items-center justify-between p-5">
                <div class="flex items-center gap-4">
                  <div
                    class="w-10 h-10 rounded-lg bg-orange-500/10 flex items-center justify-center"
                  >
                    <Trash2 size={18} class="text-orange-500" />
                  </div>
                  <div>
                    <p class="text-sm font-medium text-gray-700 dark:text-gray-300">
                      {$t('settings.clearCache')}
                    </p>
                    <p class="text-xs text-gray-500 mt-0.5">{$t('settings.clearCacheDesc')}</p>
                  </div>
                </div>
                <button
                  type="button"
                  onclick={handleClearCache}
                  class="px-4 py-2 text-sm text-orange-500 border border-orange-500 rounded-lg hover:bg-orange-500 hover:text-white transition-colors"
                >
                  {$t('settings.clearCache')}
                </button>
              </div>

              <!-- 重置设置 -->
              <div class="flex items-center justify-between p-5">
                <div class="flex items-center gap-4">
                  <div class="w-10 h-10 rounded-lg bg-red-500/10 flex items-center justify-center">
                    <RefreshCw size={18} class="text-red-500" />
                  </div>
                  <div>
                    <p class="text-sm font-medium text-gray-700 dark:text-gray-300">
                      {$t('settings.resetSettings')}
                    </p>
                    <p class="text-xs text-gray-500 mt-0.5">{$t('settings.resetSettingsDesc')}</p>
                  </div>
                </div>
                <button
                  type="button"
                  onclick={handleReset}
                  class="px-4 py-2 text-sm text-red-500 border border-red-500 rounded-lg hover:bg-red-500 hover:text-white transition-colors"
                >
                  {$t('settings.resetSettings')}
                </button>
              </div>
            </div>
          </div>

          <!-- 系统信息 -->
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-6">
            <h3
              class="text-base font-medium text-gray-800 dark:text-white mb-4 flex items-center gap-2"
            >
              <Info size={18} class="text-[#409eff]" />
              {$t('settings.systemInfo')}
            </h3>
            <div class="grid grid-cols-2 gap-4">
              {#each [{ label: $t('settings.systemName'), value: APP_CONFIG.title, iconComponent: Box, color: '#409eff' }, { label: $t('settings.systemVersion'), value: APP_CONFIG.version, iconComponent: Tag, color: '#67c23a' }, { label: $t('settings.frontend'), value: 'Svelte 5 + TypeScript', iconComponent: Code, color: '#e6a23c' }, { label: $t('settings.uiFramework'), value: 'Tailwind CSS 4 + Bits UI', iconComponent: Paintbrush, color: '#f56c6c' }, { label: $t('settings.buildTool'), value: 'Vite 7', iconComponent: Zap, color: '#722ed1' }, { label: $t('settings.stateManagement'), value: 'Svelte Stores', iconComponent: Database, color: '#13c2c2' }] as item}
                <div class="flex items-center gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                  <div
                    class="w-9 h-9 rounded-lg flex items-center justify-center"
                    style="background-color: {item.color}15"
                  >
                    <svelte:component
                      this={item.iconComponent}
                      size={16}
                      style="color: {item.color}"
                    />
                  </div>
                  <div>
                    <p class="text-xs text-gray-500">{item.label}</p>
                    <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{item.value}</p>
                  </div>
                </div>
              {/each}
            </div>
          </div>
        </Tabs.Content>
      </div>
    </Tabs.Root>
  </div>
</div>
