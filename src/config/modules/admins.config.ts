/**
 * 管理员管理配置
 */
import type { AppModule } from '../app.modules';
import { ALL_PERMISSIONS } from '../permissions';

const adminsModule: AppModule = {
  id: 'admins',
  label: 'menu.admins',
  icon: 'user-circle',
  path: '/admins',
  permissions: [ALL_PERMISSIONS.ADMIN.VIEW],
  crud: {
    title: '管理员',
    apiBase: '/admin',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'username', label: '用户名', width: 120 },
      { field: 'name', label: '姓名', width: 120 },
      { field: 'role', label: '角色', width: 120 },
      { field: 'phone', label: '手机号', width: 140 },
      { field: 'email', label: '邮箱', minWidth: 180 },
      {
        field: 'status',
        label: '状态',
        width: 100,
        format: 'status',
        statusMap: {
          1: { label: '启用', color: 'success' },
          0: { label: '禁用', color: 'danger' },
        },
      },
      { field: 'lastLogin', label: '最后登录', minWidth: 180, format: 'datetime' },
      { field: 'createTime', label: '创建时间', minWidth: 180, format: 'datetime' },
    ],

    search: [
      { field: 'name', label: '姓名', type: 'input', placeholder: '请输入姓名' },
      { field: 'phone', label: '手机号', type: 'input', placeholder: '请输入手机号' },
    ],

    form: [
      {
        field: 'username',
        label: '用户名',
        type: 'input',
        required: true,
        placeholder: '请输入用户名',
      },
      {
        field: 'name',
        label: '姓名',
        type: 'input',
        required: true,
        placeholder: '请输入姓名',
      },
      {
        field: 'password',
        label: '密码',
        type: 'input',
        required: true,
        placeholder: '请输入密码',
      },
      {
        field: 'role',
        label: '角色',
        type: 'select',
        required: true,
        options: [
          { label: '超级管理员', value: 'super_admin' },
          { label: '管理员', value: 'admin' },
          { label: '房源管理员', value: 'property_manager' },
          { label: '财务人员', value: 'finance_staff' },
          { label: '客服人员', value: 'customer_service' },
          { label: '运营人员', value: 'operator' },
          { label: '查看者', value: 'viewer' },
        ],
      },
      { field: 'phone', label: '手机号', type: 'input', placeholder: '请输入手机号' },
      { field: 'email', label: '邮箱', type: 'input', placeholder: '请输入邮箱' },
      {
        field: 'status',
        label: '状态',
        type: 'select',
        defaultValue: 1,
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    ],

    actions: [
      {
        label: '编辑',
        type: 'primary',
        icon: 'pencil',
        permission: ALL_PERMISSIONS.ADMIN.EDIT,
      },
      {
        label: '重置密码',
        type: 'warning',
        icon: 'key',
        confirm: '确定重置该管理员密码？',
        permission: ALL_PERMISSIONS.ADMIN.RESET_PASSWORD,
      },
      {
        label: '删除',
        type: 'danger',
        icon: 'trash',
        confirm: '确定删除该管理员吗？',
        permission: ALL_PERMISSIONS.ADMIN.DELETE,
      },
    ],

    actionPermissions: {
      add: ALL_PERMISSIONS.ADMIN.ADD,
      edit: ALL_PERMISSIONS.ADMIN.EDIT,
      delete: ALL_PERMISSIONS.ADMIN.DELETE,
      view: ALL_PERMISSIONS.ADMIN.VIEW,
    },

    showAdd: true,
    showExport: true,
    showSelection: true,
  },
};

export default adminsModule;
