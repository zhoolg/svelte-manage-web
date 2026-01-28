/**
 * 用户中心菜单分组配置
 */
import type { AppModule } from '../app.modules';

const userCenterModule: AppModule = {
  id: 'user-center',
  label: 'menu.userCenter',
  icon: 'users',
  path: '/user-center',
};

export default userCenterModule;
