<script lang="ts">
  /**
   * 图片上传组件
   * 支持单图和多图上传，带预览功能
   */
  import { Button } from 'bits-ui';
  import { uploadImage } from '../api/upload';
  import { toast } from '../utils/toast';
  import { t } from '$lib/locales';

  // Props
  export let value: string = ''; // 单图模式：图片 URL
  export let multiple: boolean = false; // 是否支持多图上传
  export let maxSize: number = 5; // 最大文件大小（MB）
  export let accept: string = 'image/*'; // 接受的文件类型
  export let disabled: boolean = false;

  let uploading = false;
  let fileInput: HTMLInputElement;

  /**
   * 处理文件选择
   */
  async function handleFileChange(event: Event) {
    const target = event.target as HTMLInputElement;
    const files = target.files;

    if (!files || files.length === 0) return;

    // 验证文件大小
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (file.size > maxSize * 1024 * 1024) {
        toast.error($t('imageUpload.fileTooLarge', { name: file.name, maxSize }));
        target.value = '';
        return;
      }
    }

    uploading = true;

    try {
      if (multiple) {
        // 多图上传（暂不实现，需要调整 value 类型）
        toast.warning($t('imageUpload.multiNotSupported'));
      } else {
        // 单图上传
        const file = files[0];
        const url = await uploadImage(file);
        value = url;
        toast.success($t('imageUpload.success'));
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : $t('imageUpload.failed'));
    } finally {
      uploading = false;
      target.value = '';
    }
  }

  /**
   * 触发文件选择
   */
  function triggerUpload() {
    fileInput?.click();
  }

  /**
   * 删除图片
   */
  function removeImage() {
    value = '';
  }
</script>

<div class="space-y-2">
  <!-- 隐藏的文件输入 -->
  <input
    bind:this={fileInput}
    type="file"
    {accept}
    {multiple}
    onchange={handleFileChange}
    class="hidden"
    {disabled}
  />

  <!-- 上传按钮 -->
  {#if !value}
    <Button.Root
      onclick={triggerUpload}
      disabled={disabled || uploading}
      class="h-9 px-4 bg-[#409eff] hover:bg-[#66b1ff] text-white text-sm rounded transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
    >
      {#if uploading}
        <i class="pi pi-spin pi-spinner"></i>
        {$t('imageUpload.loading')}
      {:else}
        <i class="pi pi-upload"></i>
        {$t('imageUpload.button')}
      {/if}
    </Button.Root>
  {/if}

  <!-- 图片预览 -->
  {#if value}
    <div class="relative inline-block group">
      <div class="w-32 h-32 border border-gray-200 dark:border-gray-700 rounded overflow-hidden bg-gray-50 dark:bg-gray-800">
        <img
          src={value}
          alt={$t('imageUpload.previewAlt')}
          class="w-full h-full object-cover"
          onerror={(e) => {
            const placeholderText = encodeURIComponent($t('imageUpload.previewFailed'));
            e.currentTarget.src = `data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="128" height="128"%3E%3Crect fill="%23ddd" width="128" height="128"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="sans-serif" font-size="14"%3E${placeholderText}%3C/text%3E%3C/svg%3E`;
          }}
        />
      </div>
      <!-- 悬浮操作按钮 -->
      {#if !disabled}
        <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
          <Button.Root
            onclick={triggerUpload}
            disabled={uploading}
            class="h-8 px-3 bg-white hover:bg-gray-100 text-gray-900 text-xs rounded transition-colors"
          >
            <i class="pi pi-refresh"></i>
          </Button.Root>
          <Button.Root
            onclick={removeImage}
            class="h-8 px-3 bg-red-500 hover:bg-red-600 text-white text-xs rounded transition-colors"
          >
            <i class="pi pi-trash"></i>
          </Button.Root>
        </div>
      {/if}
    </div>
  {/if}

  <!-- 提示信息 -->
  <p class="text-xs text-gray-500">
    {$t('imageUpload.hint', { maxSize })}
  </p>
</div>
