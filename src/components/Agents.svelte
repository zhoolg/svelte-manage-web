<script lang="ts">
  /**
   * 代理商管理页面（简化版）
   */
  import { Button } from 'bits-ui';
  import { t } from '../lib/locales';

  interface Agent {
    id: number;
    name: string;
    contact: string;
    phone: string;
    level: string;
    status: 'pending' | 'approved' | 'rejected';
    createTime: string;
  }

  const mockAgents: Agent[] = [
    { id: 1, name: '北京代理商', contact: '张三', phone: '13800138000', level: '金牌代理', status: 'approved', createTime: '2024-01-15 10:00:00' },
    { id: 2, name: '上海代理商', contact: '李四', phone: '13800138001', level: '银牌代理', status: 'approved', createTime: '2024-01-14 10:00:00' },
    { id: 3, name: '广州代理商', contact: '王五', phone: '13800138002', level: '普通代理', status: 'pending', createTime: '2024-01-13 10:00:00' },
  ];

  let agents = mockAgents;
  let searchKeyword = '';

  $: filteredAgents = agents.filter(a =>
    a.name.includes(searchKeyword) || a.contact.includes(searchKeyword)
  );

  function getStatusColor(status: string) {
    switch (status) {
      case 'approved': return 'bg-green-50 text-green-600 dark:bg-green-900/30';
      case 'pending': return 'bg-yellow-50 text-yellow-600 dark:bg-yellow-900/30';
      case 'rejected': return 'bg-red-50 text-red-600 dark:bg-red-900/30';
      default: return 'bg-gray-50 text-gray-600 dark:bg-gray-900/30';
    }
  }

  function getStatusText(status: string) {
    switch (status) {
      case 'approved': return 'agent.approved';
      case 'pending': return 'agent.pending';
      case 'rejected': return 'agent.rejected';
      default: return status;
    }
  }
</script>

<div class="fade-in space-y-4">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-xl font-semibold text-gray-800 dark:text-white">{$t('agent.title')}</h1>
      <p class="text-sm text-gray-500 mt-1">{$t('agent.subtitle')}</p>
    </div>
  </div>

  <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-4">
    <div class="flex items-center gap-4">
      <input
        type="text"
        bind:value={searchKeyword}
        placeholder={$t('agent.searchPlaceholder')}
        class="flex-1 h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] focus:border-[#409eff] focus:outline-none"
      />
      <Button.Root class="h-9 px-4 bg-[#409eff] text-white text-sm rounded-lg hover:bg-[#66b1ff] transition-colors">
        <i class="pi pi-search mr-1"></i>
        {$t('common.search')}
      </Button.Root>
      <Button.Root class="h-9 px-4 bg-[#67c23a] text-white text-sm rounded-lg hover:bg-[#85ce61] transition-colors">
        <i class="pi pi-plus mr-1"></i>
        {$t('agent.addAgent')}
      </Button.Root>
    </div>
  </div>

  <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
    <table class="w-full">
      <thead>
        <tr class="bg-gray-50 dark:bg-[#262626]">
          <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">{$t('agent.name')}</th>
          <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">{$t('agent.contact')}</th>
          <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">{$t('agent.phone')}</th>
          <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">{$t('agent.level')}</th>
          <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">{$t('common.status')}</th>
          <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">{$t('agent.applyTime')}</th>
          <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">{$t('common.action')}</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
        {#each filteredAgents as agent}
          <tr class="hover:bg-gray-50 dark:hover:bg-[#262626] transition-colors">
            <td class="px-4 py-3 text-sm text-gray-800 dark:text-white font-medium">{agent.name}</td>
            <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{agent.contact}</td>
            <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400 font-mono">{agent.phone}</td>
            <td class="px-4 py-3 text-sm">
              <span class="px-2 py-1 text-xs rounded bg-blue-50 text-blue-600 dark:bg-blue-900/30">
                {agent.level}
              </span>
            </td>
            <td class="px-4 py-3 text-sm">
              <span class="px-2 py-1 text-xs rounded {getStatusColor(agent.status)}">
                {$t(getStatusText(agent.status))}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{agent.createTime}</td>
            <td class="px-4 py-3 text-sm">
              <Button.Root class="text-[#409eff] hover:text-[#66b1ff] mr-3">{$t('common.view')}</Button.Root>
              {#if agent.status === 'pending'}
                <Button.Root class="text-[#67c23a] hover:text-[#85ce61] mr-3">{$t('agent.approve')}</Button.Root>
                <Button.Root class="text-red-500 hover:text-red-600">{$t('agent.reject')}</Button.Root>
              {:else}
                <Button.Root class="text-[#409eff] hover:text-[#66b1ff]">{$t('common.edit')}</Button.Root>
              {/if}
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  </div>
</div>
