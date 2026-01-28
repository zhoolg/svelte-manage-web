<script lang="ts">
  /**
   * 数据字典管理页面（简化版）
   */
  import { writable } from 'svelte/store';
  import { Button } from 'bits-ui';
  import { Plus, Book } from 'lucide-svelte';
  import { t } from '../lib/locales';

  interface DictType {
    id: number;
    name: string;
    code: string;
    status: 'enabled' | 'disabled';
    remark: string;
    createTime: string;
  }

  interface DictItem {
    id: number;
    typeId: number;
    label: string;
    value: string;
    sort: number;
    status: 'enabled' | 'disabled';
    remark: string;
  }

  // 模拟数据
  const mockDictTypes: DictType[] = [
    {
      id: 1,
      name: '用户状态',
      code: 'user_status',
      status: 'enabled',
      remark: '用户账号状态',
      createTime: '2024-01-01 10:00:00',
    },
    {
      id: 2,
      name: '性别',
      code: 'gender',
      status: 'enabled',
      remark: '性别选项',
      createTime: '2024-01-01 10:00:00',
    },
    {
      id: 3,
      name: '代理商等级',
      code: 'agent_level',
      status: 'enabled',
      remark: '代理商等级分类',
      createTime: '2024-01-01 10:00:00',
    },
  ];

  const mockDictItems: Record<number, DictItem[]> = {
    1: [
      { id: 1, typeId: 1, label: '正常', value: '1', sort: 1, status: 'enabled', remark: '' },
      { id: 2, typeId: 1, label: '禁用', value: '0', sort: 2, status: 'enabled', remark: '' },
    ],
    2: [
      { id: 4, typeId: 2, label: '男', value: '1', sort: 1, status: 'enabled', remark: '' },
      { id: 5, typeId: 2, label: '女', value: '2', sort: 2, status: 'enabled', remark: '' },
    ],
    3: [
      { id: 7, typeId: 3, label: '普通代理', value: '1', sort: 1, status: 'enabled', remark: '' },
      { id: 8, typeId: 3, label: '银牌代理', value: '2', sort: 2, status: 'enabled', remark: '' },
      { id: 9, typeId: 3, label: '金牌代理', value: '3', sort: 3, status: 'enabled', remark: '' },
    ],
  };

  let dictTypes = mockDictTypes;
  let selectedType: DictType | null = null;
  let dictItems: DictItem[] = [];
  let searchKeyword = '';

  function handleSelectType(type: DictType) {
    selectedType = type;
    dictItems = mockDictItems[type.id] || [];
  }

  $: filteredTypes = dictTypes.filter(
    t => t.name.includes(searchKeyword) || t.code.includes(searchKeyword)
  );
</script>

<div class="fade-in">
  <div class="mb-6">
    <h1 class="text-xl font-semibold text-gray-800 dark:text-white">{$t('dict.title')}</h1>
    <p class="text-sm text-gray-500 mt-1">{$t('dict.subtitle')}</p>
  </div>

  <div class="grid grid-cols-12 gap-4">
    <!-- 左侧：字典类型列表 -->
    <div class="col-span-4">
      <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm">
        <div class="p-4 border-b border-gray-100 dark:border-gray-800">
          <input
            type="text"
            bind:value={searchKeyword}
            placeholder={$t('dict.searchPlaceholder')}
            class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] focus:border-[#409eff] focus:outline-none"
          />
        </div>
        <div class="divide-y divide-gray-100 dark:divide-gray-800">
          {#each filteredTypes as type}
            <Button.Root
              onclick={() => handleSelectType(type)}
              class="w-full px-4 py-3 text-left hover:bg-gray-50 dark:hover:bg-[#262626] transition-colors {selectedType?.id ===
              type.id
                ? 'bg-[#409eff]/10 border-l-4 border-[#409eff]'
                : ''}"
            >
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-sm font-medium text-gray-800 dark:text-white">{type.name}</p>
                  <p class="text-xs text-gray-500 mt-0.5">{type.code}</p>
                </div>
                <span
                  class="px-2 py-1 text-xs rounded {type.status === 'enabled'
                    ? 'bg-green-50 text-green-600 dark:bg-green-900/30'
                    : 'bg-gray-50 text-gray-600 dark:bg-gray-900/30'}"
                >
                  {type.status === 'enabled' ? $t('common.enable') : $t('common.disable')}
                </span>
              </div>
            </Button.Root>
          {/each}
        </div>
      </div>
    </div>

    <!-- 右侧：字典项列表 -->
    <div class="col-span-8">
      {#if selectedType}
        <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm">
          <div
            class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between"
          >
            <h3 class="text-base font-medium text-gray-800 dark:text-white">
              {selectedType.name} - {$t('dict.dictItems')}
            </h3>
            <Button.Root
              class="h-8 px-3 bg-[#409eff] text-white text-sm rounded hover:bg-[#66b1ff] transition-colors flex items-center gap-1"
            >
              <Plus size={14} />
              {$t('common.add')}
            </Button.Root>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="bg-gray-50 dark:bg-[#262626]">
                  <th
                    class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase"
                    >{$t('dict.label')}</th
                  >
                  <th
                    class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase"
                    >{$t('dict.value')}</th
                  >
                  <th
                    class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase"
                    >{$t('dict.sort')}</th
                  >
                  <th
                    class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase"
                    >{$t('common.status')}</th
                  >
                  <th
                    class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase"
                    >{$t('common.action')}</th
                  >
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
                {#each dictItems as item}
                  <tr class="hover:bg-gray-50 dark:hover:bg-[#262626] transition-colors">
                    <td class="px-4 py-3 text-sm text-gray-800 dark:text-white">{item.label}</td>
                    <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400 font-mono"
                      >{item.value}</td
                    >
                    <td class="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{item.sort}</td>
                    <td class="px-4 py-3 text-sm">
                      <span
                        class="px-2 py-1 text-xs rounded {item.status === 'enabled'
                          ? 'bg-green-50 text-green-600 dark:bg-green-900/30'
                          : 'bg-gray-50 text-gray-600 dark:bg-gray-900/30'}"
                      >
                        {item.status === 'enabled' ? $t('common.enable') : $t('common.disable')}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-sm">
                      <Button.Root class="text-[#409eff] hover:text-[#66b1ff] mr-3"
                        >{$t('common.edit')}</Button.Root
                      >
                      <Button.Root class="text-red-500 hover:text-red-600"
                        >{$t('common.delete')}</Button.Root
                      >
                    </td>
                  </tr>
                {/each}
              </tbody>
            </table>
          </div>
        </div>
      {:else}
        <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm p-12 text-center">
          <Book size={48} class="text-gray-300 dark:text-gray-600 mx-auto mb-4" />
          <p class="text-gray-500 dark:text-gray-400">{$t('dict.selectTypePrompt')}</p>
        </div>
      {/if}
    </div>
  </div>
</div>
