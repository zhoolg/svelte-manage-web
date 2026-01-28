/**
 * 数据字典配置
 */
import type { AppModule } from '../app.modules';

const dictModule: AppModule = {
  id: 'dict',
  label: 'menu.dict',
  icon: 'book',
  path: '/dict',
  crud: {
    title: '数据字典',
    apiBase: '/dict',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'dictType', label: '字典类型', width: 150 },
      { field: 'dictLabel', label: '字典标签', width: 150 },
      { field: 'dictValue', label: '字典值', width: 150 },
      { field: 'sortOrder', label: '排序', width: 80, align: 'center' },
      {
        field: 'status',
        label: '状态',
        width: 100,
        format: 'status',
        statusMap: {
          0: { label: '禁用', color: 'danger' },
          1: { label: '启用', color: 'success' },
        },
      },
      { field: 'createTime', label: '创建时间', minWidth: 180, format: 'datetime' },
    ],

    search: [
      { field: 'dictType', label: '字典类型', type: 'input', placeholder: '请输入字典类型' },
      { field: 'dictLabel', label: '字典标签', type: 'input', placeholder: '请输入字典标签' },
    ],

    form: [
      {
        field: 'dictType',
        label: '字典类型',
        type: 'input',
        required: true,
        placeholder: '请输入字典类型',
      },
      {
        field: 'dictLabel',
        label: '字典标签',
        type: 'input',
        required: true,
        placeholder: '请输入字典标签',
      },
      {
        field: 'dictValue',
        label: '字典值',
        type: 'input',
        required: true,
        placeholder: '请输入字典值',
      },
      { field: 'sortOrder', label: '排序', type: 'number', defaultValue: 0 },
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
      { field: 'remark', label: '备注', type: 'textarea', placeholder: '请输入备注', rows: 3 },
    ],

    actions: [
      { label: '编辑', type: 'primary', icon: 'pencil' },
      { label: '删除', type: 'danger', icon: 'trash', confirm: '确定删除该字典吗？' },
    ],

    showAdd: true,
    showExport: true,
    showSelection: true,
  },
};

export default dictModule;
