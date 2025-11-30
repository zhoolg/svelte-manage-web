<script lang="ts">
  /**
   * 图形验证码组件
   * 纯前端生成，支持点击刷新
   */
  import { onMount } from 'svelte';

  export let width = 120;
  export let height = 40;
  export let length = 4;
  export let onChange: ((code: string) => void) | undefined = undefined;

  let canvas: HTMLCanvasElement;
  let captchaCode = '';

  // 生成随机字符
  function randomChar(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
    return chars.charAt(Math.floor(Math.random() * chars.length));
  }

  // 生成随机颜色
  function randomColor(min: number, max: number): string {
    const r = Math.floor(Math.random() * (max - min) + min);
    const g = Math.floor(Math.random() * (max - min) + min);
    const b = Math.floor(Math.random() * (max - min) + min);
    return `rgb(${r},${g},${b})`;
  }

  // 绘制验证码
  function draw() {
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // 清空画布
    ctx.fillStyle = randomColor(240, 255);
    ctx.fillRect(0, 0, width, height);

    // 生成验证码
    captchaCode = '';
    for (let i = 0; i < length; i++) {
      const char = randomChar();
      captchaCode += char;

      // 绘制字符
      ctx.font = `bold ${Math.floor(height * 0.6)}px Arial`;
      ctx.fillStyle = randomColor(50, 160);
      ctx.textBaseline = 'middle';

      // 随机旋转和位置
      const x = (width / length) * i + width / length / 2 - 8;
      const y = height / 2 + Math.random() * 8 - 4;
      const rotate = (Math.random() - 0.5) * 0.4;

      ctx.save();
      ctx.translate(x, y);
      ctx.rotate(rotate);
      ctx.fillText(char, 0, 0);
      ctx.restore();
    }

    // 绘制干扰线
    for (let i = 0; i < 4; i++) {
      ctx.strokeStyle = randomColor(150, 200);
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(Math.random() * width, Math.random() * height);
      ctx.lineTo(Math.random() * width, Math.random() * height);
      ctx.stroke();
    }

    // 绘制干扰点
    for (let i = 0; i < 30; i++) {
      ctx.fillStyle = randomColor(150, 200);
      ctx.beginPath();
      ctx.arc(Math.random() * width, Math.random() * height, 1, 0, 2 * Math.PI);
      ctx.fill();
    }

    // 调用 onChange 回调
    if (onChange) {
      onChange(captchaCode);
    }
  }

  // 点击刷新
  function handleRefresh() {
    draw();
  }

  // 组件挂载时绘制
  onMount(() => {
    draw();
  });

  // 导出验证函数供外部使用
  export function validate(input: string): boolean {
    return input.toLowerCase() === captchaCode.toLowerCase();
  }
</script>

<canvas
  bind:this={canvas}
  {width}
  {height}
  onclick={handleRefresh}
  class="cursor-pointer rounded-lg border border-gray-200 dark:border-gray-700 hover:border-[#409eff] transition-colors"
  title="点击刷新验证码"
></canvas>
