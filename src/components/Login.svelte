<script lang="ts">
  import { authStore } from '../stores/authStore';
  import { permissionStore } from '../stores/permissionStore';
  import { onMount } from 'svelte';
  import { Toggle } from 'bits-ui';
  import Captcha from './Captcha.svelte';
  import { t } from '../lib/locales';
  import { APP_CONFIG } from '../config';

  let username = '';
  let password = '';
  let captcha = '';
  let loading = false;
  let bgUrl = '';
  let captchaCode = '';
  let showPassword = false;

  // 简化的测试账号（直接定义，不依赖外部文件）
  const testAccounts = [
    {
      username: 'admin',
      password: 'admin123',
      name: '超级管理员',
      description: '拥有所有权限',
      roles: ['超级管理员']
    },
    {
      username: 'manager',
      password: 'manager123',
      name: '系统管理员',
      description: '拥有大部分管理权限',
      roles: ['管理员']
    },
    {
      username: 'user_admin',
      password: 'user123',
      name: '用户管理员',
      description: '负责用户和代理商管理',
      roles: ['用户管理员']
    },
    {
      username: 'content_admin',
      password: 'content123',
      name: '内容管理员',
      description: '负责内容管理',
      roles: ['内容管理员']
    },
    {
      username: 'operator',
      password: 'operator123',
      name: '运营专员',
      description: '日常运营工作',
      roles: ['运营人员']
    },
    {
      username: 'viewer',
      password: 'viewer123',
      name: '访客',
      description: '只能查看数据',
      roles: ['查看者']
    },
  ];

  // 获取 Bing 每日壁纸
  onMount(() => {
    const img = new Image();
    img.src = 'https://bing.img.run/1920x1080.php';
    img.onload = () => {
      bgUrl = img.src;
    };
  });

  // 验证码验证函数
  function validateCaptcha(input: string, code: string): boolean {
    return input.toLowerCase() === code.toLowerCase();
  }

  // 模拟登录
  function handleLogin() {
    const translate = $t;

    if (!username.trim()) {
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.warning(translate('login.usernameRequired'));
      }
      return;
    }
    if (!password.trim()) {
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.warning(translate('login.passwordRequired'));
      }
      return;
    }
    if (!captcha.trim()) {
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.warning(translate('login.captchaRequired'));
      }
      return;
    }
    if (!validateCaptcha(captcha, captchaCode)) {
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.warning(translate('login.captchaError'));
      }
      captcha = '';
      return;
    }

    loading = true;

    // 模拟API调用
    setTimeout(() => {
      // 查找测试账号
      const account = testAccounts.find(acc => acc.username === username && acc.password === password);

      if (!account) {
        if (typeof window !== 'undefined' && (window as any).toast) {
          (window as any).toast.error(translate('login.loginFailed'));
        }
        loading = false;
        return;
      }

      // 设置用户信息和token
      authStore.login('dev_token_' + Date.now(), {
        username: account.username,
        name: account.name,
        roles: account.roles,
      });

      // 设置用户权限
      // admin用户拥有所有权限
      if (username.toLowerCase() === 'admin') {
        permissionStore.setPermissions(['*']);
        permissionStore.setIsAdmin(true);
      } else if (username === 'manager') {
        // 管理员：大部分权限
        permissionStore.setPermissions([
          'user:view', 'user:add', 'user:edit', 'user:delete', 'user:export',
          'agent:view', 'agent:add', 'agent:edit', 'agent:delete', 'agent:export',
          'faq:view', 'faq:add', 'faq:edit', 'faq:delete', 'faq:export',
          'article:view', 'article:add', 'article:edit', 'article:delete', 'article:export',
          'log:view', 'log:export',
          'dict:view', 'dict:add', 'dict:edit', 'dict:delete',
        ]);
        permissionStore.setIsAdmin(false);
      } else if (username === 'user_admin') {
        // 用户管理员
        permissionStore.setPermissions([
          'user:view', 'user:add', 'user:edit', 'user:export',
          'agent:view', 'agent:add', 'agent:edit', 'agent:approve', 'agent:reject', 'agent:export',
        ]);
        permissionStore.setIsAdmin(false);
      } else if (username === 'content_admin') {
        // 内容管理员
        permissionStore.setPermissions([
          'faq:view', 'faq:add', 'faq:edit', 'faq:delete', 'faq:export',
          'article:view', 'article:add', 'article:edit', 'article:delete', 'article:export',
        ]);
        permissionStore.setIsAdmin(false);
      } else if (username === 'operator') {
        // 运营人员
        permissionStore.setPermissions([
          'user:view', 'agent:view',
          'faq:view', 'faq:add', 'faq:edit',
          'article:view', 'article:add', 'article:edit',
        ]);
        permissionStore.setIsAdmin(false);
      } else {
        // 查看者
        permissionStore.setPermissions([
          'user:view', 'agent:view', 'faq:view', 'article:view', 'log:view',
        ]);
        permissionStore.setIsAdmin(false);
      }

      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.success(translate('login.loginSuccess'));
      }

      loading = false;
    }, 1000);
  }

  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      handleLogin();
    }
  }
</script>

<div
  class="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 bg-cover bg-center bg-no-repeat transition-all duration-1000 relative"
  style={bgUrl ? `background-image: url('${bgUrl}')` : ''}
>
  <!-- 半透明遮罩 -->
  {#if bgUrl}
    <div class="absolute inset-0 bg-black/20"></div>
  {/if}

  <div class="w-full max-w-md px-6 relative z-10">
    <div class="bg-white/95 dark:bg-[#1f1f1f]/95 backdrop-blur-md rounded-2xl shadow-2xl overflow-hidden">
      <!-- 头部 -->
      <div class="pt-10 pb-6 text-center">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-tr from-[#409eff] to-[#66b1ff] rounded-2xl shadow-lg mb-5">
          <i class="pi pi-briefcase text-3xl text-white"></i>
        </div>
        <h1 class="text-2xl font-bold text-gray-800 dark:text-white mb-2">
          {APP_CONFIG.title}
        </h1>
        <p class="text-sm text-gray-500 dark:text-gray-400">
          {$t('dashboard.welcome')}
        </p>
      </div>

      <!-- 表单 -->
      <div class="px-8 pb-10">
        <div class="space-y-5">
          <!-- 用户名 -->
          <div class="group">
            <div class="relative">
              <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#409eff] transition-colors">
                <i class="pi pi-user text-lg"></i>
              </span>
              <input
                type="text"
                bind:value={username}
                onkeydown={handleKeyDown}
                class="w-full h-12 pl-12 pr-4 text-sm bg-gray-50 dark:bg-[#2c2c2c] text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700 rounded-xl placeholder-gray-400 focus:outline-none focus:bg-white dark:focus:bg-[#2c2c2c] focus:border-[#409eff] focus:ring-4 focus:ring-[#409eff]/10 transition-all"
                placeholder={$t('login.username')}
              />
            </div>
          </div>

          <!-- 密码 -->
          <div class="group">
            <div class="relative">
              <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#409eff] transition-colors">
                <i class="pi pi-lock text-lg"></i>
              </span>
              <input
                type={showPassword ? 'text' : 'password'}
                bind:value={password}
                onkeydown={handleKeyDown}
                class="w-full h-12 pl-12 pr-12 text-sm bg-gray-50 dark:bg-[#2c2c2c] text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700 rounded-xl placeholder-gray-400 focus:outline-none focus:bg-white dark:focus:bg-[#2c2c2c] focus:border-[#409eff] focus:ring-4 focus:ring-[#409eff]/10 transition-all"
                placeholder={$t('login.password')}
              />
              <!-- 密码显示/隐藏切换 - 使用 Bits UI Toggle -->
              <Toggle.Root
                bind:pressed={showPassword}
                class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 transition-colors cursor-pointer outline-none"
              >
                <i class="pi {showPassword ? 'pi-eye' : 'pi-eye-slash'} text-lg"></i>
              </Toggle.Root>
            </div>
          </div>

          <!-- 验证码 -->
          <div class="group">
            <div class="flex gap-3">
              <div class="relative flex-1">
                <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#409eff] transition-colors">
                  <i class="pi pi-shield text-lg"></i>
                </span>
                <input
                  type="text"
                  bind:value={captcha}
                  onkeydown={handleKeyDown}
                  maxlength="4"
                  class="w-full h-12 pl-12 pr-4 text-sm bg-gray-50 dark:bg-[#2c2c2c] text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700 rounded-xl placeholder-gray-400 focus:outline-none focus:bg-white dark:focus:bg-[#2c2c2c] focus:border-[#409eff] focus:ring-4 focus:ring-[#409eff]/10 transition-all"
                  placeholder={$t('login.captcha')}
                />
              </div>
              <Captcha
                width={120}
                height={48}
                length={4}
                onChange={(code) => { captchaCode = code; }}
              />
            </div>
          </div>

          <!-- 登录按钮 -->
          <button
            type="button"
            onclick={handleLogin}
            disabled={loading}
            class="w-full h-12 bg-[#409eff] hover:bg-[#66b1ff] active:bg-[#3a8ee6] text-white text-base font-semibold rounded-xl transition-all disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center shadow-lg hover:-translate-y-0.5"
          >
            {#if loading}
              <i class="pi pi-spin pi-spinner mr-2"></i>
              {$t('login.logging')}
            {:else}
              {$t('login.loginButton')}
            {/if}
          </button>

          <p class="text-xs text-center text-gray-400">
            {$t('login.devLogin')}
          </p>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <p class="text-center text-xs text-gray-500 dark:text-gray-400 mt-8">
      {APP_CONFIG.copyright}
    </p>
  </div>
</div>
