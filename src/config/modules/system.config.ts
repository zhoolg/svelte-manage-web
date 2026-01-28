/**
 * 系统管理菜单分组配置
 */
import type { AppModule } from '../app.modules';

const systemModule: AppModule = {
  id: 'system',
  label: 'menu.system',
  icon: 'cog',
  path: '/system',
};

export default systemModule;
