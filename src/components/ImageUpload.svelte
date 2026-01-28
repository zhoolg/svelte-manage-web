<script lang="ts">
  /**
   * 图片上传组件
   * 支持单图和多图上传，带预览功能
   */
  import { Button } from 'bits-ui';
  import { PlayCircle, Play, ArrowLeft, ArrowRight, RefreshCw, Trash2, Plus, Loader2 } from 'lucide-svelte';
  import { uploadImage } from '../api/upload';
  import { toast } from '../utils/toast';
  import { getImageUrl } from '../utils/image';
  import { t } from '$lib/locales';

  // Props
  export let value: string = ''; // 单图模式：图片 URL；多图模式：逗号分隔的 URL 字符串
  export let multiple: boolean = false; // 是否支持多图上传
  export let maxSize: number = 5; // 最大文件大小（MB）
  export let accept: string = 'image/*'; // 接受的文件类型
  export let disabled: boolean = false;
  export let limit: number = 0; // 最大图片数量限制（0 表示不限制）

  let uploading = false;
  let fileInput: HTMLInputElement;

  // 解析图片列表（多图模式下 value 为逗号分隔字符串）
  $: imageList = multiple && value ? value.split(',').filter(Boolean) : value ? [value] : [];

  /**
   * 处理文件选择
   */
  async function handleFileChange(event: Event) {
    const target = event.target as HTMLInputElement;
    const files = target.files;

    if (!files || files.length === 0) return;

    // 验证数量限制
    if (multiple && limit > 0 && imageList.length + files.length > limit) {
      toast.warning(`最多只能上传 ${limit} 张图片`);
      target.value = '';
      return;
    }

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
        // 多图上传
        const promises = Array.from(files).map(f => uploadImage(f));
        const urls = await Promise.all(promises);
        // 追加新图片
        const newList = [...imageList, ...urls];
        value = newList.join(',');
        toast.success($t('imageUpload.success'));
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
  function removeImage(index: number = 0) {
    if (multiple) {
      const newList = [...imageList];
      newList.splice(index, 1);
      value = newList.join(',');
    } else {
      value = '';
    }
  }

  /**
   * 移动图片（排序）
   */
  function moveImage(index: number, direction: -1 | 1) {
    if (!multiple) return;
    const newList = [...imageList];
    const targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= newList.length) return;

    [newList[index], newList[targetIndex]] = [newList[targetIndex], newList[index]];
    value = newList.join(',');
  }

  /**
   * 判断是否为视频文件
   */
  function isVideo(url: string): boolean {
    const videoExtensions = ['.mp4', '.webm', '.ogg', '.mov', '.avi', '.wmv', '.flv', '.mkv'];
    const lowerUrl = url.toLowerCase();
    return videoExtensions.some(ext => lowerUrl.endsWith(ext));
  }

  /**
   * 预览视频（在新窗口打开）
   */
  function previewVideo(url: string) {
    const fullUrl = getImageUrl(url);
    window.open(fullUrl, '_blank');
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

  <div class="flex flex-wrap gap-4">
    <!-- 图片预览列表 -->
    {#each imageList as url, index (url + index)}
      <div class="relative inline-block group">
        <div
          class="w-32 h-32 border border-gray-200 dark:border-gray-700 rounded overflow-hidden bg-gray-50 dark:bg-gray-800"
        >
          {#if isVideo(url)}
            <!-- 视频预览（无控制条，避免与操作按钮重叠） -->
            <video
              src={getImageUrl(url)}
              class="w-full h-full object-cover"
              preload="metadata"
              muted
            >
              <track kind="captions" />
            </video>
            <!-- 视频标识 -->
            <div
              class="absolute top-2 left-2 bg-black/70 text-white px-2 py-1 rounded text-xs flex items-center gap-1 pointer-events-none"
            >
              <PlayCircle size={12} />
              <span>视频</span>
            </div>
          {:else}
            <!-- 图片预览 -->
            <img
              src={getImageUrl(url)}
              alt={$t('imageUpload.previewAlt')}
              class="w-full h-full object-cover"
              onerror={e => {
                const placeholderText = encodeURIComponent($t('imageUpload.previewFailed'));
                (e.currentTarget as HTMLImageElement).src =
                  `data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="128" height="128"%3E%3Crect fill="%23ddd" width="128" height="128"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" font-family="sans-serif" font-size="14"%3E${placeholderText}%3C/text%3E%3C/svg%3E`;
              }}
            />
          {/if}
        </div>

        <!-- 悬浮操作按钮 -->
        {#if !disabled}
          <div
            class="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center gap-2 pointer-events-none"
          >
            <div class="pointer-events-auto flex flex-col items-center gap-2">
              {#if isVideo(url)}
                <!-- 视频预览按钮 -->
                <Button.Root
                  onclick={() => previewVideo(url)}
                  class="h-8 px-3 bg-blue-500 hover:bg-blue-600 text-white text-xs rounded transition-colors flex items-center gap-1"
                  title="预览视频"
                >
                  <Play size={12} />
                  <span>预览</span>
                </Button.Root>
              {/if}
              {#if multiple}
                <!-- 多图排序 -->
                <div class="flex gap-2">
                  <Button.Root
                    onclick={() => moveImage(index, -1)}
                    disabled={index === 0}
                    class="h-8 w-8 bg-white hover:bg-gray-100 text-gray-900 text-xs rounded transition-colors flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
                    title="左移"
                  >
                    <ArrowLeft size={14} />
                  </Button.Root>
                  <Button.Root
                    onclick={() => moveImage(index, 1)}
                    disabled={index === imageList.length - 1}
                    class="h-8 w-8 bg-white hover:bg-gray-100 text-gray-900 text-xs rounded transition-colors flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
                    title="右移"
                  >
                    <ArrowRight size={14} />
                  </Button.Root>
                </div>
              {:else}
                <!-- 单图替换 -->
                <Button.Root
                  onclick={triggerUpload}
                  disabled={uploading}
                  class="h-8 px-3 bg-white hover:bg-gray-100 text-gray-900 text-xs rounded transition-colors"
                  title={$t('common.edit')}
                >
                  <RefreshCw size={14} />
                </Button.Root>
              {/if}
              <!-- 删除按钮 -->
              <Button.Root
                onclick={() => removeImage(index)}
                class="h-8 px-3 bg-red-500 hover:bg-red-600 text-white text-xs rounded transition-colors"
                title={$t('common.delete')}
              >
                <Trash2 size={14} />
              </Button.Root>
            </div>
          </div>
        {/if}
      </div>
    {/each}

    <!-- 上传按钮（始终显示直到达到限制） -->
    {#if !disabled && ((!multiple && imageList.length === 0) || (multiple && (limit === 0 || imageList.length < limit)))}
      <button
        type="button"
        class="w-32 h-32 border border-dashed border-gray-300 dark:border-gray-600 rounded flex flex-col items-center justify-center gap-2 text-gray-400 hover:border-[#409eff] hover:text-[#409eff] cursor-pointer transition-colors bg-gray-50 dark:bg-gray-800/50"
        onclick={triggerUpload}
        aria-label={$t('imageUpload.button')}
      >
        {#if uploading}
          <Loader2 size={24} class="animate-spin" />
          <span class="text-xs">{$t('imageUpload.loading')}</span>
        {:else}
          <Plus size={24} />
          <span class="text-xs">{$t('imageUpload.button')}</span>
        {/if}
      </button>
    {/if}
  </div>
</div>
