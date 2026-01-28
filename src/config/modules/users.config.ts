/**
 * 用户管理配置
 */
import type { AppModule } from '../app.modules';
import { ALL_PERMISSIONS } from '../permissions';

const usersModule: AppModule = {
  id: 'users',
  label: 'menu.users',
  icon: 'user',
  path: '/users',
  permissions: [ALL_PERMISSIONS.USER.VIEW],
  crud: {
    title: '用户',
    apiBase: '/manage',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'name', label: '姓名', minWidth: 120 },
      { field: 'phone', label: '手机号码', minWidth: 150 },
      { field: 'createTime', label: '创建时间', minWidth: 180, format: 'datetime' },
    ],

    search: [
      { field: 'name', label: '姓名', type: 'input', placeholder: '请输入姓名' },
      { field: 'phone', label: '手机号码', type: 'input', placeholder: '请输入手机号码' },
    ],

    form: [
      { field: 'name', label: '姓名', type: 'input', required: true, placeholder: '请输入姓名' },
      {
        field: 'phone',
        label: '手机号码',
        type: 'input',
        required: true,
        placeholder: '请输入手机号码',
      },
      { field: 'email', label: '邮箱', type: 'input', placeholder: '请输入邮箱' },
    ],

    actions: [
      {
        label: '编辑',
        type: 'primary',
        icon: 'pencil',
        permission: ALL_PERMISSIONS.USER.EDIT,
      },
      {
        label: '删除',
        type: 'danger',
        icon: 'trash',
        confirm: '确定删除该用户吗？',
        permission: ALL_PERMISSIONS.USER.DELETE,
      },
    ],

    actionPermissions: {
      add: ALL_PERMISSIONS.USER.ADD,
      edit: ALL_PERMISSIONS.USER.EDIT,
      delete: ALL_PERMISSIONS.USER.DELETE,
      export: ALL_PERMISSIONS.USER.EXPORT,
      view: ALL_PERMISSIONS.USER.VIEW,
    },

    showAdd: true,
    showExport: true,
    showSelection: true,
  },
};

export default usersModule;
