/**
 * 个人信息配置
 */
import type { AppModule } from '../app.modules';

const profileModule: AppModule = {
  id: 'profile',
  label: 'menu.profile',
  icon: 'user',
  path: '/profile',
  hidden: true,
  crud: {
    title: '个人信息',
    apiBase: '/profile',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'username', label: '用户名', width: 150 },
      { field: 'nickname', label: '昵称', width: 150 },
      { field: 'email', label: '邮箱', minWidth: 200 },
      { field: 'phone', label: '手机号', width: 150 },
    ],

    form: [
      { field: 'username', label: '用户名', type: 'input', disabled: true },
      {
        field: 'nickname',
        label: '昵称',
        type: 'input',
        required: true,
        placeholder: '请输入昵称',
      },
      { field: 'email', label: '邮箱', type: 'input', placeholder: '请输入邮箱' },
      { field: 'phone', label: '手机号', type: 'input', placeholder: '请输入手机号' },
      { field: 'avatar', label: '头像URL', type: 'input', placeholder: '请输入头像URL' },
    ],

    showAdd: false,
    showExport: false,
    showSelection: false,
  },
};

export default profileModule;
