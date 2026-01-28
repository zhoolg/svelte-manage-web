/**
 * 首页配置
 */
import type { AppModule } from '../app.modules';

const homeModule: AppModule = {
  id: 'home',
  label: 'menu.home',
  icon: 'home',
  path: '/',
  crud: {
    title: '首页',
    apiBase: '/dashboard',
    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'title', label: '标题', minWidth: 200 },
      { field: 'value', label: '数值', width: 120 },
      { field: 'createTime', label: '创建时间', minWidth: 180, format: 'datetime' },
    ],
    showAdd: false,
    showExport: false,
    showSelection: false,
  },
};

export default homeModule;
