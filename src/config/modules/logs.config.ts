/**
 * 操作日志配置
 */
import type { AppModule } from '../app.modules';

const logsModule: AppModule = {
  id: 'logs',
  label: 'menu.logs',
  icon: 'pi pi-history',
  path: '/logs',
  crud: {
    title: '操作日志',
    apiBase: '/log',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'username', label: '操作人', width: 120 },
      { field: 'action', label: '操作类型', width: 120 },
      { field: 'module', label: '模块', width: 120 },
      { field: 'description', label: '操作描述', minWidth: 200 },
      { field: 'ip', label: 'IP地址', width: 150 },
      { field: 'createTime', label: '操作时间', minWidth: 180, format: 'datetime' },
    ],

    search: [
      { field: 'username', label: '操作人', type: 'input', placeholder: '请输入操作人' },
      { field: 'module', label: '模块', type: 'input', placeholder: '请输入模块名称' },
      {
        field: 'action',
        label: '操作类型',
        type: 'select',
        options: [
          { label: '新增', value: 'add' },
          { label: '编辑', value: 'edit' },
          { label: '删除', value: 'delete' },
          { label: '查询', value: 'query' },
          { label: '登录', value: 'login' },
        ],
      },
    ],

    showAdd: false,
    showExport: true,
    showSelection: false,
  },
};

export default logsModule;
