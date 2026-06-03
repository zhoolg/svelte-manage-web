<script lang="ts">
  import { onMount } from 'svelte';
  import {
    Activity,
    Bot,
    CheckCircle2,
    CircleAlert,
    Cpu,
    Database,
    HardDrive,
    Loader2,
    LockKeyhole,
    RefreshCw,
    Server,
    ShieldAlert,
    TriangleAlert,
    UserCheck,
    Wifi,
  } from 'lucide-svelte';
  import {
    systemApi,
    type HealthComponent,
    type StatusCheck,
    type SystemStatus,
  } from '../api/system';

  let status = $state<SystemStatus | null>(null);
  let loading = $state(false);
  let error = $state('');

  onMount(() => {
    void loadStatus();
  });

  async function loadStatus() {
    loading = true;
    error = '';
    try {
      const response = await systemApi.getStatus();
      status = normalizeStatus(response.data);
    } catch (err) {
      error = err instanceof Error ? err.message : '系统状态加载失败';
    } finally {
      loading = false;
    }
  }

  function statusLabel(value?: string) {
    if (value === 'UP') return '正常';
    if (value === 'WARN') return '关注';
    if (value === 'DOWN') return '异常';
    return value || '-';
  }

  function statusClass(value?: string) {
    if (value === 'UP')
      return 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-300';
    if (value === 'WARN')
      return 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-300';
    if (value === 'DOWN')
      return 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300';
    return 'border-gray-200 bg-gray-50 text-gray-600 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-300';
  }

  function statusIcon(value?: string) {
    if (value === 'UP') return CheckCircle2;
    if (value === 'WARN') return TriangleAlert;
    if (value === 'DOWN') return CircleAlert;
    return Activity;
  }

  function percent(part: number, total: number) {
    if (!total || total <= 0 || part < 0) return 0;
    return Math.min(100, Math.max(0, Math.round((part / total) * 100)));
  }

  function loadPercent(value: number) {
    return value < 0 ? 0 : Math.round(value * 100);
  }

  function formatBytes(value: number) {
    if (!value || value <= 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let size = value;
    let unit = 0;
    while (size >= 1024 && unit < units.length - 1) {
      size /= 1024;
      unit += 1;
    }
    return `${size.toFixed(size >= 10 || unit === 0 ? 0 : 1)} ${units[unit]}`;
  }

  function formatDate(value?: string | null) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  function formatUptime(seconds: number) {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (days > 0) return `${days} 天 ${hours} 小时`;
    if (hours > 0) return `${hours} 小时 ${minutes} 分钟`;
    return `${minutes} 分钟`;
  }

  function securityInfo(current: Partial<SystemStatus> | null | undefined) {
    return (
      current?.security || {
        activeSessions: 0,
        recentLoginFailures: 0,
        lockedIpCount: 0,
        lastAdminLoginTime: null,
      }
    );
  }

  function normalizeStatus(raw: Partial<SystemStatus> | null | undefined): SystemStatus {
    const runtime = (raw?.runtime || {}) as Partial<SystemStatus['runtime']>;
    const resources = (raw?.resources || {}) as Partial<SystemStatus['resources']>;
    const aiFactory = (raw?.aiFactory || {}) as Partial<SystemStatus['aiFactory']>;

    return {
      status: raw?.status || 'WARN',
      checkedAt: raw?.checkedAt || new Date().toISOString(),
      runtime: {
        application: runtime.application || '-',
        environment: runtime.environment || '-',
        springBootVersion: runtime.springBootVersion || '-',
        javaVersion: runtime.javaVersion || '-',
        os: runtime.os || '-',
        startedAt: runtime.startedAt || '',
        uptimeSeconds: runtime.uptimeSeconds || 0,
      },
      resources: {
        processors: resources.processors || 0,
        systemCpuLoad: resources.systemCpuLoad || 0,
        processCpuLoad: resources.processCpuLoad || 0,
        heapUsedBytes: resources.heapUsedBytes || 0,
        heapMaxBytes: resources.heapMaxBytes || 0,
        diskUsedBytes: resources.diskUsedBytes || 0,
        diskTotalBytes: resources.diskTotalBytes || 0,
      },
      database: normalizeComponent(raw?.database, '数据库状态未返回'),
      redis: normalizeComponent(raw?.redis, 'Redis 状态未返回'),
      aiFactory: {
        status: aiFactory.status || 'WARN',
        enabledDynamicModules: aiFactory.enabledDynamicModules || 0,
        message: aiFactory.message || 'AI 工厂状态未返回',
      },
      security: securityInfo(raw),
      checks: Array.isArray(raw?.checks) ? raw.checks.map(normalizeCheck) : [],
    };
  }

  function normalizeComponent(
    component: Partial<HealthComponent> | null | undefined,
    fallbackMessage: string
  ): HealthComponent {
    return {
      status: component?.status || 'WARN',
      message: component?.message || fallbackMessage,
      latencyMs: component?.latencyMs || 0,
      details: component?.details || {},
    };
  }

  function normalizeCheck(check: Partial<StatusCheck>): StatusCheck {
    return {
      key: check.key || check.label || 'unknown',
      label: check.label || check.key || '未知检查项',
      status: check.status || 'WARN',
      message: check.message || '-',
      latencyMs: check.latencyMs || 0,
    };
  }

  function componentDetails(component: HealthComponent) {
    return Object.entries(component.details || {}).filter(
      ([, value]) => value !== null && value !== undefined && value !== ''
    );
  }
</script>

<div class="space-y-4">
  <div class="flex flex-wrap items-center justify-between gap-3">
    <div>
      <h1 class="text-xl font-semibold text-gray-900 dark:text-white">系统状态</h1>
      <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
        {#if status}
          最近检查：{formatDate(status.checkedAt)}
        {:else}
          查看运行时、依赖服务和 AI 工厂状态
        {/if}
      </p>
    </div>
    <button
      type="button"
      onclick={loadStatus}
      disabled={loading}
      class="inline-flex h-9 items-center gap-2 rounded-md border border-gray-200 bg-white px-3 text-sm text-gray-600 transition-colors hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-60 dark:border-gray-700 dark:bg-gray-900/40 dark:text-gray-300"
    >
      {#if loading}
        <Loader2 size={15} class="animate-spin" />
      {:else}
        <RefreshCw size={15} />
      {/if}
      刷新
    </button>
  </div>

  {#if error}
    <div
      class="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-300"
    >
      {error}
    </div>
  {/if}

  {#if !status && loading}
    <div
      class="flex h-64 items-center justify-center rounded-md border border-dashed border-gray-200 text-sm text-gray-400 dark:border-gray-700"
    >
      <Loader2 size={16} class="mr-2 animate-spin" />
      正在加载系统状态
    </div>
  {:else if status}
    {@const security = securityInfo(status)}
    <section class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
      <div
        class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
      >
        <div class="flex items-center justify-between">
          <span class="text-sm text-gray-500 dark:text-gray-400">总体状态</span>
          <span
            class={`inline-flex h-6 items-center gap-1 rounded border px-2 text-xs ${statusClass(status.status)}`}
          >
            {#if status.status === 'UP'}
              <CheckCircle2 size={13} />
            {:else if status.status === 'WARN'}
              <TriangleAlert size={13} />
            {:else if status.status === 'DOWN'}
              <CircleAlert size={13} />
            {:else}
              <Activity size={13} />
            {/if}
            {statusLabel(status.status)}
          </span>
        </div>
        <p class="mt-3 text-2xl font-semibold text-gray-900 dark:text-white">
          {status.runtime.application}
        </p>
        <p class="mt-1 truncate text-xs text-gray-500 dark:text-gray-400">
          {status.runtime.environment}
        </p>
      </div>

      <div
        class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
      >
        <div class="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
          <span>运行时长</span>
          <Server size={16} />
        </div>
        <p class="mt-3 text-2xl font-semibold text-gray-900 dark:text-white">
          {formatUptime(status.runtime.uptimeSeconds)}
        </p>
        <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
          启动：{formatDate(status.runtime.startedAt)}
        </p>
      </div>

      <div
        class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
      >
        <div class="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
          <span>AI 动态模块</span>
          <Bot size={16} />
        </div>
        <p class="mt-3 text-2xl font-semibold text-gray-900 dark:text-white">
          {status.aiFactory.enabledDynamicModules}
        </p>
        <p class="mt-1 truncate text-xs text-gray-500 dark:text-gray-400">
          {status.aiFactory.message}
        </p>
      </div>

      <div
        class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
      >
        <div class="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
          <span>运行环境</span>
          <Activity size={16} />
        </div>
        <p class="mt-3 text-sm font-medium text-gray-900 dark:text-white">
          Spring Boot {status.runtime.springBootVersion}
        </p>
        <p class="mt-1 truncate text-xs text-gray-500 dark:text-gray-400">
          Java {status.runtime.javaVersion}
        </p>
      </div>
    </section>

    <section
      class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
    >
      <div class="mb-3 flex items-center justify-between">
        <h2 class="text-sm font-medium text-gray-900 dark:text-white">登录与安全运行状态</h2>
        <span class="text-xs text-gray-400">最近 1 小时</span>
      </div>
      <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        {@render SecurityMetric(
          '当前在线会话数',
          String(security.activeSessions),
          '活跃后台会话',
          UserCheck
        )}
        {@render SecurityMetric(
          '最近登录失败次数',
          String(security.recentLoginFailures),
          '密码失败与验证码失败记录',
          ShieldAlert
        )}
        {@render SecurityMetric(
          '当前被锁定 IP 数',
          String(security.lockedIpCount),
          '本实例正在封禁的 IP',
          LockKeyhole
        )}
        {@render SecurityMetric(
          '最近一次管理员登录',
          formatDate(security.lastAdminLoginTime),
          '成功登录记录',
          Activity
        )}
      </div>
    </section>

    <section class="grid gap-3 lg:grid-cols-3">
      <div
        class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
      >
        <div class="mb-3 flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
          <Cpu size={16} class="text-[color:var(--color-primary)]" />
          CPU
        </div>
        <div class="space-y-3">
          {@render MetricBar('系统负载', loadPercent(status.resources.systemCpuLoad), '%')}
          {@render MetricBar('进程负载', loadPercent(status.resources.processCpuLoad), '%')}
        </div>
        <p class="mt-3 text-xs text-gray-500 dark:text-gray-400">
          处理器：{status.resources.processors}
        </p>
      </div>

      <div
        class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
      >
        <div class="mb-3 flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
          <Server size={16} class="text-[color:var(--color-primary)]" />
          内存
        </div>
        {@render MetricBar(
          '堆内存',
          percent(status.resources.heapUsedBytes, status.resources.heapMaxBytes),
          '%'
        )}
        <p class="mt-3 text-xs text-gray-500 dark:text-gray-400">
          {formatBytes(status.resources.heapUsedBytes)} / {formatBytes(
            status.resources.heapMaxBytes
          )}
        </p>
      </div>

      <div
        class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
      >
        <div class="mb-3 flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
          <HardDrive size={16} class="text-[color:var(--color-primary)]" />
          磁盘
        </div>
        {@render MetricBar(
          '工作目录磁盘',
          percent(status.resources.diskUsedBytes, status.resources.diskTotalBytes),
          '%'
        )}
        <p class="mt-3 text-xs text-gray-500 dark:text-gray-400">
          {formatBytes(status.resources.diskUsedBytes)} / {formatBytes(
            status.resources.diskTotalBytes
          )}
        </p>
      </div>
    </section>

    <section
      class="rounded-md border border-gray-100 bg-white dark:border-gray-800 dark:bg-gray-900/30"
    >
      <div
        class="flex items-center justify-between border-b border-gray-100 px-4 py-3 dark:border-gray-800"
      >
        <h2 class="text-sm font-medium text-gray-900 dark:text-white">依赖服务</h2>
        <span class="text-xs text-gray-400">实时探测</span>
      </div>
      <div class="divide-y divide-gray-100 dark:divide-gray-800">
        {#each status.checks as check (check.key)}
          {@render StatusRow(check)}
        {/each}
      </div>
    </section>

    <section class="grid gap-3 lg:grid-cols-2">
      {@render ComponentPanel('数据库', Database, status.database)}
      {@render ComponentPanel('Redis', Wifi, status.redis)}
    </section>

    <section
      class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
    >
      <h2 class="text-sm font-medium text-gray-900 dark:text-white">运行信息</h2>
      <div class="mt-3 grid gap-3 text-sm md:grid-cols-2">
        {@render InfoItem('操作系统', status.runtime.os)}
        {@render InfoItem('应用环境', status.runtime.environment)}
        {@render InfoItem('Spring Boot', status.runtime.springBootVersion)}
        {@render InfoItem('Java', status.runtime.javaVersion)}
      </div>
    </section>
  {/if}
</div>

{#snippet MetricBar(label: string, value: number, suffix: string)}
  <div>
    <div class="mb-1 flex items-center justify-between text-xs">
      <span class="text-gray-500 dark:text-gray-400">{label}</span>
      <span class="font-medium text-gray-700 dark:text-gray-200">{value}{suffix}</span>
    </div>
    <div class="h-2 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
      <div
        class="h-full rounded-full bg-[color:var(--color-primary)] transition-all"
        style={`width: ${value}%`}
      ></div>
    </div>
  </div>
{/snippet}

{#snippet SecurityMetric(label: string, value: string, hint: string, icon: typeof Activity)}
  {@const MetricIcon = icon}
  <div class="rounded-md bg-gray-50 px-3 py-3 dark:bg-gray-800/70">
    <div class="flex items-center justify-between gap-3">
      <span class="text-xs text-gray-500 dark:text-gray-400">{label}</span>
      <MetricIcon size={15} class="shrink-0 text-[color:var(--color-primary)]" />
    </div>
    <p class="mt-2 truncate text-xl font-semibold text-gray-900 dark:text-white">{value}</p>
    <p class="mt-1 truncate text-xs text-gray-400">{hint}</p>
  </div>
{/snippet}

{#snippet StatusRow(check: StatusCheck)}
  {@const RowIcon = statusIcon(check.status)}
  <div class="flex items-center gap-3 px-4 py-3">
    <div
      class={`inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md border ${statusClass(check.status)}`}
    >
      <RowIcon size={15} />
    </div>
    <div class="min-w-0 flex-1">
      <div class="flex items-center gap-2">
        <p class="text-sm font-medium text-gray-900 dark:text-white">{check.label}</p>
        <span
          class={`inline-flex h-5 items-center rounded border px-1.5 text-[11px] ${statusClass(check.status)}`}
        >
          {statusLabel(check.status)}
        </span>
      </div>
      <p class="mt-0.5 truncate text-xs text-gray-500 dark:text-gray-400">{check.message}</p>
    </div>
    <span class="shrink-0 text-xs text-gray-400">{check.latencyMs} ms</span>
  </div>
{/snippet}

{#snippet ComponentPanel(title: string, icon: typeof Database, component: HealthComponent)}
  {@const PanelIcon = icon}
  {@const PanelStatusIcon = statusIcon(component.status)}
  <div
    class="rounded-md border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-gray-900/30"
  >
    <div class="flex items-center justify-between gap-3">
      <div class="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
        <PanelIcon size={16} class="text-[color:var(--color-primary)]" />
        {title}
      </div>
      <span
        class={`inline-flex h-6 items-center gap-1 rounded border px-2 text-xs ${statusClass(component.status)}`}
      >
        <PanelStatusIcon size={13} />
        {statusLabel(component.status)}
      </span>
    </div>
    <p class="mt-3 text-sm text-gray-600 dark:text-gray-300">{component.message}</p>
    <div class="mt-3 flex flex-wrap gap-2">
      <span
        class="inline-flex h-6 items-center rounded bg-gray-50 px-2 text-xs text-gray-500 dark:bg-gray-800 dark:text-gray-400"
      >
        延迟 {component.latencyMs} ms
      </span>
      {#each componentDetails(component) as [key, value]}
        <span
          class="inline-flex h-6 items-center rounded bg-gray-50 px-2 text-xs text-gray-500 dark:bg-gray-800 dark:text-gray-400"
        >
          {key}: {String(value)}
        </span>
      {/each}
    </div>
  </div>
{/snippet}

{#snippet InfoItem(label: string, value: string)}
  <div class="rounded-md bg-gray-50 px-3 py-2 dark:bg-gray-800/70">
    <p class="text-xs text-gray-400">{label}</p>
    <p class="mt-1 break-words text-sm text-gray-800 dark:text-gray-100">{value || '-'}</p>
  </div>
{/snippet}
