/**
 * 租房申请配置
 */
import type { AppModule } from '../app.modules';
import { ALL_PERMISSIONS } from '../permissions';

const applicationsModule: AppModule = {
  id: 'applications',
  label: 'menu.applications',
  icon: 'clipboard-check',
  path: '/applications',
  permissions: [ALL_PERMISSIONS.APPLICATION.VIEW],
  crud: {
    title: '租房申请',
    apiBase: '/application',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'applicant', label: '申请人', width: 120 },
      { field: 'phone', label: '联系电话', width: 140 },
      { field: 'property', label: '意向房源', minWidth: 180 },
      { field: 'moveInDate', label: '入住时间', width: 120, format: 'date' },
      { field: 'leasePeriod', label: '租赁期限', width: 120 },
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
      { field: 'createTime', label: '申请时间', minWidth: 180, format: 'datetime' },
    ],

    search: [
      { field: 'applicant', label: '申请人', type: 'input', placeholder: '请输入申请人姓名' },
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

    showAdd: false,
    showExport: true,
    showSelection: false,

    actions: [
      {
        label: '通过',
        type: 'success',
        icon: 'check',
        confirm: '确定通过该租房申请吗？',
        show: (row: Record<string, unknown>) => row.status === 'pending',
        permission: ALL_PERMISSIONS.APPLICATION.APPROVE,
      },
      {
        label: '拒绝',
        type: 'danger',
        icon: 'x-mark',
        confirm: '确定拒绝该租房申请吗？',
        show: (row: Record<string, unknown>) => row.status === 'pending',
        permission: ALL_PERMISSIONS.APPLICATION.REJECT,
      },
    ],
  },
};

export default applicationsModule;
