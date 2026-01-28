/**
 * 系统设置配置
 */
import type { AppModule } from '../app.modules';

const settingsModule: AppModule = {
  id: 'settings',
  label: 'menu.settings',
  icon: 'sliders-h',
  path: '/settings',
  customPage: 'Settings', // 系统设置保留自定义页面（UI配置）
};

export default settingsModule;
