<script lang="ts">
  /**
   * 应用主入口
   * ============================================================
   *
   * 功能：
   * - 路由管理
   * - 登录状态判断
   * - 全局组件挂载（Toast、Modal）
   * - 主题初始化
   */
  import { onMount } from 'svelte';
  import { isLoggedIn, authStore } from './stores/authStore';
  import { initTheme } from './stores/settingsStore';
  import { currentPath, navigate } from './stores/routerStore';
  import { getModuleByPath, toModuleConfig } from './config/app.modules';
  import Toast from './components/Toast.svelte';
  import Modal from './components/Modal.svelte';
  import Login from './components/Login.svelte';
  import AdminLayout from './components/AdminLayout.svelte';
  import Dashboard from './components/Dashboard.svelte';
  import CrudPage from './components/CrudPage.svelte';
  import Settings from './components/Settings.svelte';
  import Logs from './components/Logs.svelte';
  import Dict from './components/Dict.svelte';
  import Agents from './components/Agents.svelte';
  import './app.css';

  // 初始化
  onMount(() => {
    authStore.init();
    initTheme();
  });

  // 根据路径获取模块配置
  $: currentModule = getModuleByPath($currentPath);
  $: moduleConfig = currentModule ? toModuleConfig(currentModule) : null;
</script>

<svelte:head>
  <title>Svelte 管理后台</title>
</svelte:head>

<!-- Toast 和 Modal 组件 -->
<Toast />
<Modal />

<!-- 主内容：根据登录状态显示不同页面 -->
{#if $isLoggedIn}
  <AdminLayout>
    {#if $currentPath === '/'}
      <Dashboard />
    {:else if currentModule?.customPage === 'Settings'}
      <Settings />
    {:else if currentModule?.customPage === 'Logs'}
      <Logs />
    {:else if currentModule?.customPage === 'Dict'}
      <Dict />
    {:else if currentModule?.customPage === 'Agents'}
      <Agents />
    {:else if currentModule?.customPage === 'Dashboard'}
      <Dashboard />
    {:else if moduleConfig}
      <!-- CRUD 页面 -->
      <CrudPage config={moduleConfig} />
    {:else}
      <!-- 自定义页面或 404 -->
      <div class="bg-white dark:bg-[#1d1d1d] rounded-lg p-8 text-center">
        <i class="pi pi-exclamation-triangle text-6xl text-yellow-500 mb-4"></i>
        <h2 class="text-xl font-semibold text-gray-800 dark:text-white mb-2">页面开发中</h2>
        <p class="text-gray-500 dark:text-gray-400 mb-4">
          当前页面 "{$currentPath}" 正在开发中，敬请期待！
        </p>
        <button
          on:click={() => navigate('/')}
          class="px-4 py-2 bg-[#409eff] hover:bg-[#66b1ff] text-white rounded transition-colors"
        >
          返回首页
        </button>
      </div>
    {/if}
  </AdminLayout>
{:else}
  <Login />
{/if}
