<script lang="ts">
  /**
   * 仪表盘页面 - 常用功能快捷入口
   *
   * 数据来源：
   * - 快捷入口：自动从 app.modules.ts 配置文件读取，按访问频率排序
   */
  import { LayoutGrid } from 'lucide-svelte';
  import { authStore } from '../stores/authStore';
  import { t } from '../lib/locales';
  import { navigate } from '../stores/routerStore';
  import { getFlatModules } from '../config/app.modules';
  import { topVisitedPaths } from '../stores/visitHistoryStore';
  import Icon from './Icon.svelte';

  // 从配置文件自动生成快捷入口
  // 获取所有非隐藏的、有路径的、有组件的模块（排除首页）
  const allModules = getFlatModules().filter(
    m => !m.hidden && m.path && m.path !== '/' && (m.customPage || m.crud) // 只包含有自定义页面或CRUD配置的模块
  );

  // 创建路径到模块的映射
  const pathToModule = new Map(allModules.map(m => [m.path, m]));

  // 快捷入口：完全按访问频率排序，取前8个
  $: quickLinks = (() => {
    // 获取所有访问过的路径（按访问次数排序）
    const visitedPaths = $topVisitedPaths.filter(path => pathToModule.has(path));

    // 如果访问记录少于8个，补充未访问的模块
    if (visitedPaths.length < 8) {
      const unvisitedPaths = allModules
        .map(m => m.path)
        .filter(path => !visitedPaths.includes(path))
        .slice(0, 8 - visitedPaths.length);

      visitedPaths.push(...unvisitedPaths);
    }

    // 只取前8个
    const sortedPaths = visitedPaths.slice(0, 8);

    // 转换为快捷入口数据
    return sortedPaths.map(path => {
      const m = pathToModule.get(path)!;
      const translate = $t;
      return {
        title: m.label,
        icon: m.icon,
        path: m.path,
        desc: m.crud?.title
          ? `${translate('dashboard.manage')}${translate(m.crud.title)}`
          : translate(m.label),
      };
    });
  })();
</script>

<div class="space-y-6 fade-in">
  <!-- 欢迎信息 -->
  <div
    class="rounded-lg p-6 text-white"
    style="background-image: linear-gradient(90deg, var(--color-primary), color-mix(in srgb, var(--color-primary) 60%, white));"
  >
    <h1 class="text-2xl font-bold mb-2">
      {$t('dashboard.welcome')}, {$authStore.user?.name || 'Admin'}!
    </h1>
    <p class="text-white/80">{$t('dashboard.welcomeSubtitle')}</p>
  </div>

  <!-- 常用功能 -->
  <div class="bg-white dark:bg-[#1d1d1d] rounded-lg p-6 shadow-sm">
    <h2 class="text-lg font-semibold text-gray-800 dark:text-white mb-4 flex items-center gap-2">
      <LayoutGrid size={18} style="color: var(--color-primary);" />
      {$t('dashboard.quickAccess')}
    </h2>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
      {#each quickLinks as link}
        <button
          type="button"
          onclick={() => navigate(link.path)}
          class="group p-4 rounded-lg border border-gray-100 bg-white text-left w-full cursor-pointer transition-all duration-150 hover:-translate-y-0.5 hover:border-[color:color-mix(in_srgb,var(--color-primary)_35%,#e5e7eb)] hover:shadow-md active:translate-y-0 dark:border-gray-800 dark:bg-[#1d1d1d] dark:hover:border-[color:color-mix(in_srgb,var(--color-primary)_45%,#1f2937)]"
        >
          <div
            class="w-10 h-10 rounded-lg flex items-center justify-center mb-3 transition-transform duration-150 bg-[color:var(--color-primary-subtle)] dark:bg-[color:color-mix(in_srgb,var(--color-primary)_18%,transparent)] group-hover:scale-105"
          >
            <Icon
              name={link.icon?.replace('pi pi-', '') || 'file'}
              size={18}
              style="color: var(--color-primary);"
            />
          </div>
          <h3 class="font-medium text-gray-800 dark:text-white text-sm mb-1">{$t(link.title)}</h3>
          <p class="text-xs text-gray-500 dark:text-gray-400">{link.desc}</p>
        </button>
      {/each}
    </div>
  </div>
</div>

<style>
  .fade-in {
    animation: fadeIn 0.3s ease-in;
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
</style>
