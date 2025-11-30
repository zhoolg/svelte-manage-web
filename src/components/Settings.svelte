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
  import { Tabs } from 'bits-ui';
  import { settingsStore } from '../stores/settingsStore';
  import { t, locale, setLocale, localeOptions } from '../lib/locales';
  import { APP_CONFIG } from '../config';

  const themeColors = [
    { name: '默认蓝', value: '#409eff' },
    { name: '极光绿', value: '#67c23a' },
    { name: '明亮紫', value: '#722ed1' },
    { name: '活力橙', value: '#fa8c16' },
    { name: '浪漫粉', value: '#eb2f96' },
    { name: '极客青', value: '#13c2c2' },
  ];

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
    { value: 'theme', label: $t('settings.theme'), icon: 'pi-palette' },
    { value: 'interface', label: $t('settings.interface'), icon: 'pi-sliders-h' },
    { value: 'other', label: $t('settings.other'), icon: 'pi-cog' },
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
      <Tabs.List class="w-48 flex-shrink-0 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-2 h-fit">
        {#each tabs as tab}
          <Tabs.Trigger
            value={tab.value}
            class="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm transition-colors data-[state=active]:bg-[#409eff] data-[state=active]:text-white data-[state=inactive]:text-gray-600 data-[state=inactive]:dark:text-gray-400 data-[state=inactive]:hover:bg-gray-100 data-[state=inactive]:dark:hover:bg-gray-800"
          >
            <i class="pi {tab.icon}"></i>
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
              {#each [
                { value: 'light' as const, label: $t('settings.light'), icon: 'pi-sun', preview: 'bg-white border-gray-200' },
                { value: 'dark' as const, label: $t('settings.dark'), icon: 'pi-moon', preview: 'bg-gray-800 border-gray-700' },
                { value: 'system' as const, label: $t('settings.system'), icon: 'pi-desktop', preview: 'bg-gradient-to-r from-white to-gray-800 border-gray-300' },
              ] as item}
                <button
                  type="button"
                  onclick={() => settingsStore.setTheme(item.value)}
                  class="relative flex flex-col items-center gap-3 p-4 rounded-xl border-2 transition-all {$settingsStore.theme === item.value
                    ? 'border-[#409eff] bg-[#409eff]/5'
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
                    <i class="pi {item.icon} {$settingsStore.theme === item.value ? 'text-[#409eff]' : 'text-gray-400'}"></i>
                    <span class="text-sm {$settingsStore.theme === item.value ? 'text-[#409eff] font-medium' : 'text-gray-600 dark:text-gray-400'}">
                      {item.label}
                    </span>
                  </div>
                  {#if $settingsStore.theme === item.value}
                    <div class="absolute top-2 right-2 w-5 h-5 bg-[#409eff] rounded-full flex items-center justify-center">
                      <i class="pi pi-check text-white text-xs"></i>
                    </div>
                  {/if}
                </button>
              {/each}
            </div>
          </div>

          <!-- 主题色 -->
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-6">
            <h3 class="text-base font-medium text-gray-800 dark:text-white mb-4">
              {$t('settings.themeColor')}
            </h3>
            <div class="flex flex-wrap gap-4">
              {#each themeColors as color}
                <button
                  type="button"
                  onclick={() => settingsStore.setPrimaryColor(color.value)}
                  class="group relative w-14 h-14 rounded-xl flex items-center justify-center transition-all hover:scale-110 {$settingsStore.primaryColor === color.value ? 'ring-2 ring-offset-2 ring-offset-white dark:ring-offset-[#1d1d1d] ring-gray-400' : ''}"
                  style="background-color: {color.value}"
                  title={color.name}
                >
                  {#if $settingsStore.primaryColor === color.value}
                    <i class="pi pi-check text-white text-lg"></i>
                  {/if}
                  <span class="absolute -bottom-6 left-1/2 -translate-x-1/2 text-xs text-gray-500 whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity">
                    {color.name}
                  </span>
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
                  class="flex items-center gap-3 p-4 rounded-xl border-2 transition-all {$locale === lang.value
                    ? 'border-[#409eff] bg-[#409eff]/5'
                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'}"
                >
                  <span class="text-2xl">{lang.icon}</span>
                  <span class="text-sm {$locale === lang.value ? 'text-[#409eff] font-medium' : 'text-gray-600 dark:text-gray-400'}">
                    {lang.label}
                  </span>
                  {#if $locale === lang.value}
                    <i class="pi pi-check text-[#409eff] ml-auto"></i>
                  {/if}
                </button>
              {/each}
            </div>
          </div>
        </Tabs.Content>

        <!-- 界面设置标签页 -->
        <Tabs.Content value="interface">
          <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm">
            <div class="divide-y divide-gray-100 dark:divide-gray-800">
              {#each [
                { key: 'showTagsView', label: $t('settings.showTagsView'), desc: $t('settings.showTagsViewDesc'), value: $settingsStore.showTagsView, onChange: settingsStore.setShowTagsView, icon: 'pi-bookmark' },
                { key: 'showBreadcrumb', label: $t('settings.showBreadcrumb'), desc: $t('settings.showBreadcrumbDesc'), value: $settingsStore.showBreadcrumb, onChange: settingsStore.setShowBreadcrumb, icon: 'pi-sitemap' },
                { key: 'fixedHeader', label: $t('settings.fixedHeader'), desc: $t('settings.fixedHeaderDesc'), value: $settingsStore.fixedHeader, onChange: settingsStore.setFixedHeader, icon: 'pi-arrows-v' },
                { key: 'showFooter', label: $t('settings.showFooter'), desc: $t('settings.showFooterDesc'), value: $settingsStore.showFooter, onChange: settingsStore.setShowFooter, icon: 'pi-minus' },
              ] as item}
                <div class="flex items-center justify-between p-5 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors">
                  <div class="flex items-center gap-4">
                    <div class="w-10 h-10 rounded-lg bg-[#409eff]/10 flex items-center justify-center">
                      <i class="pi {item.icon} text-[#409eff]"></i>
                    </div>
                    <div>
                      <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{item.label}</p>
                      <p class="text-xs text-gray-500 mt-0.5">{item.desc}</p>
                    </div>
                  </div>
                  <label class="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={item.value}
                      onchange={(e) => item.onChange(e.currentTarget.checked)}
                      class="sr-only peer"
                    />
                    <div class="w-12 h-6 bg-gray-200 dark:bg-gray-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-6 peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all after:shadow-sm peer-checked:bg-[#409eff]"></div>
                  </label>
                </div>
              {/each}
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
                  <div class="w-10 h-10 rounded-lg bg-orange-500/10 flex items-center justify-center">
                    <i class="pi pi-trash text-orange-500"></i>
                  </div>
                  <div>
                    <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{$t('settings.clearCache')}</p>
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
                    <i class="pi pi-refresh text-red-500"></i>
                  </div>
                  <div>
                    <p class="text-sm font-medium text-gray-700 dark:text-gray-300">{$t('settings.resetSettings')}</p>
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
            <h3 class="text-base font-medium text-gray-800 dark:text-white mb-4 flex items-center gap-2">
              <i class="pi pi-info-circle text-[#409eff]"></i>
              {$t('settings.systemInfo')}
            </h3>
            <div class="grid grid-cols-2 gap-4">
              {#each [
                { label: $t('settings.systemName'), value: APP_CONFIG.title, icon: 'pi-box', color: '#409eff' },
                { label: $t('settings.systemVersion'), value: APP_CONFIG.version, icon: 'pi-tag', color: '#67c23a' },
                { label: $t('settings.frontend'), value: 'Svelte 5 + TypeScript', icon: 'pi-code', color: '#e6a23c' },
                { label: $t('settings.uiFramework'), value: 'Tailwind CSS 4 + Bits UI', icon: 'pi-palette', color: '#f56c6c' },
                { label: $t('settings.buildTool'), value: 'Vite 7', icon: 'pi-bolt', color: '#722ed1' },
                { label: $t('settings.stateManagement'), value: 'Svelte Stores', icon: 'pi-database', color: '#13c2c2' },
              ] as item}
                <div class="flex items-center gap-3 p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                  <div
                    class="w-9 h-9 rounded-lg flex items-center justify-center"
                    style="background-color: {item.color}15"
                  >
                    <i class="pi {item.icon} text-sm" style="color: {item.color}"></i>
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
