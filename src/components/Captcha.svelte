<script lang="ts">
  /**
   * 增强型图形验证码组件
   */
  import { onMount, onDestroy } from 'svelte';
  import {
    obfuscate,
    validateObfuscated,
    generateSalt,
    MouseTracker,
    detectAutomation,
    getCanvasFingerprint,
    validateFingerprint,
    calculateSimilarity,
    checkIntegrity,
  } from '../lib/captchaUtils';
  import { t } from '$lib/locales';

  export let width = 120;
  export let height = 40;
  export let length = 4;
  export let onChange: ((code: string) => void) | undefined = undefined;

  let canvas: HTMLCanvasElement;
  let captchaHash = ''; // 存储多层加密后的哈希
  let salt = ''; // 随机盐值
  let timestamp = 0; // 生成时间戳
  let mouseTracker = new MouseTracker();
  let canvasFingerprint = '';
  let interactionStarted = false;

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

    // 代码完整性检查
    if (!checkIntegrity()) {
      console.warn('Security warning: Code integrity check failed');
    }

    // 清空画布
    ctx.fillStyle = randomColor(240, 255);
    ctx.fillRect(0, 0, width, height);

    // 生成验证码明文
    let captchaCode = '';
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
    for (let i = 0; i < 100; i++) {
      ctx.fillStyle = randomColor(150, 200);
      ctx.beginPath();
      // 随机噪点大小：0.5-1.5像素
      const radius = Math.random() * 1 + 0.5;
      ctx.arc(Math.random() * width, Math.random() * height, radius, 0, 2 * Math.PI);
      ctx.fill();
    }

    // 生成新的盐值和时间戳
    salt = generateSalt();
    timestamp = Date.now();

    // 使用多层加密存储
    captchaHash = obfuscate(captchaCode.toLowerCase(), salt);

    // 重置鼠标追踪
    mouseTracker.reset();
    interactionStarted = false;

    // 通知父组件验证码已更新（不传递明文）
    if (onChange) {
      onChange('');
    }
  }

  // 点击刷新
  function handleRefresh(e: MouseEvent) {
    mouseTracker.recordClick();
    draw();
  }

  // 鼠标移动追踪
  function handleMouseMove(e: MouseEvent) {
    if (!interactionStarted) {
      mouseTracker.start();
      interactionStarted = true;
    }
    const rect = canvas.getBoundingClientRect();
    mouseTracker.track(e.clientX - rect.left, e.clientY - rect.top);
  }

  // 全局鼠标移动追踪
  function handleGlobalMouseMove(e: MouseEvent) {
    if (!interactionStarted) {
      mouseTracker.start();
      interactionStarted = true;
    }
    // 记录页面任何位置的鼠标移动
    mouseTracker.track(e.clientX, e.clientY);
  }

  // 组件挂载时绘制和初始化
  onMount(() => {
    draw();
    canvasFingerprint = getCanvasFingerprint();

    // 监听整个页面的鼠标移动
    window.addEventListener('mousemove', handleGlobalMouseMove);

    return () => {
      window.removeEventListener('mousemove', handleGlobalMouseMove);
    };
  });

  // 组件卸载时清理
  onDestroy(() => {
    mouseTracker.reset();
  });

  // 验证函数（包含多重验证）
  export function validate(input: string): { valid: boolean; reason?: string } {
    // 1. 检测自动化工具（提高阈值，降低误判）
    const automationScore = detectAutomation();
    if (automationScore > 80) {
      // 从 50 提高到 80
      return { valid: false, reason: $t('captcha.automationDetected') };
    }

    // 2. 验证鼠标行为（降低要求）
    // 只有在有足够鼠标移动时才验证，否则跳过
    if (mouseTracker.validate && !mouseTracker.validate()) {
      // 给用户一个提示，但不强制要求
      console.warn($t('captcha.mouseBehaviorWarn'));
    }

    // 3. 验证时效性（5分钟）
    const now = Date.now();
    if (now - timestamp > 300000) {
      return { valid: false, reason: $t('captcha.expired') };
    }

    // 4. 验证 Canvas 指纹相似度
    const currentFingerprint = getCanvasFingerprint();
    if (canvasFingerprint && currentFingerprint) {
      const similarity = calculateSimilarity(canvasFingerprint, currentFingerprint);
      const isValid = validateFingerprint(canvasFingerprint, currentFingerprint, 70);

      // console.log(`浏览器指纹相似度: ${similarity}%`);

      if (!isValid) {
        // console.error(`浏览器指纹验证失败，相似度: ${similarity}%`);
        return { valid: false, reason: $t('captcha.envError') };
      }

      // console.log(`浏览器指纹验证通过，相似度: ${similarity}%`);
    }

    // 5. 使用多层解密验证
    const isValid = validateObfuscated(input, captchaHash, salt, timestamp);
    if (!isValid) {
      return { valid: false, reason: $t('captcha.incorrect') };
    }

    return { valid: true };
  }

  // 导出刷新方法
  export function refresh() {
    draw();
  }

  // 手动记录用户交互（供外部调用）
  export function recordInteraction() {
    if (!interactionStarted) {
      mouseTracker.start();
      interactionStarted = true;
    }
    mouseTracker.recordClick();
  }

  // 获取安全信息（用于调试）
  // export function getSecurityInfo() {
  //   const currentFingerprint = getCanvasFingerprint();
  //   const similarity = canvasFingerprint && currentFingerprint
  //     ? calculateSimilarity(canvasFingerprint, currentFingerprint)
  //     : 0;

  //   return {
  //     automationScore: detectAutomation(),
  //     hasMouseMovement: mouseTracker.validate(),
  //     age: Date.now() - timestamp,
  //     fingerprint: canvasFingerprint,
  //     currentFingerprint: currentFingerprint,
  //     fingerprintSimilarity: similarity,
  //     codeIntegrity: checkIntegrity()
  //   };
  // }
</script>

<canvas
  bind:this={canvas}
  {width}
  {height}
  onclick={handleRefresh}
  onmousemove={handleMouseMove}
  class="cursor-pointer rounded-lg border border-gray-200 dark:border-gray-700"
  title={$t('captcha.refreshTitle')}
></canvas>
