<script lang="ts">
  /**
   * 仪表盘页面
   * 显示系统概览统计和快捷入口
   *
   * 数据来源：
   * - 快捷入口：自动从 app.modules.ts 配置文件读取，按访问频率排序
   * - 统计数据：可配置的统计卡片
   */
  import { authStore } from '../stores/authStore';
  import { t } from '../lib/locales';
  import { navigate } from '../stores/routerStore';
  import { APP_MODULES, getFlatModules } from '../config/app.modules';
  import { topVisitedPaths } from '../stores/visitHistoryStore';

  // 从配置文件自动生成快捷入口
  // 获取所有非隐藏的、有路径的模块（排除首页）
  const allModules = getFlatModules().filter(m => !m.hidden && m.path && m.path !== '/');

  // 创建路径到模块的映射
  const pathToModule = new Map(allModules.map(m => [m.path, m]));

  // 快捷入口：按访问频率排序，取前8个
  $: quickLinks = (() => {
    // 获取访问过的路径
    const visitedPaths = $topVisitedPaths.filter(path => pathToModule.has(path));

    // 获取未访问过的路径
    const unvisitedPaths = allModules
      .map(m => m.path)
      .filter(path => !visitedPaths.includes(path));

    // 合并：访问过的在前，未访问的在后
    const sortedPaths = [...visitedPaths, ...unvisitedPaths].slice(0, 8);

    // 转换为快捷入口数据
    return sortedPaths.map(path => {
      const m = pathToModule.get(path)!;
      const translate = $t;
      return {
        title: m.label,
        icon: m.icon,
        path: m.path,
        desc: m.crud?.title ? `${translate('dashboard.manage')}${m.crud.title}` : translate(m.label)
      };
    });
  })();

  // 统计数据配置
  interface StatConfig {
    title: string;
    value: string;
    icon: string;
    color: 'blue' | 'green' | 'yellow' | 'red';
    change: string;
    moduleId?: string; // 关联的模块ID，用于自动统计
  }

  // 统计数据 - 可以根据模块自动生成
  const stats: StatConfig[] = [
    { title: 'dashboard.totalUsers', value: '1,234', icon: 'pi-users', color: 'blue', change: '+12%', moduleId: 'users' },
    { title: 'dashboard.totalAgents', value: '567', icon: 'pi-briefcase', color: 'green', change: '+8%', moduleId: 'agents' },
    { title: 'dashboard.totalFaq', value: '89', icon: 'pi-question-circle', color: 'yellow', change: '+5%', moduleId: 'faq' },
    { title: 'dashboard.monthlyNew', value: '42', icon: 'pi-chart-line', color: 'red', change: '+15%' },
  ];

  // 最近活动
  interface Activity {
    user: string;
    action: string;
    time: string;
    type: 'success' | 'info' | 'warning' | 'error';
  }

  // 使用翻译键
  $: activities = [
    { user: 'Zhang San', action: $t('dashboard.activity.addedUser'), time: $t('dashboard.activity.minutesAgo', { minutes: 5 }), type: 'success' as const },
    { user: 'Li Si', action: $t('dashboard.activity.modifiedAgent'), time: $t('dashboard.activity.minutesAgo', { minutes: 10 }), type: 'info' as const },
    { user: 'Wang Wu', action: $t('dashboard.activity.deletedFaq'), time: $t('dashboard.activity.minutesAgo', { minutes: 30 }), type: 'warning' as const },
    { user: 'Zhao Liu', action: $t('dashboard.activity.loggedIn'), time: $t('dashboard.activity.hoursAgo', { hours: 1 }), type: 'info' as const },
  ];

  function getColorClasses(color: string) {
    const colors: Record<string, { bg: string; icon: string }> = {
      blue: { bg: 'bg-blue-100 dark:bg-blue-900/30', icon: 'text-blue-500' },
      green: { bg: 'bg-green-100 dark:bg-green-900/30', icon: 'text-green-500' },
      yellow: { bg: 'bg-yellow-100 dark:bg-yellow-900/30', icon: 'text-yellow-500' },
      red: { bg: 'bg-red-100 dark:bg-red-900/30', icon: 'text-red-500' },
    };
    return colors[color] || colors.blue;
  }

  function getActivityTypeClass(type: string) {
    const types: Record<string, string> = {
      success: 'bg-green-500',
      info: 'bg-blue-500',
      warning: 'bg-yellow-500',
      error: 'bg-red-500',
    };
    return types[type] || types.info;
  }
</script>

<div class="space-y-6 fade-in">
  <!-- 欢迎信息 -->
  <div class="bg-gradient-to-r from-[#409eff] to-[#66b1ff] rounded-lg p-6 text-white">
    <h1 class="text-2xl font-bold mb-2">
      {$t('dashboard.welcome')}, {$authStore.user?.name || 'Admin'}!
    </h1>
    <p class="text-white/80">{$t('dashboard.welcomeSubtitle')}</p>
  </div>

  <!-- 统计卡片 -->
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
    {#each stats as stat}
      {@const colors = getColorClasses(stat.color)}
      <div class="bg-white dark:bg-[#1d1d1d] rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500 dark:text-gray-400 mb-1">{$t(stat.title)}</p>
            <p class="text-2xl font-semibold text-gray-800 dark:text-white">{stat.value}</p>
            <p class="text-xs text-green-500 mt-1">{stat.change} {$t('dashboard.comparedLastMonth')}</p>
          </div>
          <div class="w-12 h-12 rounded-lg {colors.bg} flex items-center justify-center">
            <i class="pi {stat.icon} text-2xl {colors.icon}"></i>
          </div>
        </div>
      </div>
    {/each}
  </div>

  <!-- 主内容区 -->
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
    <!-- 快捷入口 -->
    <div class="lg:col-span-2 bg-white dark:bg-[#1d1d1d] rounded-lg p-6 shadow-sm">
      <h2 class="text-lg font-semibold text-gray-800 dark:text-white mb-4">
        <i class="pi pi-th-large mr-2 text-[#409eff]"></i>
        {$t('dashboard.quickAccess')}
      </h2>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        {#each quickLinks as link}
          <button
            type="button"
            onclick={() => navigate(link.path)}
            class="group p-4 rounded-lg border border-gray-100 dark:border-gray-800 hover:border-[#409eff] hover:bg-[#409eff]/5 transition-all cursor-pointer text-left w-full"
          >
            <div class="w-10 h-10 rounded-lg bg-[#409eff]/10 flex items-center justify-center mb-3 group-hover:bg-[#409eff] transition-colors">
              <i class="pi {link.icon} text-lg text-[#409eff] group-hover:text-white"></i>
            </div>
            <h3 class="font-medium text-gray-800 dark:text-white text-sm mb-1">{$t(link.title)}</h3>
            <p class="text-xs text-gray-500 dark:text-gray-400">{link.desc}</p>
          </button>
        {/each}
      </div>
    </div>

    <!-- 最近活动 -->
    <div class="bg-white dark:bg-[#1d1d1d] rounded-lg p-6 shadow-sm">
      <h2 class="text-lg font-semibold text-gray-800 dark:text-white mb-4">
        <i class="pi pi-history mr-2 text-[#409eff]"></i>
        {$t('dashboard.recentActivity')}
      </h2>
      <div class="space-y-4">
        {#each activities as activity}
          <div class="flex items-start gap-3">
            <div class="w-2 h-2 rounded-full {getActivityTypeClass(activity.type)} mt-2"></div>
            <div class="flex-1">
              <p class="text-sm text-gray-800 dark:text-gray-200">
                <span class="font-medium">{activity.user}</span>
                <span class="text-gray-500 dark:text-gray-400"> {activity.action}</span>
              </p>
              <p class="text-xs text-gray-400 mt-0.5">{activity.time}</p>
            </div>
          </div>
        {/each}
      </div>
      <button class="w-full mt-4 py-2 text-sm text-[#409eff] hover:bg-[#409eff]/5 rounded transition-colors">
        {$t('common.viewAll')} →
      </button>
    </div>
  </div>

  <!-- 系统信息 -->
  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
    <div class="bg-white dark:bg-[#1d1d1d] rounded-lg p-5 shadow-sm">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
          <i class="pi pi-check-circle text-xl text-green-500"></i>
        </div>
        <div>
          <p class="text-sm text-gray-500 dark:text-gray-400">{$t('dashboard.systemStatus')}</p>
          <p class="font-semibold text-green-500">{$t('dashboard.statusNormal')}</p>
        </div>
      </div>
    </div>
    <div class="bg-white dark:bg-[#1d1d1d] rounded-lg p-5 shadow-sm">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
          <i class="pi pi-server text-xl text-blue-500"></i>
        </div>
        <div>
          <p class="text-sm text-gray-500 dark:text-gray-400">{$t('dashboard.serverLoad')}</p>
          <p class="font-semibold text-gray-800 dark:text-white">23%</p>
        </div>
      </div>
    </div>
    <div class="bg-white dark:bg-[#1d1d1d] rounded-lg p-5 shadow-sm">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-lg bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center">
          <i class="pi pi-database text-xl text-purple-500"></i>
        </div>
        <div>
          <p class="text-sm text-gray-500 dark:text-gray-400">{$t('dashboard.databaseConnections')}</p>
          <p class="font-semibold text-gray-800 dark:text-white">12 / 100</p>
        </div>
      </div>
    </div>
  </div>
</div>
