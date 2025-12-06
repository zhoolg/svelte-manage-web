/**
 * 文章管理配置
 */
import type { AppModule } from '../app.modules';

const articlesModule: AppModule = {
  id: 'articles',
  label: 'menu.articles',
  icon: 'pi pi-file-edit',
  path: '/articles',
  crud: {
    title: '文章管理',
    apiBase: '/article',

    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'title', label: '标题', minWidth: 200 },
      { field: 'author', label: '作者', width: 120 },
      {
        field: 'status',
        label: '状态',
        width: 100,
        format: 'status',
        statusMap: {
          0: { label: '草稿', color: 'info' },
          1: { label: '已发布', color: 'success' },
          2: { label: '已下架', color: 'danger' },
        },
      },
      { field: 'createTime', label: '创建时间', minWidth: 180, format: 'datetime' },
    ],

    search: [
      { field: 'title', label: '标题', type: 'input', placeholder: '请输入标题' },
      {
        field: 'status',
        label: '状态',
        type: 'select',
        options: [
          { label: '草稿', value: 0 },
          { label: '已发布', value: 1 },
          { label: '已下架', value: 2 },
        ],
      },
    ],

    form: [
      { field: 'title', label: '标题', type: 'input', required: true },
      { field: 'author', label: '作者', type: 'input', required: true },
      { field: 'content', label: '内容', type: 'textarea', required: true, rows: 6 },
      {
        field: 'status',
        label: '状态',
        type: 'select',
        defaultValue: 0,
        options: [
          { label: '草稿', value: 0 },
          { label: '已发布', value: 1 },
        ],
      },
    ],

    actions: [
      { label: '编辑', type: 'primary', icon: 'pi pi-pencil' },
      { label: '删除', type: 'danger', icon: 'pi pi-trash', confirm: '确定删除吗？' },
    ],

    showAdd: true,
    showExport: true,
    showSelection: true,
  },
};

export default articlesModule;
