<script lang="ts" context="module">
  export interface ToastMessage {
    id: number;
    type: 'success' | 'error' | 'warning' | 'info';
    message: string;
  }
</script>

<script lang="ts">
  import { writable } from 'svelte/store';
  import { onDestroy } from 'svelte';
  import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-svelte';
  import { getTranslator } from '../lib/locales';

  // Toast store
  const toasts = writable<ToastMessage[]>([]);
  let toastId = 0;

  // 获取翻译函数
  const t = getTranslator();

  // 导出toast函数供全局使用
  export const toast = {
    success: (message: string) => addToast('success', message),
    error: (message: string) => addToast('error', message),
    warning: (message: string) => addToast('warning', message),
    info: (message: string) => addToast('info', message),
  };

  function addToast(type: ToastMessage['type'], message: string) {
    const id = ++toastId;
    toasts.update(items => {
      const newItems = items.length >= 3 ? items.slice(1) : items;
      return [...newItems, { id, type, message }];
    });

    setTimeout(() => {
      toasts.update(items => items.filter(t => t.id !== id));
    }, 2500);
  }

  function removeToast(id: number) {
    toasts.update(items => items.filter(t => t.id !== id));
  }

  function getIconComponent(type: string) {
    switch (type) {
      case 'success':
        return CheckCircle;
      case 'error':
        return XCircle;
      case 'warning':
        return AlertTriangle;
      case 'info':
        return Info;
      default:
        return Info;
    }
  }

  function getStyle(type: string) {
    switch (type) {
      case 'success':
        return 'bg-[#67c23a]';
      case 'error':
        return 'bg-[#f56c6c]';
      case 'warning':
        return 'bg-[#e6a23c]';
      case 'info':
        return 'bg-[#909399]';
      default:
        return 'bg-[#909399]';
    }
  }

  // 将toast函数设置到window对象供全局访问
  if (typeof window !== 'undefined') {
    (window as any).toast = toast;
  }
</script>

<div
  class="fixed top-4 left-1/2 -translate-x-1/2 z-[9999] flex flex-col items-center gap-2 pointer-events-none"
>
  {#each $toasts as toastItem (toastItem.id)}
    <div
      class="{getStyle(
        toastItem.type
      )} text-white px-4 py-2.5 rounded-md shadow-md flex items-center gap-2 pointer-events-auto animate-[slideDown_0.3s_ease-out]"
    >
      <svelte:component this={getIconComponent(toastItem.type)} size={16} />
      <span class="text-sm">{toastItem.message}</span>
      <button
        onclick={() => removeToast(toastItem.id)}
        class="ml-2 text-white/70 hover:text-white transition-colors"
        aria-label={t('common.close')}
      >
        <X size={12} />
      </button>
    </div>
  {/each}
</div>
