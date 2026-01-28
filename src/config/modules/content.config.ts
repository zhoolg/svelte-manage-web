/**
 * 内容管理菜单分组配置
 */
import type { AppModule } from '../app.modules';

const contentModule: AppModule = {
  id: 'content',
  label: 'menu.content',
  icon: 'file',
  path: '/content',
};

export default contentModule;
