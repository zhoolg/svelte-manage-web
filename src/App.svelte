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
  import { APP_CONFIG } from './config';
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
  import NotFound from './components/NotFound.svelte';
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
  <title>{APP_CONFIG.title}</title>
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
      <!-- 404 页面 -->
      <NotFound path={$currentPath} />
    {/if}
  </AdminLayout>
{:else}
  <Login />
{/if}
