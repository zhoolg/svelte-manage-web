<script lang="ts">
  import { Dialog, Tabs, Button } from 'bits-ui';
  import {
    Eye,
    EyeOff,
    KeyRound,
    Loader2,
    Save,
    Trash2,
    Upload,
    UserCircle,
    X,
  } from 'lucide-svelte';
  import { authApi, type PasskeyCredential } from '../api/auth';
  import { uploadImage } from '../api/upload';
  import { authStore } from '../stores/authStore';
  import { t, getTranslator } from '../lib/locales';
  import { getImageUrl } from '../utils/image';
  import { credentialToJson, isPasskeySupported, toCreateOptions } from '../utils/passkey';

  let {
    open = false,
    onOpenChange,
  }: {
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
  } = $props();

  const currentUser = $derived($authStore.user);

  let activeTab = $state('profile');
  let profileName = $state('');
  let avatarUrl = $state('');
  let oldPassword = $state('');
  let newPassword = $state('');
  let confirmPassword = $state('');
  let loadingProfile = $state(false);
  let savingProfile = $state(false);
  let savingPassword = $state(false);
  let addingPasskey = $state(false);
  let loadingPasskeys = $state(false);
  let deletingPasskeyId = $state<number | null>(null);
  let passkeys = $state<PasskeyCredential[]>([]);
  let uploadingAvatar = $state(false);
  let showOldPassword = $state(false);
  let showNewPassword = $state(false);
  let showConfirmPassword = $state(false);
  let avatarInput: HTMLInputElement;
  let initializedForOpen = false;

  const displayName = $derived(currentUser?.name || currentUser?.username || 'Admin');
  const roleLabel = $derived(currentUser?.roles?.[0] || 'viewer');
  const avatarPreview = $derived(
    avatarUrl.trim() ||
      currentUser?.avatar ||
      `https://api.dicebear.com/7.x/avataaars/svg?seed=${currentUser?.username || 'admin'}`
  );
  const avatarSrc = $derived(getImageUrl(avatarPreview) || avatarPreview);

  $effect(() => {
    if (open && !initializedForOpen) {
      initializedForOpen = true;
      resetProfileForm();
      resetPasswordForm();
      void refreshCurrentUser();
      void refreshPasskeys();
    }
    if (!open) {
      initializedForOpen = false;
    }
  });

  function handleOpenChange(nextOpen: boolean) {
    onOpenChange?.(nextOpen);
  }

  function resetProfileForm() {
    profileName = currentUser?.name || currentUser?.username || '';
    avatarUrl = currentUser?.avatar || '';
  }

  function resetPasswordForm() {
    oldPassword = '';
    newPassword = '';
    confirmPassword = '';
    showOldPassword = false;
    showNewPassword = false;
    showConfirmPassword = false;
  }

  async function refreshCurrentUser() {
    loadingProfile = true;
    try {
      const response = await authApi.getMe();
      if (response.data) {
        authStore.setUser(response.data);
        profileName = response.data.name || response.data.username || '';
        avatarUrl = response.data.avatar || '';
      }
    } catch {
      resetProfileForm();
    } finally {
      loadingProfile = false;
    }
  }

  async function refreshPasskeys() {
    loadingPasskeys = true;
    try {
      const response = await authApi.listPasskeys();
      passkeys = response.data || [];
    } catch {
      passkeys = [];
    } finally {
      loadingPasskeys = false;
    }
  }

  async function handleSaveProfile() {
    const name = profileName.trim();
    if (!name) {
      (window as any).toast?.warning('姓名不能为空');
      return;
    }

    savingProfile = true;
    try {
      const response = await authApi.updateProfile({ name, avatar: avatarUrl.trim() || null });
      if (response.data) {
        authStore.setUser(response.data);
        profileName = response.data.name || name;
        avatarUrl = response.data.avatar || '';
      }
      (window as any).toast?.success(response.msg || '资料已更新');
    } catch (error) {
      (window as any).toast?.error(error instanceof Error ? error.message : '资料更新失败');
    } finally {
      savingProfile = false;
    }
  }

  function triggerAvatarUpload() {
    avatarInput?.click();
  }

  async function handleAvatarFileChange(event: Event) {
    const target = event.currentTarget as HTMLInputElement;
    const file = target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      (window as any).toast?.warning('请选择图片文件');
      target.value = '';
      return;
    }

    uploadingAvatar = true;
    try {
      const url = await uploadImage(file);
      avatarUrl = url;
      (window as any).toast?.success('头像上传成功');
    } catch (error) {
      (window as any).toast?.error(error instanceof Error ? error.message : '头像上传失败');
    } finally {
      uploadingAvatar = false;
      target.value = '';
    }
  }

  async function handleChangePassword() {
    if (!oldPassword.trim()) {
      (window as any).toast?.warning('请输入旧密码');
      return;
    }
    if (newPassword.length < 6) {
      (window as any).toast?.warning('新密码至少 6 位');
      return;
    }
    if (newPassword !== confirmPassword) {
      (window as any).toast?.warning('两次输入的新密码不一致');
      return;
    }

    savingPassword = true;
    try {
      const response = await authApi.changePassword({
        oldPassword,
        newPassword,
        confirmPassword,
      });
      resetPasswordForm();
      authStore.updateUser({ usingDefaultPassword: false });
      (window as any).toast?.success(response.msg || '密码已修改');
    } catch (error) {
      (window as any).toast?.error(error instanceof Error ? error.message : '密码修改失败');
    } finally {
      savingPassword = false;
    }
  }

  async function handleAddPasskey() {
    if (!isPasskeySupported()) {
      (window as any).toast?.warning('当前浏览器不支持 Passkey');
      return;
    }
    addingPasskey = true;
    try {
      const start = await authApi.startPasskeyRegistration();
      if (!start.data?.publicKey || !start.data.requestId) {
        throw new Error('Passkey 挑战创建失败');
      }
      const credential = await navigator.credentials.create({
        publicKey: toCreateOptions(start.data.publicKey),
      });
      if (!credential) {
        throw new Error('未创建 Passkey');
      }
      const response = await authApi.finishPasskeyRegistration({
        requestId: start.data.requestId,
        credentialJson: credentialToJson(credential),
        displayName: currentUser?.username ? `${currentUser.username} Passkey` : 'Passkey',
      });
      await refreshPasskeys();
      (window as any).toast?.success(response.msg || 'Passkey 已添加');
    } catch (error) {
      (window as any).toast?.error(error instanceof Error ? error.message : 'Passkey 添加失败');
    } finally {
      addingPasskey = false;
    }
  }

  async function handleDeletePasskey(passkey: PasskeyCredential) {
    const translate = getTranslator();
    const confirmed = await (window as any).confirm({
      title: translate('login.deletePasskeyTitle'),
      content: translate('login.deletePasskeyConfirm', { name: passkey.displayName || 'Passkey' }),
      type: 'danger',
      confirmText: translate('common.delete'),
      cancelText: translate('common.cancel'),
    });

    if (!confirmed) {
      return;
    }
    deletingPasskeyId = passkey.id;
    try {
      const response = await authApi.deletePasskey(passkey.id);
      passkeys = passkeys.filter(item => item.id !== passkey.id);
      (window as any).toast?.success(response.msg || 'Passkey 已删除');
    } catch (error) {
      (window as any).toast?.error(error instanceof Error ? error.message : 'Passkey 删除失败');
    } finally {
      deletingPasskeyId = null;
    }
  }

  function formatPasskeyTime(value?: string | null) {
    if (!value) return '从未使用';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
</script>

<Dialog.Root {open} onOpenChange={handleOpenChange}>
  <Dialog.Portal>
    <Dialog.Overlay class="fixed inset-0 z-50 bg-black/45" />
    <Dialog.Content
      class="fixed left-1/2 top-1/2 z-50 flex h-[560px] max-h-[calc(100vh-48px)] w-[780px] max-w-[calc(100vw-32px)] -translate-x-1/2 -translate-y-1/2 flex-col overflow-hidden rounded-lg border border-gray-100 bg-white shadow-xl dark:border-gray-800 dark:bg-[#1d1d1d]"
    >
      <div
        class="flex h-14 shrink-0 items-center justify-between border-b border-gray-100 px-5 dark:border-gray-800"
      >
        <Dialog.Title class="text-base font-medium text-gray-900 dark:text-white">
          用户中心
        </Dialog.Title>
        <Dialog.Close
          class="text-gray-400 transition-colors hover:text-gray-600 dark:hover:text-gray-200"
        >
          <X size={16} />
        </Dialog.Close>
      </div>

      <div
        class="grid min-h-0 flex-1 grid-cols-[240px_minmax(0,1fr)] overflow-hidden max-md:grid-cols-1"
      >
        <aside
          class="border-r border-gray-100 bg-gray-50/80 p-5 dark:border-gray-800 dark:bg-gray-900/20 max-md:border-b max-md:border-r-0"
        >
          <div class="flex items-center gap-3">
            <img
              src={avatarSrc}
              alt="avatar"
              class="h-12 w-12 rounded-full border border-white bg-gray-100 object-cover shadow-sm dark:border-gray-800 dark:bg-gray-800"
            />
            <div class="min-w-0">
              <p class="truncate text-sm font-medium text-gray-900 dark:text-white">
                {displayName}
              </p>
              <p class="truncate text-xs text-gray-500 dark:text-gray-400">
                {currentUser?.username || '-'}
              </p>
            </div>
          </div>

          <div class="mt-5 space-y-2 text-xs">
            <div class="flex items-center justify-between gap-3">
              <span class="text-gray-500 dark:text-gray-400">用户 ID</span>
              <span class="font-medium text-gray-700 dark:text-gray-200"
                >{currentUser?.id || '-'}</span
              >
            </div>
            <div class="flex items-center justify-between gap-3">
              <span class="text-gray-500 dark:text-gray-400">角色</span>
              <span
                class="inline-flex h-6 items-center rounded border border-[color:var(--color-primary)]/20 bg-[color:var(--color-primary-subtle)] px-2 text-[color:var(--color-primary)]"
              >
                {roleLabel}
              </span>
            </div>
          </div>
        </aside>

        <Tabs.Root bind:value={activeTab} class="flex min-h-0 flex-col">
          <Tabs.List class="flex h-11 shrink-0 border-b border-gray-100 px-4 dark:border-gray-800">
            <Tabs.Trigger
              value="profile"
              class="relative flex items-center gap-2 px-3 text-sm text-gray-500 transition-colors data-[state=active]:text-[color:var(--color-primary)] dark:text-gray-400"
            >
              <UserCircle size={15} />
              基本资料
              <span
                class="absolute inset-x-3 bottom-0 hidden h-0.5 rounded-full bg-[color:var(--color-primary)] data-[state=active]:block"
              ></span>
            </Tabs.Trigger>
            <Tabs.Trigger
              value="password"
              class="relative flex items-center gap-2 px-3 text-sm text-gray-500 transition-colors data-[state=active]:text-[color:var(--color-primary)] dark:text-gray-400"
            >
              <KeyRound size={15} />
              修改密码
              <span
                class="absolute inset-x-3 bottom-0 hidden h-0.5 rounded-full bg-[color:var(--color-primary)] data-[state=active]:block"
              ></span>
            </Tabs.Trigger>
            <Tabs.Trigger
              value="passkey"
              class="relative flex items-center gap-2 px-3 text-sm text-gray-500 transition-colors data-[state=active]:text-[color:var(--color-primary)] dark:text-gray-400"
            >
              <KeyRound size={15} />
              Passkey
              <span
                class="absolute inset-x-3 bottom-0 hidden h-0.5 rounded-full bg-[color:var(--color-primary)] data-[state=active]:block"
              ></span>
            </Tabs.Trigger>
          </Tabs.List>

          <div class="min-h-0 flex-1 overflow-y-auto p-5">
            <Tabs.Content value="profile" class="space-y-4">
              <div>
                <label
                  for="profile-username"
                  class="mb-1.5 block text-sm text-gray-600 dark:text-gray-400"
                >
                  登录账号
                </label>
                <input
                  id="profile-username"
                  type="text"
                  value={currentUser?.username || ''}
                  disabled
                  class="h-9 w-full rounded-md border border-gray-200 bg-gray-50 px-3 text-sm text-gray-500 disabled:cursor-not-allowed dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400"
                />
              </div>
              <div>
                <label
                  for="profile-name"
                  class="mb-1.5 block text-sm text-gray-600 dark:text-gray-400"
                >
                  显示姓名
                </label>
                <input
                  id="profile-name"
                  type="text"
                  bind:value={profileName}
                  maxlength="64"
                  disabled={loadingProfile || savingProfile}
                  class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 text-sm text-gray-800 outline-none transition-colors focus:border-[color:var(--color-primary)] focus:ring-1 focus:ring-[color:color-mix(in_srgb,var(--color-primary)_20%,transparent)] disabled:cursor-not-allowed disabled:opacity-60 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                />
              </div>
              <div>
                <label
                  for="profile-avatar"
                  class="mb-1.5 block text-sm text-gray-600 dark:text-gray-400"
                >
                  头像
                </label>
                <input
                  bind:this={avatarInput}
                  id="profile-avatar"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  onchange={handleAvatarFileChange}
                  disabled={loadingProfile || savingProfile || uploadingAvatar}
                />
                <div class="flex items-center gap-3">
                  <img
                    src={avatarSrc}
                    alt="avatar preview"
                    class="h-9 w-9 shrink-0 rounded-full border border-gray-200 bg-gray-100 object-cover dark:border-gray-700 dark:bg-gray-800"
                  />
                  <Button.Root
                    type="button"
                    onclick={triggerAvatarUpload}
                    disabled={loadingProfile || savingProfile || uploadingAvatar}
                    class="inline-flex h-9 items-center gap-2 rounded-md border border-gray-200 px-3 text-sm text-gray-600 transition-colors hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] disabled:cursor-not-allowed disabled:opacity-60 dark:border-gray-700 dark:text-gray-300"
                  >
                    {#if uploadingAvatar}
                      <Loader2 size={14} class="animate-spin" />
                    {:else}
                      <Upload size={14} />
                    {/if}
                    上传头像
                  </Button.Root>
                  {#if avatarUrl}
                    <button
                      type="button"
                      onclick={() => (avatarUrl = '')}
                      disabled={loadingProfile || savingProfile || uploadingAvatar}
                      class="h-9 rounded-md px-2 text-sm text-gray-400 transition-colors hover:text-[#f56c6c] disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      移除
                    </button>
                  {/if}
                </div>
              </div>
            </Tabs.Content>

            <Tabs.Content value="password" class="space-y-4">
              <div>
                <label
                  for="profile-old-password"
                  class="mb-1.5 block text-sm text-gray-600 dark:text-gray-400"
                >
                  旧密码
                </label>
                <div class="relative">
                  <input
                    id="profile-old-password"
                    type={showOldPassword ? 'text' : 'password'}
                    bind:value={oldPassword}
                    class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 pr-10 text-sm text-gray-800 outline-none transition-colors focus:border-[color:var(--color-primary)] focus:ring-1 focus:ring-[color:color-mix(in_srgb,var(--color-primary)_20%,transparent)] dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                  />
                  <button
                    type="button"
                    onclick={() => (showOldPassword = !showOldPassword)}
                    class="absolute right-2 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center text-gray-400 transition-colors hover:text-[color:var(--color-primary)]"
                    aria-label="切换旧密码显示"
                  >
                    {#if showOldPassword}<EyeOff size={15} />{:else}<Eye size={15} />{/if}
                  </button>
                </div>
              </div>
              <div>
                <label
                  for="profile-new-password"
                  class="mb-1.5 block text-sm text-gray-600 dark:text-gray-400"
                >
                  新密码
                </label>
                <div class="relative">
                  <input
                    id="profile-new-password"
                    type={showNewPassword ? 'text' : 'password'}
                    bind:value={newPassword}
                    class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 pr-10 text-sm text-gray-800 outline-none transition-colors focus:border-[color:var(--color-primary)] focus:ring-1 focus:ring-[color:color-mix(in_srgb,var(--color-primary)_20%,transparent)] dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                  />
                  <button
                    type="button"
                    onclick={() => (showNewPassword = !showNewPassword)}
                    class="absolute right-2 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center text-gray-400 transition-colors hover:text-[color:var(--color-primary)]"
                    aria-label="切换新密码显示"
                  >
                    {#if showNewPassword}<EyeOff size={15} />{:else}<Eye size={15} />{/if}
                  </button>
                </div>
              </div>
              <div>
                <label
                  for="profile-confirm-password"
                  class="mb-1.5 block text-sm text-gray-600 dark:text-gray-400"
                >
                  确认密码
                </label>
                <div class="relative">
                  <input
                    id="profile-confirm-password"
                    type={showConfirmPassword ? 'text' : 'password'}
                    bind:value={confirmPassword}
                    class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 pr-10 text-sm text-gray-800 outline-none transition-colors focus:border-[color:var(--color-primary)] focus:ring-1 focus:ring-[color:color-mix(in_srgb,var(--color-primary)_20%,transparent)] dark:border-gray-700 dark:bg-gray-800 dark:text-gray-100"
                  />
                  <button
                    type="button"
                    onclick={() => (showConfirmPassword = !showConfirmPassword)}
                    class="absolute right-2 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center text-gray-400 transition-colors hover:text-[color:var(--color-primary)]"
                    aria-label="切换确认密码显示"
                  >
                    {#if showConfirmPassword}<EyeOff size={15} />{:else}<Eye size={15} />{/if}
                  </button>
                </div>
              </div>
            </Tabs.Content>

            <Tabs.Content value="passkey" class="space-y-4">
              <div
                class="rounded-md border border-gray-100 bg-gray-50/80 p-4 dark:border-gray-800 dark:bg-gray-900/20"
              >
                <div class="flex items-start justify-between gap-4">
                  <div class="min-w-0">
                    <p class="text-sm font-medium text-gray-900 dark:text-white">
                      Passkey
                      <span class="ml-2 text-xs font-normal text-gray-400">
                        {passkeys.length} 个
                      </span>
                    </p>
                    <p class="mt-1 text-xs leading-5 text-gray-500 dark:text-gray-400">
                      使用设备指纹、面容或系统安全密钥登录管理后台。
                    </p>
                  </div>
                  <Button.Root
                    type="button"
                    onclick={handleAddPasskey}
                    disabled={addingPasskey}
                    class="inline-flex h-9 shrink-0 items-center gap-2 rounded-md px-3 text-sm text-white transition-colors disabled:cursor-not-allowed disabled:opacity-60 hover:brightness-105"
                    style="background-color: var(--color-primary);"
                  >
                    {#if addingPasskey}
                      <Loader2 size={14} class="animate-spin" />
                    {:else}
                      <KeyRound size={14} />
                    {/if}
                    添加
                  </Button.Root>
                </div>
              </div>

              <div class="space-y-2">
                {#if loadingPasskeys}
                  <div
                    class="flex h-28 items-center justify-center rounded-md border border-dashed border-gray-200 text-sm text-gray-400 dark:border-gray-700"
                  >
                    <Loader2 size={15} class="mr-2 animate-spin" />
                    加载 Passkey
                  </div>
                {:else if passkeys.length === 0}
                  <div
                    class="flex h-28 flex-col items-center justify-center rounded-md border border-dashed border-gray-200 text-sm text-gray-400 dark:border-gray-700"
                  >
                    <KeyRound size={20} class="mb-2" />
                    暂未绑定 Passkey
                  </div>
                {:else}
                  {#each passkeys as passkey (passkey.id)}
                    <div
                      class="flex items-center gap-3 rounded-md border border-gray-100 bg-white p-3 transition-colors hover:border-[color:var(--color-primary)]/30 dark:border-gray-800 dark:bg-gray-900/20"
                    >
                      <div
                        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-[color:var(--color-primary-subtle)] text-[color:var(--color-primary)]"
                      >
                        <KeyRound size={17} />
                      </div>
                      <div class="min-w-0 flex-1">
                        <div class="flex min-w-0 items-center gap-2">
                          <p class="truncate text-sm font-medium text-gray-900 dark:text-white">
                            {passkey.displayName || 'Passkey'}
                          </p>
                          <span
                            class="inline-flex h-5 shrink-0 items-center rounded border border-emerald-200 bg-emerald-50 px-1.5 text-[11px] text-emerald-600 dark:border-emerald-900/60 dark:bg-emerald-950/30 dark:text-emerald-300"
                          >
                            已启用
                          </span>
                        </div>
                        <p class="mt-1 truncate text-xs text-gray-500 dark:text-gray-400">
                          最近使用：{formatPasskeyTime(passkey.lastUsedTime)}
                          <span class="mx-1 text-gray-300">/</span>
                          创建：{formatPasskeyTime(passkey.createTime)}
                        </p>
                      </div>
                      <button
                        type="button"
                        onclick={() => handleDeletePasskey(passkey)}
                        disabled={deletingPasskeyId === passkey.id || addingPasskey}
                        class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-gray-400 transition-colors hover:bg-red-50 hover:text-[#f56c6c] disabled:cursor-not-allowed disabled:opacity-60 dark:hover:bg-red-950/30"
                        aria-label="删除 Passkey"
                        title="删除"
                      >
                        {#if deletingPasskeyId === passkey.id}
                          <Loader2 size={14} class="animate-spin" />
                        {:else}
                          <Trash2 size={14} />
                        {/if}
                      </button>
                    </div>
                  {/each}
                {/if}
              </div>
            </Tabs.Content>
          </div>

          <div
            class="flex h-14 shrink-0 items-center justify-between border-t border-gray-100 px-5 dark:border-gray-800"
          >
            <div class="flex items-center gap-2 text-xs text-gray-400">
              {#if activeTab === 'profile' && loadingProfile}
                <Loader2 size={13} class="animate-spin" />
                <span>{$t('common.loading')}</span>
              {/if}
            </div>
            <div class="flex items-center gap-2">
              <Dialog.Close
                class="h-8 rounded-md border border-gray-200 px-3 text-sm text-gray-600 transition-colors hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] dark:border-gray-700 dark:text-gray-300"
              >
                {$t('common.cancel')}
              </Dialog.Close>
              {#if activeTab === 'profile'}
                <Button.Root
                  type="button"
                  onclick={handleSaveProfile}
                  disabled={savingProfile || loadingProfile}
                  class="inline-flex h-8 items-center gap-2 rounded-md px-3 text-sm text-white transition-colors disabled:cursor-not-allowed disabled:opacity-60 hover:brightness-105"
                  style="background-color: var(--color-primary);"
                >
                  {#if savingProfile}
                    <Loader2 size={14} class="animate-spin" />
                  {:else}
                    <Save size={14} />
                  {/if}
                  保存资料
                </Button.Root>
              {:else if activeTab === 'password'}
                <Button.Root
                  type="button"
                  onclick={handleChangePassword}
                  disabled={savingPassword}
                  class="inline-flex h-8 items-center gap-2 rounded-md px-3 text-sm text-white transition-colors disabled:cursor-not-allowed disabled:opacity-60 hover:brightness-105"
                  style="background-color: var(--color-primary);"
                >
                  {#if savingPassword}
                    <Loader2 size={14} class="animate-spin" />
                  {:else}
                    <KeyRound size={14} />
                  {/if}
                  修改密码
                </Button.Root>
              {:else}
                <Button.Root
                  type="button"
                  onclick={handleAddPasskey}
                  disabled={addingPasskey}
                  class="inline-flex h-8 items-center gap-2 rounded-md px-3 text-sm text-white transition-colors disabled:cursor-not-allowed disabled:opacity-60 hover:brightness-105"
                  style="background-color: var(--color-primary);"
                >
                  {#if addingPasskey}
                    <Loader2 size={14} class="animate-spin" />
                  {:else}
                    <KeyRound size={14} />
                  {/if}
                  添加 Passkey
                </Button.Root>
              {/if}
            </div>
          </div>
        </Tabs.Root>
      </div>
    </Dialog.Content>
  </Dialog.Portal>
</Dialog.Root>
