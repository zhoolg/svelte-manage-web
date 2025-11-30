<script lang="ts" context="module">
  export interface ConfirmOptions {
    title?: string;
    content: string;
    type?: 'info' | 'warning' | 'danger';
    confirmText?: string;
    cancelText?: string;
  }
</script>

<script lang="ts">
  /**
   * 确认对话框组件 - 使用 Bits UI Dialog
   * ============================================================
   *
   * 重构说明：
   * - 从自定义 Modal 迁移到 Bits UI Dialog
   * - 保持原有 API 和功能不变
   * - 提升可访问性（ARIA、键盘导航）
   */
  import { Dialog } from 'bits-ui';
  import { writable } from 'svelte/store';
  import { getTranslator } from '../lib/locales';

  interface ConfirmState extends ConfirmOptions {
    visible: boolean;
    resolve?: (value: boolean) => void;
  }

  const confirmState = writable<ConfirmState>({ visible: false, content: '' });

  // 导出confirm函数
  export const confirm = (options: ConfirmOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      confirmState.set({ ...options, visible: true, resolve });
    });
  };

  function handleClose(result: boolean) {
    confirmState.update(state => {
      state.resolve?.(result);
      return { ...state, visible: false };
    });
  }

  function getTypeConfig(type?: string) {
    switch (type) {
      case 'warning':
        return { icon: 'pi-exclamation-triangle', color: '#e6a23c' };
      case 'danger':
        return { icon: 'pi-times-circle', color: '#f56c6c' };
      default:
        return { icon: 'pi-info-circle', color: '#409eff' };
    }
  }

  // 设置到window对象供全局访问
  if (typeof window !== 'undefined') {
    (window as any).confirm = confirm;
  }

  // 计算当前类型配置
  $: typeConfig = getTypeConfig($confirmState.type);

  // 获取翻译函数
  const t = getTranslator();
</script>

<Dialog.Root open={$confirmState.visible} onOpenChange={(open) => !open && handleClose(false)}>
  <Dialog.Portal>
    <!-- 背景遮罩 -->
    <Dialog.Overlay
      class="fixed inset-0 z-[2000] bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0"
    />

    <!-- 对话框内容 -->
    <Dialog.Content
      class="fixed left-1/2 top-1/2 z-[2001] w-[420px] max-w-[calc(100vw-32px)] -translate-x-1/2 -translate-y-1/2 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-xl data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%]"
    >
      <!-- 内容区域 -->
      <div class="px-5 py-6">
        <div class="flex items-start gap-3">
          <i
            class="pi {typeConfig.icon} text-2xl mt-0.5"
            style="color: {typeConfig.color}"
          ></i>
          <div class="flex-1">
            {#if $confirmState.title}
              <Dialog.Title class="text-base font-medium text-gray-900 dark:text-white mb-2">
                {$confirmState.title}
              </Dialog.Title>
            {/if}
            <Dialog.Description class="text-sm text-gray-600 dark:text-gray-300">
              {$confirmState.content}
            </Dialog.Description>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="flex items-center justify-end gap-3 px-5 py-4 border-t border-gray-100 dark:border-gray-800">
        <button
          type="button"
          onclick={() => handleClose(false)}
          class="h-8 px-4 text-sm border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 rounded hover:text-[#409eff] hover:border-[#409eff] transition-colors"
        >
          {$confirmState.cancelText || t('common.cancel')}
        </button>
        <button
          type="button"
          onclick={() => handleClose(true)}
          class="h-8 px-4 text-sm text-white rounded transition-colors"
          style="background-color: {$confirmState.type === 'danger' ? '#f56c6c' : '#409eff'}"
        >
          {$confirmState.confirmText || t('common.confirm')}
        </button>
      </div>
    </Dialog.Content>
  </Dialog.Portal>
</Dialog.Root>
