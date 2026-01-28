/**
 * 浏览器兼容性检测
 * 检测浏览器版本并在必要时显示升级提示
 */

import { t } from '$lib/locales';

export interface BrowserInfo {
  name: string;
  version: number;
  isSupported: boolean;
  isOldVersion: boolean;
}

/**
 * 检测浏览器信息
 */
export function detectBrowser(): BrowserInfo {
  const ua = navigator.userAgent;
  let name = 'Unknown';
  let version = 0;

  // 检测 Firefox
  if (ua.indexOf('Firefox') > -1) {
    name = 'Firefox';
    const match = ua.match(/Firefox\/(\d+)/);
    if (match) {
      version = parseInt(match[1], 10);
    }
  }
  // 检测 Chrome
  else if (ua.indexOf('Chrome') > -1 && ua.indexOf('Edg') === -1) {
    name = 'Chrome';
    const match = ua.match(/Chrome\/(\d+)/);
    if (match) {
      version = parseInt(match[1], 10);
    }
  }
  // 检测 Edge
  else if (ua.indexOf('Edg') > -1) {
    name = 'Edge';
    const match = ua.match(/Edg\/(\d+)/);
    if (match) {
      version = parseInt(match[1], 10);
    }
  }
  // 检测 Safari
  else if (ua.indexOf('Safari') > -1 && ua.indexOf('Chrome') === -1) {
    name = 'Safari';
    const match = ua.match(/Version\/(\d+)/);
    if (match) {
      version = parseInt(match[1], 10);
    }
  }
  // 检测 IE
  else if (ua.indexOf('MSIE') > -1 || ua.indexOf('Trident') > -1) {
    name = 'IE';
    const match = ua.match(/(?:MSIE |rv:)(\d+)/);
    if (match) {
      version = parseInt(match[1], 10);
    }
  }

  // 判断是否支持
  const isSupported = checkBrowserSupport(name, version);
  const isOldVersion = checkIsOldVersion(name, version);

  return {
    name,
    version,
    isSupported,
    isOldVersion,
  };
}

/**
 * 检查浏览器是否支持
 */
function checkBrowserSupport(name: string, version: number): boolean {
  const minVersions: Record<string, number> = {
    Firefox: 110, // 最低支持 Firefox 90
    Chrome: 100, // 最低支持 Chrome 90
    Edge: 100, // 最低支持 Edge 90
    Safari: 20, // 最低支持 Safari 14
    IE: 0, // 不支持 IE
  };

  const minVersion = minVersions[name];
  if (minVersion === undefined) return true; // 未知浏览器假设支持
  if (minVersion === 0) return false; // IE 不支持

  return version >= minVersion;
}

/**
 * 检查是否为老版本浏览器(建议升级但仍可使用)
 */
function checkIsOldVersion(name: string, version: number): boolean {
  const recommendedVersions: Record<string, number> = {
    Firefox: 120, // 建议使用 Firefox 120+
    Chrome: 120, // 建议使用 Chrome 120+
    Edge: 120, // 建议使用 Edge 120+
    Safari: 20, // 建议使用 Safari 20+
  };

  const recommendedVersion = recommendedVersions[name];
  if (recommendedVersion === undefined) return false;

  return version < recommendedVersion;
}

/**
 * 显示浏览器升级提示
 */
export function showBrowserWarning(browserInfo: BrowserInfo): void {
  if (!browserInfo.isSupported) {
    showUnsupportedWarning(browserInfo);
  } else if (browserInfo.isOldVersion) {
    showOldVersionWarning(browserInfo);
  }
}

/**
 * 显示不支持的浏览器警告(模态框,阻止使用)
 */
function showUnsupportedWarning(browserInfo: BrowserInfo): void {
  // 获取当前语言的翻译函数
  let currentT: any;
  const unsubscribe = t.subscribe(value => {
    currentT = value;
  });
  unsubscribe();

  const modal = `
    <div style="
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(102, 177, 255, 0.1) 100%);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      z-index: 999999;
      display: flex;
      align-items: center;
      justify-content: center;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      animation: fadeIn 0.3s ease-out;
    ">
      <div style="
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(20px);
        -webkit-backdrop-filter: blur(20px);
        border-radius: 24px;
        padding: 48px;
        max-width: 520px;
        width: 90%;
        text-align: center;
        box-shadow: 0 20px 60px rgba(64, 158, 255, 0.2), 0 0 0 1px rgba(255, 255, 255, 0.1);
        animation: slideUp 0.4s ease-out;
      ">
        <!-- 图标 -->
        <div style="
          width: 80px;
          height: 80px;
          background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
          border-radius: 50%;
          margin: 0 auto 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 8px 24px rgba(64, 158, 255, 0.25);
          position: relative;
        ">
          <div style="
            position: absolute;
            inset: -6px;
            background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
            border-radius: 50%;
            opacity: 0.15;
            filter: blur(12px);
          "></div>
          <span style="
            position: relative;
            z-index: 1;
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
          "><svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"/><path d="M12 9v4"/><path d="M12 17h.01"/></svg></span>
        </div>

        <!-- 标题 -->
        <h2 style="
          font-size: 28px;
          font-weight: 700;
          background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          margin: 0 0 16px 0;
          letter-spacing: -0.5px;
        ">
          ${currentT('browserCompatibility.unsupported.title')}
        </h2>

        <!-- 描述 -->
        <p style="
          font-size: 16px;
          color: #4a5568;
          line-height: 1.6;
          margin: 0 0 24px 0;
        ">
          ${currentT('browserCompatibility.unsupported.description', { browser: browserInfo.name, version: browserInfo.version })}
        </p>

        <!-- 分割线 -->
        <div style="
          height: 1px;
          background: linear-gradient(90deg, transparent, rgba(64, 158, 255, 0.2), transparent);
          margin: 24px 0;
        "></div>

        <!-- 支持的浏览器 -->
        <p style="
          font-size: 14px;
          color: #718096;
          margin: 0 0 24px 0;
        ">
          ${currentT('browserCompatibility.unsupported.upgradeHint')}
        </p>

        <div style="
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 12px;
          margin-bottom: 32px;
        ">
          <div style="
            padding: 12px;
            background: rgba(64, 158, 255, 0.05);
            border-radius: 12px;
            font-size: 14px;
            font-weight: 600;
            color: #409eff;
          ">Chrome 120+</div>
          <div style="
            padding: 12px;
            background: rgba(64, 158, 255, 0.05);
            border-radius: 12px;
            font-size: 14px;
            font-weight: 600;
            color: #409eff;
          ">Firefox 120+</div>
          <div style="
            padding: 12px;
            background: rgba(64, 158, 255, 0.05);
            border-radius: 12px;
            font-size: 14px;
            font-weight: 600;
            color: #409eff;
          ">Edge 120+</div>
          <div style="
            padding: 12px;
            background: rgba(64, 158, 255, 0.05);
            border-radius: 12px;
            font-size: 14px;
            font-weight: 600;
            color: #409eff;
          ">Safari 20+</div>
        </div>

        <!-- 下载按钮 -->
        <div style="
          display: flex;
          gap: 12px;
          justify-content: center;
        ">
          <a href="https://www.google.com/chrome/" target="_blank" style="
            flex: 1;
            padding: 14px 24px;
            background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
            color: white;
            text-decoration: none;
            border-radius: 12px;
            font-weight: 600;
            font-size: 15px;
            box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
            transition: all 0.3s ease;
            position: relative;
            overflow: hidden;
          "
          onmouseover="
            this.style.transform='translateY(-2px)';
            this.style.boxShadow='0 8px 20px rgba(64, 158, 255, 0.4)';
          "
          onmouseout="
            this.style.transform='translateY(0)';
            this.style.boxShadow='0 4px 12px rgba(64, 158, 255, 0.3)';
          ">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 8px; vertical-align: middle;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
            ${currentT('browserCompatibility.unsupported.downloadChrome')}
          </a>
          <a href="https://www.mozilla.org/firefox/" target="_blank" style="
            flex: 1;
            padding: 14px 24px;
            background: rgba(64, 158, 255, 0.1);
            color: #409eff;
            text-decoration: none;
            border-radius: 12px;
            font-weight: 600;
            font-size: 15px;
            border: 2px solid rgba(64, 158, 255, 0.2);
            transition: all 0.3s ease;
          "
          onmouseover="
            this.style.background='rgba(64, 158, 255, 0.15)';
            this.style.borderColor='rgba(64, 158, 255, 0.3)';
            this.style.transform='translateY(-2px)';
          "
          onmouseout="
            this.style.background='rgba(64, 158, 255, 0.1)';
            this.style.borderColor='rgba(64, 158, 255, 0.2)';
            this.style.transform='translateY(0)';
          ">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 8px; vertical-align: middle;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/></svg>
            ${currentT('browserCompatibility.unsupported.downloadFirefox')}
          </a>
        </div>
      </div>
    </div>

    <style>
      @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
      }
      @keyframes slideUp {
        from {
          opacity: 0;
          transform: translateY(30px) scale(0.95);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }
    </style>
  `;

  try {
    const div = document.createElement('div');
    div.innerHTML = modal;
    const modalElement = div.firstElementChild;
    if (modalElement && document.body) {
      document.body.appendChild(modalElement);
      // 阻止页面滚动
      document.body.style.overflow = 'hidden';
    }
  } catch (error) {
    console.error('[Browser] Failed to show unsupported warning:', error);
  }
}

/**
 * 显示老版本浏览器提示
 */
function showOldVersionWarning(browserInfo: BrowserInfo): void {
  // 获取当前语言的翻译函数
  let currentT: any;
  const unsubscribe = t.subscribe(value => {
    currentT = value;
  });
  unsubscribe();

  const message = `
    <div style="
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      color: white;
      padding: 12px 24px;
      text-align: center;
      z-index: 9999;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      font-size: 14px;
    ">
      <div style="max-width: 1200px; margin: 0 auto; display: flex; align-items: center; justify-content: space-between;">
        <span>
          ${currentT('browserCompatibility.oldVersion.message', { browser: browserInfo.name, version: browserInfo.version })}
        </span>
        <button onclick="this.parentElement.parentElement.remove()" style="
          background: rgba(255,255,255,0.2);
          border: 1px solid rgba(255,255,255,0.3);
          color: white;
          padding: 4px 12px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 12px;
          margin-left: 16px;
          white-space: nowrap;
        ">
          ${currentT('browserCompatibility.oldVersion.dismiss')}
        </button>
      </div>
    </div>
  `;

  try {
    const div = document.createElement('div');
    div.innerHTML = message;
    const warningElement = div.firstElementChild as HTMLElement;
    if (warningElement && document.body) {
      if (document.body.firstChild) {
        document.body.insertBefore(warningElement, document.body.firstChild);
      } else {
        document.body.appendChild(warningElement);
      }

      // 3秒后自动关闭
      setTimeout(() => {
        if (warningElement && warningElement.parentNode) {
          warningElement.style.transition = 'opacity 0.3s ease-out, transform 0.3s ease-out';
          warningElement.style.opacity = '0';
          warningElement.style.transform = 'translateY(-100%)';
          setTimeout(() => {
            warningElement.remove();
          }, 300);
        }
      }, 5000);
    }
  } catch (error) {
    console.error('[Browser] Failed to show old version warning:', error);
  }
}

/**
 * 初始化浏览器兼容性检测
 */
export function initBrowserCompatibility(): void {
  const browserInfo = detectBrowser();
  console.log('[Browser] Detected:', browserInfo);

  // 显示警告(如果需要)
  showBrowserWarning(browserInfo);

  // 为老版本浏览器添加特殊类名
  if (browserInfo.isOldVersion) {
    document.documentElement.classList.add('old-browser');
    document.documentElement.classList.add(`old-${browserInfo.name.toLowerCase()}`);
    console.log('[Browser] Added classes:', 'old-browser', `old-${browserInfo.name.toLowerCase()}`);
  }

  // 为不支持的浏览器添加特殊类名
  if (!browserInfo.isSupported) {
    document.documentElement.classList.add('unsupported-browser');
    console.log('[Browser] Added class: unsupported-browser');
  }
}
