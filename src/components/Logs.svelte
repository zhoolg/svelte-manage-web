<script lang="ts">
  /**
   * 操作日志页面
   */
  import { writable } from 'svelte/store';
  import { t } from '../lib/locales';

  interface LogItem {
    id: number;
    username: string;
    module: string;
    action: string;
    ip: string;
    location: string;
    browser: string;
    os: string;
    status: 'success' | 'error';
    message: string;
    createTime: string;
  }

  // 模拟日志数据
  const mockLogs: LogItem[] = [
    { id: 1, username: 'admin', module: '用户管理', action: '新增用户', ip: '192.168.1.100', location: '北京市', browser: 'Chrome 120', os: 'Windows 10', status: 'success', message: '操作成功', createTime: '2024-01-15 14:30:25' },
    { id: 2, username: 'admin', module: '系统登录', action: '用户登录', ip: '192.168.1.100', location: '北京市', browser: 'Chrome 120', os: 'Windows 10', status: 'success', message: '登录成功', createTime: '2024-01-15 14:25:10' },
    { id: 3, username: 'zhangsan', module: '代理商管理', action: '编辑代理商', ip: '192.168.1.101', location: '上海市', browser: 'Firefox 121', os: 'macOS 14', status: 'success', message: '操作成功', createTime: '2024-01-15 13:45:30' },
    { id: 4, username: 'lisi', module: '问答管理', action: '删除问答', ip: '192.168.1.102', location: '广州市', browser: 'Safari 17', os: 'macOS 14', status: 'error', message: '权限不足', createTime: '2024-01-15 12:20:15' },
    { id: 5, username: 'admin', module: '系统设置', action: '修改配置', ip: '192.168.1.100', location: '北京市', browser: 'Chrome 120', os: 'Windows 10', status: 'success', message: '操作成功', createTime: '2024-01-15 11:10:45' },
    { id: 6, username: 'wangwu', module: '用户管理', action: '重置密码', ip: '192.168.1.103', location: '深圳市', browser: 'Edge 120', os: 'Windows 11', status: 'success', message: '操作成功', createTime: '2024-01-15 10:30:20' },
    { id: 7, username: 'admin', module: '系统登录', action: '用户登出', ip: '192.168.1.100', location: '北京市', browser: 'Chrome 120', os: 'Windows 10', status: 'success', message: '登出成功', createTime: '2024-01-14 18:00:00' },
    { id: 8, username: 'zhangsan', module: '代理商管理', action: '新增代理商', ip: '192.168.1.101', location: '上海市', browser: 'Firefox 121', os: 'macOS 14', status: 'error', message: '数据验证失败', createTime: '2024-01-14 16:45:30' },
  ];

  let logs = mockLogs;
  let searchForm = {
    username: '',
    module: '',
    status: '',
    dateRange: ''
  };
  let selectedLog: LogItem | null = null;

  const modules = ['系统登录', '用户管理', '代理商管理', '问答管理', '系统设置'];

  function handleSearch() {
    console.log('搜索:', searchForm);
  }

  function handleReset() {
    searchForm = { username: '', module: '', status: '', dateRange: '' };
  }

  function handleExport() {
    console.log('导出日志');
  }
</script>

<div class="fade-in space-y-4">
  <!-- 页面标题 -->
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-xl font-semibold text-gray-800 dark:text-white">{$t('logs.title')}</h1>
      <p class="text-sm text-gray-500 mt-1">{$t('dashboard.viewLogs')}</p>
    </div>
  </div>

  <!-- 搜索区域 -->
  <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-4">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <div>
        <label for="username-input" class="block text-sm text-gray-600 dark:text-gray-400 mb-1">{$t('logs.operator')}</label>
        <input
          id="username-input"
          type="text"
          bind:value={searchForm.username}
          placeholder={$t('login.username')}
          class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] text-gray-800 dark:text-white focus:border-[#409eff] focus:outline-none"
        />
      </div>
      <div>
        <label for="module-select" class="block text-sm text-gray-600 dark:text-gray-400 mb-1">{$t('logs.module')}</label>
        <select
          id="module-select"
          bind:value={searchForm.module}
          class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] text-gray-800 dark:text-white focus:border-[#409eff] focus:outline-none"
        >
          <option value="">{$t('logs.allModules')}</option>
          {#each modules as m}
            <option value={m}>{m}</option>
          {/each}
        </select>
      </div>
      <div>
        <label for="status-select" class="block text-sm text-gray-600 dark:text-gray-400 mb-1">{$t('common.status')}</label>
        <select
          id="status-select"
          bind:value={searchForm.status}
          class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] text-gray-800 dark:text-white focus:border-[#409eff] focus:outline-none"
        >
          <option value="">{$t('logs.allStatus')}</option>
          <option value="success">{$t('common.success')}</option>
          <option value="error">{$t('common.failed')}</option>
        </select>
      </div>
      <div class="flex items-end gap-2">
        <button
          onclick={handleSearch}
          class="h-9 px-4 bg-[#409eff] text-white text-sm rounded-lg hover:bg-[#66b1ff] transition-colors"
        >
          <i class="pi pi-search mr-1"></i>
          {$t('common.search')}
        </button>
        <button
          onclick={handleReset}
          class="h-9 px-4 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 text-sm rounded-lg hover:border-[#409eff] hover:text-[#409eff] transition-colors"
        >
          <i class="pi pi-refresh mr-1"></i>
          {$t('common.reset')}
        </button>
        <button
          onclick={handleExport}
          class="h-9 px-4 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400 text-sm rounded-lg hover:border-[#67c23a] hover:text-[#67c23a] transition-colors"
        >
          <i class="pi pi-download mr-1"></i>
          {$t('common.export')}
        </button>
      </div>
    </div>
  </div>

  <!-- 日志表格 -->
  <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
    <div class="overflow-x-auto">
      <table class="w-full">
        <thead>
          <tr class="bg-gray-50 dark:bg-[#262626]">
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('logs.logId')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('logs.operator')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('logs.module')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('logs.action')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('logs.ip')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('logs.location')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('common.status')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('logs.time')}</th>
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">{$t('common.action')}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
          {#each logs as log}
            <tr class="hover:bg-gray-50 dark:hover:bg-[#262626] transition-colors">
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{log.id}</td>
              <td class="px-4 py-3 text-sm text-gray-800 dark:text-white">{log.username}</td>
              <td class="px-4 py-3 text-sm">
                <span class="px-2 py-1 text-xs rounded bg-blue-50 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400">
                  {log.module}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{log.action}</td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400 font-mono">{log.ip}</td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{log.location}</td>
              <td class="px-4 py-3 text-sm">
                <span class="px-2 py-1 text-xs rounded {log.status === 'success'
                  ? 'bg-green-50 text-green-600 dark:bg-green-900/30 dark:text-green-400'
                  : 'bg-red-50 text-red-600 dark:bg-red-900/30 dark:text-red-400'}">
                  {log.status === 'success' ? $t('common.success') : $t('common.failed')}
                </span>
              </td>
              <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{log.createTime}</td>
              <td class="px-4 py-3 text-sm">
                <button
                  onclick={() => selectedLog = log}
                  class="text-[#409eff] hover:text-[#66b1ff] transition-colors"
                >
                  {$t('common.detail')}
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div class="px-4 py-3 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between">
      <div class="text-sm text-gray-500">
        {$t('table.total', { total: logs.length })}
      </div>
      <div class="flex items-center gap-2">
        <button class="px-3 py-1 text-sm border border-gray-200 dark:border-gray-700 rounded hover:border-[#409eff] hover:text-[#409eff] transition-colors disabled:opacity-50" disabled>
          {$t('table.prev')}
        </button>
        <span class="px-3 py-1 text-sm bg-[#409eff] text-white rounded">1</span>
        <button class="px-3 py-1 text-sm border border-gray-200 dark:border-gray-700 rounded hover:border-[#409eff] hover:text-[#409eff] transition-colors disabled:opacity-50" disabled>
          {$t('table.next')}
        </button>
      </div>
    </div>
  </div>

  <!-- 详情弹窗 -->
  {#if selectedLog}
    <div class="fixed inset-0 z-50 flex items-center justify-center">
      <div
        class="absolute inset-0 bg-black/50"
        onclick={() => selectedLog = null}
        onkeydown={(e) => e.key === 'Enter' && (selectedLog = null)}
        role="button"
        tabindex="0"
        aria-label={$t('logs.closeDialog')}
      ></div>
      <div class="relative w-full max-w-lg mx-4 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-xl">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 dark:border-gray-800">
          <h3 class="text-lg font-medium text-gray-800 dark:text-white">{$t('logs.detail')}</h3>
          <button
            onclick={() => selectedLog = null}
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            aria-label={$t('common.close')}
          >
            <i class="pi pi-times"></i>
          </button>
        </div>
        <div class="p-6 space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <div class="text-sm text-gray-500">{$t('logs.logId')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.id}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.operator')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.username}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.module')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.module}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.action')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.action}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.ip')}</div>
              <p class="text-gray-800 dark:text-white font-mono">{selectedLog.ip}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.location')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.location}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.browser')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.browser}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.os')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.os}</p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('common.status')}</div>
              <p class="{selectedLog.status === 'success' ? 'text-green-600' : 'text-red-600'}">
                {selectedLog.status === 'success' ? $t('common.success') : $t('common.failed')}
              </p>
            </div>
            <div>
              <div class="text-sm text-gray-500">{$t('logs.time')}</div>
              <p class="text-gray-800 dark:text-white">{selectedLog.createTime}</p>
            </div>
          </div>
          <div>
            <div class="text-sm text-gray-500">{$t('logs.returnMessage')}</div>
            <p class="text-gray-800 dark:text-white">{selectedLog.message}</p>
          </div>
        </div>
        <div class="px-6 py-4 border-t border-gray-100 dark:border-gray-800 flex justify-end">
          <button
            onclick={() => selectedLog = null}
            class="px-4 py-2 text-sm border border-gray-200 dark:border-gray-700 rounded-lg hover:border-[#409eff] hover:text-[#409eff] transition-colors"
          >
            {$t('common.close')}
          </button>
        </div>
      </div>
    </div>
  {/if}
</div>
