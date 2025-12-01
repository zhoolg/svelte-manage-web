/**
 * 增强型验证码安全工具函数
 */


// 检测开发者工具是否打开
export function detectDevTools(): boolean {
  const threshold = 160;
  const widthThreshold = window.outerWidth - window.innerWidth > threshold;
  const heightThreshold = window.outerHeight - window.innerHeight > threshold;

  // 检测 Firebug
  const firebug = (window as any).Firebug && (window as any).Firebug.chrome && (window as any).Firebug.chrome.isInitialized;

  // 检测控制台
  let devtoolsOpen = false;
  const element = new Image();
  Object.defineProperty(element, 'id', {
    get: function() {
      devtoolsOpen = true;
      return 'detect';
    }
  });

  return widthThreshold || heightThreshold || firebug || devtoolsOpen;
}

// 时间检测（防止断点调试）
export function detectDebuggerByTiming(): boolean {
  const start = performance.now();
  // 空操作
  debugger; // 如果有调试器，这里会暂停，导致时间差异大
  const end = performance.now();

  // 如果执行时间超过100ms，可能被调试
  return (end - start) > 100;
}

// 检测虚拟机环境
export function detectVM(): number {
  let score = 0;

  // 检测硬件并发数（虚拟机通常较少）
  if (navigator.hardwareConcurrency && navigator.hardwareConcurrency < 2) {
    score += 20;
  }

  // 检测设备内存（虚拟机可能较小）
  const deviceMemory = (navigator as any).deviceMemory;
  if (deviceMemory && deviceMemory < 4) {
    score += 15;
  }

  // 检测屏幕分辨率异常
  if (screen.width < 1024 || screen.height < 768) {
    score += 10;
  }

  return score;
}

// ==================== 多重哈希算法 ====================

// 自定义哈希算法 1（类似 DJB2）
function _0x4a2b(str: string, seed: number = 5381): number {
  let _0x8c = seed;
  for (let _0x1f = 0; _0x1f < str.length; _0x1f++) {
    _0x8c = ((_0x8c << 5) + _0x8c) + str.charCodeAt(_0x1f);
  }
  return _0x8c >>> 0;
}

// 自定义哈希算法 2（类似 SDBM）
function _0x7e3d(str: string): number {
  let _0x9f = 0;
  for (let _0x2c = 0; _0x2c < str.length; _0x2c++) {
    const _0x4e = str.charCodeAt(_0x2c);
    _0x9f = _0x4e + (_0x9f << 6) + (_0x9f << 16) - _0x9f;
  }
  return _0x9f >>> 0;
}

// 自定义哈希算法 3（FNV-1a 变体）
function _0x2f8a(str: string): number {
  let _0x6d = 2166136261;
  for (let _0x3b = 0; _0x3b < str.length; _0x3b++) {
    _0x6d ^= str.charCodeAt(_0x3b);
    _0x6d += (_0x6d << 1) + (_0x6d << 4) + (_0x6d << 7) + (_0x6d << 8) + (_0x6d << 24);
  }
  return _0x6d >>> 0;
}

// ROT13 变体加密
function _0x5c19(str: string, shift: number): string {
  return str.split('').map(char => {
    const code = char.charCodeAt(0);
    if (code >= 65 && code <= 90) {
      return String.fromCharCode(((code - 65 + shift) % 26) + 65);
    } else if (code >= 97 && code <= 122) {
      return String.fromCharCode(((code - 97 + shift) % 26) + 97);
    } else if (code >= 48 && code <= 57) {
      return String.fromCharCode(((code - 48 + shift) % 10) + 48);
    }
    return char;
  }).join('');
}

// XOR 加密
function _0x9b2e(str: string, key: number): string {
  return str.split('').map((char, i) => {
    return String.fromCharCode(char.charCodeAt(0) ^ ((key + i) % 256));
  }).join('');
}

// Base64 变体编码
function _0x3d7f(str: string): string {
  const _0x8a = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  let result = '';
  let i = 0;

  while (i < str.length) {
    const a = str.charCodeAt(i++);
    const b = i < str.length ? str.charCodeAt(i++) : 0;
    const c = i < str.length ? str.charCodeAt(i++) : 0;

    const bitmap = (a << 16) | (b << 8) | c;

    result += _0x8a.charAt((bitmap >> 18) & 63);
    result += _0x8a.charAt((bitmap >> 12) & 63);
    result += i - 1 < str.length ? _0x8a.charAt((bitmap >> 6) & 63) : '=';
    result += i < str.length ? _0x8a.charAt(bitmap & 63) : '=';
  }

  return result;
}

// ==================== 动态多层加密 ====================

// 根据时间戳选择加密算法组合
function _0xAlgoSelector(timestamp: number): number[] {
  const seed = timestamp % 1000;
  const combo = [
    seed % 3,           // 第一层
    (seed * 7) % 4,     // 第二层
    (seed * 13) % 3,    // 第三层
    (seed * 17) % 2     // 第四层
  ];
  return combo;
}

// 多层混淆加密
export function obfuscate(text: string, salt: string): string {
  const timestamp = Date.now();
  const combo = _0xAlgoSelector(timestamp);

  // 第一层：字符串预处理
  let layer1 = text + salt + timestamp.toString(36);

  // 第二层：ROT 变体
  const rotShift = (timestamp % 25) + 1;
  let layer2 = _0x5c19(layer1, rotShift);

  // 第三层：XOR 加密
  const xorKey = _0x4a2b(salt) % 256;
  let layer3 = _0x9b2e(layer2, xorKey);

  // 第四层：Base64 变体
  let layer4 = _0x3d7f(layer3);

  // 第五层：多重哈希组合
  const hash1 = _0x4a2b(layer4, combo[0]).toString(36);
  const hash2 = _0x7e3d(layer4 + hash1).toString(36);
  const hash3 = _0x2f8a(layer4 + hash2).toString(36);

  // 最终混淆结果
  const final = combo.join('') + ':' + hash1 + hash2.slice(0, 8) + hash3.slice(0, 8);

  return final;
}

// 验证混淆值（需要相同的盐和时间戳范围）
export function validateObfuscated(input: string, hash: string, salt: string, timestamp: number): boolean {
  // 允许时间误差（±2秒）
  for (let offset = -2000; offset <= 2000; offset += 100) {
    const testTime = timestamp + offset;
    const combo = _0xAlgoSelector(testTime);

    let layer1 = input.toLowerCase() + salt + testTime.toString(36);
    const rotShift = (testTime % 25) + 1;
    let layer2 = _0x5c19(layer1, rotShift);
    const xorKey = _0x4a2b(salt) % 256;
    let layer3 = _0x9b2e(layer2, xorKey);
    let layer4 = _0x3d7f(layer3);

    const hash1 = _0x4a2b(layer4, combo[0]).toString(36);
    const hash2 = _0x7e3d(layer4 + hash1).toString(36);
    const hash3 = _0x2f8a(layer4 + hash2).toString(36);

    const testHash = combo.join('') + ':' + hash1 + hash2.slice(0, 8) + hash3.slice(0, 8);

    if (testHash === hash) {
      return true;
    }
  }

  return false;
}

// ==================== 环境检测 ====================

// 检测自动化工具特征
export function detectAutomation(): number {
  let score = 0;

  // 检测 webdriver
  if (navigator.webdriver) score += 30;

  // 检测常见自动化工具属性
  if ((window as any).__nightmare) score += 30;
  if ((window as any).__phantomjs) score += 30;
  if ((window as any).callPhantom) score += 30;
  if ((window as any)._phantom) score += 30;
  if ((window as any).spawn) score += 20;
  if ((window as any).emit) score += 20;
  if ((window as any).Buffer) score += 15;

  // 检测 Chrome headless
  if (/HeadlessChrome/.test(navigator.userAgent)) score += 30;
  if (/PhantomJS/.test(navigator.userAgent)) score += 30;

  // 检测插件数量异常
  if (navigator.plugins.length === 0) score += 15;
  if (navigator.plugins.length > 20) score += 10;

  // 检测语言
  if (!navigator.language) score += 15;
  if (!(navigator.languages && navigator.languages.length)) score += 10;

  // 检测平台
  if (!navigator.platform) score += 15;

  // 检测权限
  try {
    if ((navigator as any).permissions === undefined) score += 10;
  } catch (e) {
    score += 10;
  }

  // 检测开发者工具
  if (detectDevTools()) score += 25;

  // 检测虚拟机
  score += detectVM();

  return score;
}

// Canvas 指纹检测
export function getCanvasFingerprint(): string {
  try {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    if (!ctx) return '';

    canvas.width = 220;
    canvas.height = 60;

    // 绘制复杂图形以增加指纹唯一性
    ctx.textBaseline = 'top';
    ctx.font = '14px "Arial"';
    ctx.textBaseline = 'alphabetic';
    ctx.fillStyle = '#f60';
    ctx.fillRect(125, 1, 62, 20);

    ctx.fillStyle = '#069';
    ctx.font = '11pt no-real-font-123';

    // 使用多语言字符增加复杂度
    const text = 'Cwm fjordbank glyphs vext quiz, 😃';
    ctx.fillText(text, 2, 15);

    ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
    ctx.font = '18pt Arial';
    ctx.fillText(text, 4, 45);

    // 绘制渐变
    const gradient = ctx.createLinearGradient(0, 0, 220, 60);
    gradient.addColorStop(0, 'red');
    gradient.addColorStop(0.5, 'green');
    gradient.addColorStop(1, 'blue');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 220, 60);

    const dataURL = canvas.toDataURL();

    // 使用多重哈希
    const hash1 = _0x4a2b(dataURL);
    const hash2 = _0x7e3d(dataURL);
    const hash3 = _0x2f8a(dataURL);

    return (hash1 ^ hash2 ^ hash3).toString(36);
  } catch (e) {
    return '';
  }
}

// 计算两个字符串的相似度（0-100，100表示完全相同）
export function calculateSimilarity(str1: string, str2: string): number {
  if (str1 === str2) return 100;
  if (!str1 || !str2) return 0;
  if (str1.length === 0 && str2.length === 0) return 100;
  if (str1.length === 0 || str2.length === 0) return 0;

  // 使用 Levenshtein 距离算法计算编辑距离
  const len1 = str1.length;
  const len2 = str2.length;
  const matrix: number[][] = [];

  for (let i = 0; i <= len1; i++) {
    matrix[i] = [i];
  }

  for (let j = 0; j <= len2; j++) {
    matrix[0][j] = j;
  }

  for (let i = 1; i <= len1; i++) {
    for (let j = 1; j <= len2; j++) {
      const cost = str1[i - 1] === str2[j - 1] ? 0 : 1;
      matrix[i][j] = Math.min(
        matrix[i - 1][j] + 1,      // 删除
        matrix[i][j - 1] + 1,      // 插入
        matrix[i - 1][j - 1] + cost // 替换
      );
    }
  }

  const distance = matrix[len1][len2];
  const maxLength = Math.max(len1, len2);

  // 转换为相似度百分比
  const similarity = ((maxLength - distance) / maxLength) * 100;
  return Math.round(similarity);
}

// 验证Canvas指纹相似度
export function validateFingerprint(fingerprint1: string, fingerprint2: string, threshold: number = 70): boolean {
  if (!fingerprint1 || !fingerprint2) return true; // 如果没有指纹，允许通过

  const similarity = calculateSimilarity(fingerprint1, fingerprint2);
  return similarity >= threshold;
}

// ==================== 鼠标轨迹追踪 ====================

export class MouseTracker {
  private movements: Array<{ x: number; y: number; time: number }> = [];
  private startTime: number = 0;
  private clicks: number = 0;

  start() {
    this.movements = [];
    this.clicks = 0;
    this.startTime = Date.now();
  }

  track(x: number, y: number) {
    const now = Date.now();
    this.movements.push({
      x: Math.floor(x),
      y: Math.floor(y),
      time: now - this.startTime
    });

    // 限制存储数量，防止内存占用
    if (this.movements.length > 200) {
      this.movements.shift();
    }
  }

  recordClick() {
    this.clicks++;
  }

  // 高级人类行为验证
  validate(): boolean {
    // 降低要求：至少有一些鼠标移动即可
    if (this.movements.length < 3) return false;

    // 点击不是必须的（用户可能直接输入验证码）
    // if (this.clicks < 1) return false;

    // 如果鼠标移动较少，直接通过（用户可能快速操作）
    if (this.movements.length < 5) return true;

    // 检查移动速度的变化（人类不会匀速移动）
    const speeds: number[] = [];
    for (let i = 1; i < this.movements.length; i++) {
      const dx = this.movements[i].x - this.movements[i - 1].x;
      const dy = this.movements[i].y - this.movements[i - 1].y;
      const dt = this.movements[i].time - this.movements[i - 1].time;
      if (dt > 0) {
        const speed = Math.sqrt(dx * dx + dy * dy) / dt;
        speeds.push(speed);
      }
    }

    // 降低速度方差要求
    const speedVariance = this.calculateVariance(speeds);
    if (speedVariance < 0.0001) return false; // 从 0.001 降低到 0.0001

    // 检查 X 和 Y 坐标的分布
    const xValues = this.movements.map(m => m.x);
    const yValues = this.movements.map(m => m.y);

    const xVariance = this.calculateVariance(xValues);
    const yVariance = this.calculateVariance(yValues);

    // 降低移动随机性要求
    if (xVariance < 10 && yVariance < 10) return false; // 从 50 降低到 10

    // 检查方向变化（人类会改变方向）
    let directionChanges = 0;
    for (let i = 2; i < this.movements.length; i++) {
      const dx1 = this.movements[i - 1].x - this.movements[i - 2].x;
      const dx2 = this.movements[i].x - this.movements[i - 1].x;
      const dy1 = this.movements[i - 1].y - this.movements[i - 2].y;
      const dy2 = this.movements[i].y - this.movements[i - 1].y;

      if ((dx1 * dx2 < 0) || (dy1 * dy2 < 0)) {
        directionChanges++;
      }
    }

    // 降低方向变化要求（不再强制要求）
    // if (directionChanges < 2) return false;

    return true;
  }

  private calculateVariance(arr: number[]): number {
    if (arr.length === 0) return 0;
    const mean = arr.reduce((a, b) => a + b, 0) / arr.length;
    const variance = arr.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / arr.length;
    return variance;
  }

  reset() {
    this.movements = [];
    this.clicks = 0;
    this.startTime = 0;
  }
}

// ==================== 工具函数 ====================

// 生成随机盐值（使用更复杂的方法）
export function generateSalt(): string {
  const timestamp = Date.now();
  const random1 = Math.random().toString(36).substring(2, 15);
  const random2 = Math.random().toString(36).substring(2, 15);
  const hash = _0x4a2b(random1 + timestamp + random2);

  return random1 + hash.toString(36) + random2;
}

// 检查验证码时效性
export function isExpired(timestamp: number, maxAge: number = 300000): boolean {
  return Date.now() - timestamp > maxAge;
}

// 代码完整性检查（检测是否被篡改）
export function checkIntegrity(): boolean {
  try {
    // 检查关键函数是否被修改
    const funcString = obfuscate.toString();
    const expectedLength = funcString.length;

    // 简单的长度检查
    if (expectedLength < 100) return false;

    // 检查是否包含关键代码片段
    if (!funcString.includes('_0xAlgoSelector')) return false;
    if (!funcString.includes('layer')) return false;

    return true;
  } catch (e) {
    return false;
  }
}
