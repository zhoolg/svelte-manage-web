/**
 * 问答管理配置
 */
import type { AppModule } from '../app.modules';

const faqModule: AppModule = {
  id: 'faq',
  label: 'menu.faq',
  icon: 'question-circle',
  path: '/faq',
  crud: {
    title: '问答管理',
    apiBase: '/interlocution',

    // 自定义 API（覆盖默认）
    api: {
      list: '/interlocution',
      add: '/interlocution',
      edit: '/interlocution/update',
      delete: '/interlocution/delete',
    },

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'question', label: '问题', minWidth: 200 },
      { field: 'answer', label: '答案', minWidth: 300 },
      { field: 'sortOrder', label: '排序', width: 80, align: 'center' },
      { field: 'createTime', label: '创建时间', minWidth: 180, format: 'datetime' },
    ],

    search: [{ field: 'question', label: '问题', type: 'input', placeholder: '请输入问题关键词' }],

    form: [
      {
        field: 'question',
        label: '问题',
        type: 'input',
        required: true,
        placeholder: '请输入问题',
        maxLength: 200,
      },
      {
        field: 'answer',
        label: '答案',
        type: 'textarea',
        required: true,
        placeholder: '请输入答案',
        rows: 4,
      },
      {
        field: 'sortOrder',
        label: '排序',
        type: 'number',
        defaultValue: 0,
        tip: '数字越小越靠前',
      },
    ],

    actions: [
      { label: '编辑', type: 'primary', icon: 'pencil' },
      { label: '删除', type: 'danger', icon: 'trash', confirm: '确定删除该问答吗？' },
    ],

    showAdd: true,
    showExport: true,
  },
};

export default faqModule;
