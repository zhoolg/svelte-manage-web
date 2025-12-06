/**
 * 代理商管理配置
 */
import type { AppModule } from '../app.modules';
import { ALL_PERMISSIONS } from '../permissions';

const agentsModule: AppModule = {
  id: 'agents',
  label: 'menu.agents',
  icon: 'pi pi-briefcase',
  path: '/agents',
  permissions: [ALL_PERMISSIONS.AGENT.VIEW],
  crud: {
    title: '代理商',
    apiBase: '/agent',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'name', label: '代理商名称', minWidth: 150 },
      { field: 'contact', label: '联系人', width: 120 },
      { field: 'phone', label: '联系电话', minWidth: 150 },
      { field: 'level', label: '代理等级', width: 120 },
      {
        field: 'status',
        label: '状态',
        width: 100,
        format: 'status',
        statusMap: {
          pending: { label: '待审核', color: 'warning' },
          approved: { label: '已通过', color: 'success' },
          rejected: { label: '已拒绝', color: 'danger' },
        },
      },
      { field: 'createTime', label: '创建时间', minWidth: 180, format: 'datetime' },
    ],

    search: [
      { field: 'name', label: '代理商名称', type: 'input', placeholder: '请输入代理商名称' },
      { field: 'contact', label: '联系人', type: 'input', placeholder: '请输入联系人' },
      {
        field: 'status',
        label: '状态',
        type: 'select',
        options: [
          { label: '待审核', value: 'pending' },
          { label: '已通过', value: 'approved' },
          { label: '已拒绝', value: 'rejected' },
        ],
      },
    ],

    form: [
      { field: 'name', label: '代理商名称', type: 'input', required: true, placeholder: '请输入代理商名称' },
      { field: 'contact', label: '联系人', type: 'input', required: true, placeholder: '请输入联系人' },
      { field: 'phone', label: '联系电话', type: 'input', required: true, placeholder: '请输入联系电话' },
      {
        field: 'level',
        label: '代理等级',
        type: 'select',
        required: true,
        options: [
          { label: '金牌代理', value: '金牌代理' },
          { label: '银牌代理', value: '银牌代理' },
          { label: '普通代理', value: '普通代理' },
        ],
      },
      {
        field: 'status',
        label: '状态',
        type: 'select',
        defaultValue: 'pending',
        options: [
          { label: '待审核', value: 'pending' },
          { label: '已通过', value: 'approved' },
          { label: '已拒绝', value: 'rejected' },
        ],
      },
      { field: 'address', label: '地址', type: 'textarea', placeholder: '请输入地址', rows: 3 },
    ],

    actions: [
      { label: '编辑', type: 'primary', icon: 'pi pi-pencil', permission: ALL_PERMISSIONS.AGENT.EDIT },
      { label: '删除', type: 'danger', icon: 'pi pi-trash', confirm: '确定删除该代理商吗？', permission: ALL_PERMISSIONS.AGENT.DELETE },
    ],

    actionPermissions: {
      add: ALL_PERMISSIONS.AGENT.ADD,
      edit: ALL_PERMISSIONS.AGENT.EDIT,
      delete: ALL_PERMISSIONS.AGENT.DELETE,
      export: ALL_PERMISSIONS.AGENT.EXPORT,
      view: ALL_PERMISSIONS.AGENT.VIEW,
    },

    showAdd: true,
    showExport: true,
    showSelection: true,
  },
};

export default agentsModule;
