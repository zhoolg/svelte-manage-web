<script lang="ts">
  import { authStore } from '../stores/authStore';
  import { permissionStore } from '../stores/permissionStore';
  import { onDestroy, onMount } from 'svelte';
  import { Toggle, Button, Label } from 'bits-ui';
  import {
    User,
    Lock,
    Eye,
    EyeOff,
    Loader2,
    ArrowRight,
    ShieldCheck,
    KeyRound,
    Fingerprint,
  } from 'lucide-svelte';
  import { t } from '../lib/locales';
  import { APP_CONFIG } from '../config';
  import { authApi } from '../api/auth';
  import { encryptLoginCredentials } from '../utils/rsa';
  import { credentialToJson, isPasskeySupported, toGetOptions } from '../utils/passkey';

  let username = '';
  let password = '';
  let captcha = '';
  let captchaId = '';
  let captchaImg = '';
  let loading = false;
  let passkeyLoading = false;
  let captchaLoading = false;
  let captchaCooldown = false;
  let captchaCooldownTimer: ReturnType<typeof setTimeout> | undefined;
  let bgUrl = '';
  let showPassword = false;

  // 拉取图形验证码：答案只在服务端，前端只拿 captchaId + 图片
  async function loadCaptcha(withCooldown = true) {
    if (captchaLoading || (withCooldown && captchaCooldown)) {
      return;
    }

    if (withCooldown) {
      captchaCooldown = true;
      captchaCooldownTimer = setTimeout(() => {
        captchaCooldown = false;
        captchaCooldownTimer = undefined;
      }, 1000);
    }

    captchaLoading = true;
    try {
      const res = await authApi.getCaptcha();
      if (res.data) {
        captchaId = res.data.captchaId;
        captchaImg = res.data.image;
      }
    } catch {
      captchaImg = '';
    } finally {
      captchaLoading = false;
    }
    captcha = '';
  }

  // 获取 Bing 每日壁纸 + 初始化验证码
  onMount(() => {
    const img = new Image();
    img.src = 'https://bing.zhoolg.com';
    img.onload = () => {
      bgUrl = img.src;
    };
    loadCaptcha(false);
  });

  onDestroy(() => {
    if (captchaCooldownTimer) {
      clearTimeout(captchaCooldownTimer);
    }
  });

  function applyLoginResponse(response: any, fallbackUsername: string) {
    const data = response.data as any;
    const user = {
      id: data.user?.id ?? 1,
      username: data.user?.username ?? fallbackUsername,
      name: data.user?.name ?? fallbackUsername,
      avatar: data.user?.avatar,
      roles: data.user?.roles ?? [],
      usingDefaultPassword: data.user?.usingDefaultPassword ?? data.usingDefaultPassword ?? false,
    };
    authStore.login(user);
    permissionStore.setPermissions(data.permissions ?? []);
    permissionStore.setIsAdmin(!!data.isAdmin);
  }

  // 账号密码登录
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

    loading = true;

    try {
      // RSA-OAEP 加密账号和密码（公钥实时取自后端）
      const encrypted = await encryptLoginCredentials(username.trim(), password.trim());

      // 调用后端登录 API（发送加密后的数据）
      const response = await authApi.login({
        accountNo: encrypted.accountNo,
        password: encrypted.password,
        captchaId,
        captcha: captcha.trim(),
      });

      // 只要 response 返回，说明 code 为 0 或 200
      applyLoginResponse(response, username.trim());

      // 显示成功消息
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.success(response.msg || translate('login.loginSuccess'));
      }
    } catch (error: any) {
      // 登录失败处理：验证码为一次性，失败后刷新
      if (typeof window !== 'undefined' && (window as any).toast) {
        (window as any).toast.error(error.message || translate('login.loginFailed'));
      }
      loadCaptcha(false);
    } finally {
      loading = false;
    }
  }

  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
      handleLogin();
    }
  }

  async function handlePasskeyLogin() {
    const translate = $t;
    if (!isPasskeySupported()) {
      (window as any).toast?.warning(translate('login.passkeyNotSupported'));
      return;
    }

    passkeyLoading = true;
    try {
      const start = await authApi.startPasskeyLogin({
        username: username.trim() || undefined,
      });
      if (!start.data?.publicKey || !start.data.requestId) {
        throw new Error(translate('login.passkeyChallengeFailed'));
      }
      const credential = await navigator.credentials.get({
        publicKey: toGetOptions(start.data.publicKey),
      });
      if (!credential) {
        throw new Error(translate('login.passkeyNotSelected'));
      }
      const response = await authApi.finishPasskeyLogin({
        requestId: start.data.requestId,
        credentialJson: credentialToJson(credential),
      });
      if ((response.code === 0 || response.code === 200) && response.data) {
        applyLoginResponse(response, username.trim() || 'Passkey');
        (window as any).toast?.success(response.msg || translate('login.loginSuccess'));
      } else {
        throw new Error(response.msg || translate('login.passkeyError'));
      }
    } catch (error: any) {
      let errorMessage = error.message || translate('login.passkeyError');

      // 处理 WebAuthn 标准错误
      if (error.name === 'NotAllowedError') {
        // 通常是用户取消了生物识别弹窗，或者权限被拒绝
        errorMessage = translate('login.passkeyCancel');
      } else if (error.name === 'TimeoutError') {
        errorMessage = translate('login.passkeyTimeout');
      }

      (window as any).toast?.error(errorMessage);
    } finally {
      passkeyLoading = false;
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
    <div
      class="bg-white dark:bg-[#1a1a1a] backdrop-blur-xl rounded-3xl shadow-[0_8px_40px_rgb(0,0,0,0.12)] dark:shadow-[0_8px_40px_rgb(0,0,0,0.5)] overflow-hidden border border-gray-100/20 dark:border-gray-800/50"
    >
      <!-- 头部 -->
      <div class="pt-8 pb-4 text-center relative">
        <!-- 标题组 -->
        <div class="space-y-2 px-6">
          <h1
            class="text-3xl font-bold bg-gradient-to-r from-gray-900 via-gray-800 to-gray-900 dark:from-white dark:via-gray-100 dark:to-white bg-clip-text text-transparent tracking-tight animate-[fadeIn_0.6s_ease-out]"
          >
            {APP_CONFIG.title}
          </h1>

          <p
            class="text-sm text-gray-500 dark:text-gray-400 font-medium tracking-wide animate-[fadeIn_0.8s_ease-out]"
          >
            {$t('dashboard.welcome')}
          </p>
        </div>
      </div>

      <!-- 表单 - Material Design 风格 -->
      <div class="px-10 pt-2 pb-6">
        <div class="space-y-3">
          <!-- 用户名 - 底线风格 with bits-ui Label -->
          <div class="group relative">
            <Label.Root
              for="username-input"
              class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 ml-1 transition-colors duration-200 group-focus-within:text-[color:var(--color-primary)]"
            >
              {$t('login.username')}
            </Label.Root>
            <div class="relative">
              <span
                class="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[color:var(--color-primary)] transition-all duration-300"
              >
                <User size={18} />
              </span>
              <input
                id="username-input"
                type="text"
                bind:value={username}
                onkeydown={handleKeyDown}
                class="w-full h-10 pl-9 pr-4 text-base bg-transparent text-gray-900 dark:text-white border-b-2 border-gray-200 dark:border-gray-700 placeholder-gray-400 outline-none focus:outline-none focus:border-[color:var(--color-primary)] transition-all duration-300"
                placeholder={$t('login.accountPlaceholder')}
              />
              <!-- 底线动画 -->
              <div
                class="absolute bottom-0 left-0 h-0.5 w-0 bg-[color:var(--color-primary)] group-focus-within:w-full transition-all duration-500"
              ></div>
            </div>
          </div>

          <!-- 密码 - 底线风格 with bits-ui Label -->
          <div class="group relative">
            <Label.Root
              for="password-input"
              class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 ml-1 transition-colors duration-200 group-focus-within:text-[color:var(--color-primary)]"
            >
              {$t('login.password')}
            </Label.Root>
            <div class="relative">
              <span
                class="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[color:var(--color-primary)] transition-all duration-300"
              >
                <Lock size={18} />
              </span>
              <input
                id="password-input"
                type={showPassword ? 'text' : 'password'}
                bind:value={password}
                onkeydown={handleKeyDown}
                class="w-full h-10 pl-9 pr-12 text-base bg-transparent text-gray-900 dark:text-white border-b-2 border-gray-200 dark:border-gray-700 placeholder-gray-400 outline-none focus:outline-none focus:border-[color:var(--color-primary)] transition-all duration-300"
                placeholder={$t('login.passwordPlaceholder')}
              />
              <!-- 底线动画 -->
              <div
                class="absolute bottom-0 left-0 h-0.5 w-0 bg-[color:var(--color-primary)] group-focus-within:w-full transition-all duration-500"
              ></div>

              <!-- 密码显示/隐藏切换 - bits-ui Toggle -->
              <Toggle.Root
                bind:pressed={showPassword}
                class="absolute right-0 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[color:var(--color-primary)] hover:scale-110 transition-all duration-200 cursor-pointer outline-none p-1"
              >
                {#if showPassword}
                  <Eye size={18} />
                {:else}
                  <EyeOff size={18} />
                {/if}
              </Toggle.Root>
            </div>
          </div>

          <!-- 验证码 - 输入框与验证码图片同一行，右侧小方块，仅图片无文字 -->
          <div class="group relative">
            <Label.Root
              for="captcha-input"
              class="block text-xs font-medium text-gray-500 dark:text-gray-400 mb-1 ml-1 transition-colors duration-200 group-focus-within:text-[color:var(--color-primary)]"
            >
              {$t('login.captcha')}
            </Label.Root>
            <div class="flex items-end gap-3">
              <div class="relative flex-1">
                <span
                  class="absolute left-0 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[color:var(--color-primary)] transition-all duration-300"
                >
                  <ShieldCheck size={18} />
                </span>
                <input
                  id="captcha-input"
                  type="text"
                  autocomplete="off"
                  bind:value={captcha}
                  onkeydown={handleKeyDown}
                  class="w-full h-10 pl-9 pr-4 text-base bg-transparent text-gray-900 dark:text-white border-b-2 border-gray-200 dark:border-gray-700 placeholder-gray-400 outline-none focus:outline-none focus:border-[color:var(--color-primary)] transition-all duration-300"
                  placeholder={$t('login.captchaPlaceholder')}
                />
                <!-- 底线动画 -->
                <div
                  class="absolute bottom-0 left-0 h-0.5 w-0 bg-[color:var(--color-primary)] group-focus-within:w-full transition-all duration-500"
                ></div>
              </div>

              <button
                type="button"
                onclick={() => loadCaptcha()}
                disabled={captchaLoading || captchaCooldown}
                title={captchaCooldown ? $t('login.captchaCooldown') : $t('login.captchaRefresh')}
                aria-label={captchaCooldown
                  ? $t('login.captchaCooldown')
                  : $t('login.captchaRefresh')}
                class="h-9 w-24 sm:w-28 rounded-xl overflow-hidden border border-gray-200/80 dark:border-gray-700/80 bg-gray-50 dark:bg-gray-800 flex items-center justify-center cursor-pointer hover:border-[color:var(--color-primary)] hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors disabled:cursor-not-allowed disabled:opacity-60"
              >
                {#if captchaLoading}
                  <Loader2 size={16} class="animate-spin text-gray-400" />
                {:else if captchaImg}
                  <img src={captchaImg} alt="验证码" class="h-full w-full object-cover" />
                {:else}
                  <ShieldCheck size={18} class="text-gray-300" />
                {/if}
              </button>
            </div>
          </div>

          <!-- 登录按钮 - 使用主题主色渐变和光晕 -->
          <div class="relative mt-4 group/btn">
            <!-- 按钮光晕背景，跟随主题主色变化 -->
            <div
              class="absolute -inset-1 rounded-2xl opacity-0 group-hover/btn:opacity-100 blur-xl transition-all duration-500 bg-[color:var(--color-primary-soft)]"
            ></div>

            <Button.Root
              type="button"
              onclick={handleLogin}
              disabled={loading || passkeyLoading}
              class="relative w-full h-11 text-white text-base font-semibold rounded-2xl transition-all duration-500 disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:scale-100 flex items-center justify-center overflow-hidden hover:brightness-105 active:brightness-95 active:scale-[0.97] shadow-xl hover:shadow-2xl"
              style="background-image: linear-gradient(90deg, var(--color-primary), color-mix(in srgb, var(--color-primary) 60%, white));"
            >
              <!-- 按钮波纹效果背景 -->
              <div
                class="absolute inset-0 bg-gradient-to-r from-white/0 via-white/20 to-white/0 -translate-x-full group-hover/btn:translate-x-full transition-transform duration-1000"
              ></div>

              <span class="relative z-10 flex items-center">
                {#if loading}
                  <Loader2 size={18} class="mr-2 animate-spin" />
                  {$t('login.logging')}
                {:else}
                  <span class="tracking-wide">{$t('login.loginButton')}</span>
                  <ArrowRight
                    size={14}
                    class="ml-2 group-hover/btn:translate-x-1 transition-transform duration-300"
                  />
                {/if}
              </span>
            </Button.Root>
          </div>

          <!-- 其他登录方式分割线 - 大厂风格设计 -->
          <div class="relative flex items-center pt-4 pb-2">
            <div class="flex-grow border-t border-gray-100 dark:border-gray-800/60"></div>
            <span
              class="flex-shrink mx-3 text-[10px] text-gray-400 font-semibold tracking-widest uppercase"
            >
              {$t('common.more')}
            </span>
            <div class="flex-grow border-t border-gray-100 dark:border-gray-800/60"></div>
          </div>

          <!-- 第三方/替代登录按钮组 -->
          <div class="flex justify-center">
            <Button.Root
              type="button"
              onclick={handlePasskeyLogin}
              disabled={loading || passkeyLoading}
              title={$t('login.passkeyLogin')}
              class="group/passkey relative flex h-10 w-10 items-center justify-center rounded-full bg-gray-50/80 hover:bg-gray-100 dark:bg-gray-800/40 dark:hover:bg-gray-800/80 text-gray-500 dark:text-gray-400 transition-all duration-300 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <!-- 悬浮时的外圈发光动画 -->
              <div
                class="absolute inset-0 rounded-full opacity-0 group-hover/passkey:opacity-100 ring-2 ring-[color:var(--color-primary)] ring-offset-2 dark:ring-offset-[#1a1a1a] transition-all duration-300"
              ></div>

              {#if passkeyLoading}
                <Loader2 size={18} class="animate-spin" />
              {:else}
                <div class="relative flex items-center justify-center">
                  <Fingerprint
                    size={20}
                    class="absolute transition-all duration-300 opacity-100 group-hover/passkey:opacity-0 group-hover/passkey:scale-50"
                  />
                  <KeyRound
                    size={20}
                    class="transition-all duration-300 opacity-0 scale-50 group-hover/passkey:opacity-100 group-hover/passkey:scale-100 group-hover/passkey:text-[color:var(--color-primary)]"
                  />
                </div>
              {/if}
            </Button.Root>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <p
      class="text-center text-xs text-white dark:text-gray-400 mt-8 opacity-60 hover:opacity-100 transition-opacity duration-300"
    >
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
