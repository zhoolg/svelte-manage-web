/**
 * 首页仪表盘配置
 */
import type { AppModule } from '../app.modules';

const homeModule: AppModule = {
  id: 'home',
  label: 'menu.home',
  icon: 'home',
  path: '/',
  customPage: 'Dashboard',
};

export default homeModule;
