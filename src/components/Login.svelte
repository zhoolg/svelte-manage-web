<script lang="ts">
  import { authStore } from '../stores/authStore';
  import { permissionStore } from '../stores/permissionStore';
  import { onMount } from 'svelte';
  import { Toggle, Button, Label } from 'bits-ui';
  import Captcha from './Captcha.svelte';
  import { t } from '../lib/locales';
  import { APP_CONFIG } from '../config';
  import { authApi } from '../api/auth';

  let username = '';
  let password = '';
  let captcha = '';
  let loading = false;
  let bgUrl = '';
  let showPassword = false;
  let captchaComponent: Captcha; // 验证码组件引用

  // 获取 Bing 每日壁纸
  onMount(() => {
    const img = new Image();
    img.src = 'https://bing.img.run/1920x1080.php';
    img.onload = () => {
      bgUrl = img.src;
    };
  });

  // 登录
  async function handleLogin() {
    const translate = $t;

    // 表单验证
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

    // 验证码验证
    const validationResult = captchaComponent?.validate(captcha);
    if (!validationResult || !validationResult.valid) {
      if (typeof window !== 'undefined' && (window as any).toast) {
        const reason = validationResult?.reason || 'login.captchaError';
        (window as any).toast.warning(translate(reason));
      }
      captcha = '';
      captchaComponent?.refresh();
      return;
    }

    loading = true;

    try {
      // 调用登录 API
      const response = await authApi.login({
        username: username.trim(),
        password: password.trim(),
        captcha: captcha.trim(),
      });

      // 设置用户信息和token
      authStore.login(response.data.token, response.data.user);

      // 设置用户权限
      permissionStore.setPermissions(response.data.permissions);
      permissionStore.setIsAdmin(response.data.isAdmin);

      // 显示成功消息
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.success(translate('login.loginSuccess'));
      }

    } catch (error: any) {
      // 登录失败处理
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.error(error.message || translate('login.loginFailed'));
      }
      captcha = '';
      captchaComponent?.refresh();
    } finally {
      loading = false;
    }
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
    <!-- 升级后的卡片：更强的阴影层次 -->
    <div class="bg-white dark:bg-[#1a1a1a] backdrop-blur-xl rounded-3xl shadow-[0_8px_40px_rgb(0,0,0,0.12)] dark:shadow-[0_8px_40px_rgb(0,0,0,0.5)] overflow-hidden border border-gray-100/20 dark:border-gray-800/50">
      <!-- 头部 -->
      <div class="pt-12 pb-6 text-center relative">

        <!-- 标题组 -->
        <div class="space-y-3 px-6">
          <h1 class="text-4xl font-bold bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 dark:from-white dark:via-gray-100 dark:to-white bg-clip-text text-transparent tracking-tight animate-[fadeIn_0.6s_ease-out]">
            {APP_CONFIG.title}
          </h1>

          <p class="text-sm text-gray-500 dark:text-gray-400 font-medium tracking-wide animate-[fadeIn_0.8s_ease-out]">
            {$t('dashboard.welcome')}
          </p>
        </div>
      </div>

      <!-- 表单 - Material Design 风格 -->
      <div class="px-10 pt-4 pb-12">
        <div class="space-y-6">
          <!-- 用户名 - 底线风格 with bits-ui Label -->
          <div class="group relative">
            <Label.Root
              for="username-input"
              class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2 ml-1 transition-colors duration-200 group-focus-within:text-[#409eff]"
            >
              {$t('login.username')}
            </Label.Root>
            <div class="relative">
              <span class="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#409eff] transition-all duration-300">
                <i class="pi pi-user text-lg"></i>
              </span>
              <input
                id="username-input"
                type="text"
                bind:value={username}
                onkeydown={handleKeyDown}
                class="w-full h-12 pl-9 pr-4 text-base bg-transparent text-gray-900 dark:text-white border-b-2 border-gray-200 dark:border-gray-700 placeholder-gray-400 focus:outline-none focus:border-[#409eff] transition-all duration-300"
                placeholder="admin / user"
              />
              <!-- 底线动画 -->
              <div class="absolute bottom-0 left-0 h-0.5 w-0 bg-gradient-to-r from-[#409eff] to-[#66b1ff] group-focus-within:w-full transition-all duration-500"></div>
            </div>
          </div>

          <!-- 密码 - 底线风格 with bits-ui Label -->
          <div class="group relative">
            <Label.Root
              for="password-input"
              class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2 ml-1 transition-colors duration-200 group-focus-within:text-[#409eff]"
            >
              {$t('login.password')}
            </Label.Root>
            <div class="relative">
              <span class="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#409eff] transition-all duration-300">
                <i class="pi pi-lock text-lg"></i>
              </span>
              <input
                id="password-input"
                type={showPassword ? 'text' : 'password'}
                bind:value={password}
                onkeydown={handleKeyDown}
                class="w-full h-12 pl-9 pr-12 text-base bg-transparent text-gray-900 dark:text-white border-b-2 border-gray-200 dark:border-gray-700 placeholder-gray-400 focus:outline-none focus:border-[#409eff] transition-all duration-300"
                placeholder="••••••••"
              />
              <!-- 底线动画 -->
              <div class="absolute bottom-0 left-0 h-0.5 w-0 bg-gradient-to-r from-[#409eff] to-[#66b1ff] group-focus-within:w-full transition-all duration-500"></div>

              <!-- 密码显示/隐藏切换 - bits-ui Toggle -->
              <Toggle.Root
                bind:pressed={showPassword}
                class="absolute right-0 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#409eff] hover:scale-110 transition-all duration-200 cursor-pointer outline-none p-1"
              >
                <i class="pi {showPassword ? 'pi-eye' : 'pi-eye-slash'} text-lg"></i>
              </Toggle.Root>
            </div>
          </div>

          <!-- 验证码 - 底线风格 with bits-ui Label -->
          <div class="group relative">
            <Label.Root
              for="captcha-input"
              class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-2 ml-1 transition-colors duration-200 group-focus-within:text-[#409eff]"
            >
              {$t('login.captcha')}
            </Label.Root>
            <div class="flex gap-3 items-end">
              <div class="relative flex-1">
                <span class="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#409eff] transition-all duration-300">
                  <i class="pi pi-shield text-lg"></i>
                </span>
                <input
                  id="captcha-input"
                  type="text"
                  bind:value={captcha}
                  onkeydown={handleKeyDown}
                  onclick={() => captchaComponent?.recordInteraction?.()}
                  onfocus={() => captchaComponent?.recordInteraction?.()}
                  maxlength="4"
                  class="w-full h-12 pl-9 pr-4 text-base bg-transparent text-gray-900 dark:text-white border-b-2 border-gray-200 dark:border-gray-700 placeholder-gray-400 focus:outline-none focus:border-[#409eff] transition-all duration-300"
                  placeholder="ABCD"
                />
                <!-- 底线动画 -->
                <div class="absolute bottom-0 left-0 h-0.5 w-0 bg-gradient-to-r from-[#409eff] to-[#66b1ff] group-focus-within:w-full transition-all duration-500"></div>
              </div>
              <div class="flex-shrink-0 rounded-xl overflow-hidden shadow-md">
                <Captcha
                  bind:this={captchaComponent}
                  width={120}
                  height={48}
                  length={4}
                />
              </div>
            </div>
          </div>

          <!-- 登录按钮 - 升级版 with bits-ui Button -->
          <div class="relative mt-8 group/btn">
            <!-- 按钮光晕背景 -->
            <div class="absolute -inset-1 bg-gradient-to-r from-[#409eff] via-[#5dade2] to-[#66b1ff] rounded-2xl opacity-0 group-hover/btn:opacity-100 blur-xl transition-all duration-500"></div>

            <Button.Root
              type="button"
              onclick={handleLogin}
              disabled={loading}
              class="relative w-full h-14 bg-gradient-to-r from-[#409eff] via-[#5dade2] to-[#66b1ff] hover:from-[#66b1ff] hover:via-[#5dade2] hover:to-[#409eff] active:scale-[0.97] text-white text-base font-semibold rounded-2xl transition-all duration-500 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:scale-100 flex items-center justify-center shadow-xl shadow-[#409eff]/30 hover:shadow-2xl hover:shadow-[#409eff]/50 overflow-hidden"
            >
              <!-- 按钮波纹效果背景 -->
              <div class="absolute inset-0 bg-gradient-to-r from-white/0 via-white/20 to-white/0 -translate-x-full group-hover/btn:translate-x-full transition-transform duration-1000"></div>

              <span class="relative z-10 flex items-center">
                {#if loading}
                  <i class="pi pi-spin pi-spinner mr-2 text-lg"></i>
                  {$t('login.logging')}
                {:else}
                  <span class="tracking-wide">{$t('login.loginButton')}</span>
                  <i class="pi pi-arrow-right ml-2 text-sm group-hover/btn:translate-x-1 transition-transform duration-300"></i>
                {/if}
              </span>
            </Button.Root>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <p class="text-center text-xs text-white dark:text-gray-400 mt-8 opacity-60 hover:opacity-100 transition-opacity duration-300">
      {APP_CONFIG.copyright}
    </p>
  </div>
</div>

<style>
  /* 淡入动画 */
  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  /* 确保动画在 Tailwind 的 animate-[fadeIn_*] 中可用 */
  :global(.animate-\[fadeIn_0\.6s_ease-out\]) {
    animation: fadeIn 0.6s ease-out;
  }

  :global(.animate-\[fadeIn_0\.8s_ease-out\]) {
    animation: fadeIn 0.8s ease-out;
  }
</style>
