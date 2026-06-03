<script lang="ts">
  /**
   * 应用主入口
   * ============================================================
   *
   * Copyright 2025 zhoolg
   * Licensed under GPL-3.0 License
   *
   * 功能：
   * - 路由管理
   * - 登录状态判断
   * - 全局组件挂载（Toast、Modal）
   * - 主题初始化
   * - 浏览器兼容性检测
   */
  import { onMount } from 'svelte';
  import { Tooltip } from 'bits-ui';
  import { isLoggedIn, authStore } from './stores/authStore';
  import { initTheme } from './stores/settingsStore';
  import { currentPath, navigate, routeRefreshVersion } from './stores/routerStore';
  import {
    clearRemoteModules,
    getModuleByPath,
    loadRemoteModules,
    modulesVersion,
    toModuleConfig,
  } from './config/app.modules';
  import { loadRemoteMenu } from './config/menu';
  import { APP_CONFIG } from './config';
  import { initBrowserCompatibility } from './utils/browser-compatibility';
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
  import AiModules from './components/AiModules.svelte';
  import SystemStatus from './components/SystemStatus.svelte';
  import NotFound from './components/NotFound.svelte';
  import './app.css';

  let metadataReady = false;
  let metadataLoading = false;

  // 初始化
  onMount(() => {
    authStore.init();
    initTheme();
    initBrowserCompatibility();
  });

  async function initializeMetadata() {
    if (metadataLoading) return;
    metadataLoading = true;
    const modulesLoaded = await loadRemoteModules();
    const menuLoaded = modulesLoaded ? await loadRemoteMenu() : false;
    metadataReady = modulesLoaded && menuLoaded;
    metadataLoading = false;
  }

  $: if ($isLoggedIn && !metadataReady && !metadataLoading) {
    void initializeMetadata();
  }

  $: if (!$isLoggedIn && metadataReady) {
    metadataReady = false;
    clearRemoteModules();
  }

  function resolveCurrentModule(path: string, _version: number) {
    return getModuleByPath(path);
  }

  // 根据路径获取模块配置
  $: currentModule = resolveCurrentModule($currentPath, $modulesVersion);
  $: moduleConfig = currentModule ? toModuleConfig(currentModule) : null;
</script>

<svelte:head>
  <title>{APP_CONFIG.title}</title>
</svelte:head>

<!-- Toast 和 Modal 组件 -->
<Toast />
<Modal />

<!-- Tooltip Provider 包裹整个应用 -->
<Tooltip.Provider>
  <!-- 主内容：根据登录状态显示不同页面 -->
  {#if $isLoggedIn}
    <AdminLayout>
      {#key `${$currentPath}:${$routeRefreshVersion}`}
        {#if !metadataReady}
          <div class="flex min-h-[50vh] items-center justify-center text-sm text-gray-500">
            正在加载后台菜单...
          </div>
        {:else if $currentPath === '/'}
          <Dashboard />
        {:else if $currentPath === '/preferences'}
          <Settings />
        {:else if currentModule?.customPage === 'Settings'}
          <Settings />
        {:else if currentModule?.customPage === 'Logs'}
          <Logs />
        {:else if currentModule?.customPage === 'Dict'}
          <Dict />
        {:else if currentModule?.customPage === 'Agents'}
          <Agents />
        {:else if currentModule?.customPage === 'AiModules'}
          <AiModules />
        {:else if currentModule?.customPage === 'SystemStatus'}
          <SystemStatus />
        {:else if currentModule?.customPage === 'Dashboard'}
          <Dashboard />
        {:else if moduleConfig}
          <!-- CRUD 页面 -->
          <CrudPage config={moduleConfig} />
        {:else}
          <!-- 404 页面 -->
          <NotFound path={$currentPath} />
        {/if}
      {/key}
    </AdminLayout>
  {:else}
    <Login />
  {/if}
</Tooltip.Provider>
