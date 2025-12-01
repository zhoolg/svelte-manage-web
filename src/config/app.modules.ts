/**
 *  超级懒人配置 - 一个文件搞定所有配置
 * ============================================================
 *
 * 在这个文件中配置：
 * ✅ 菜单结构
 * ✅ 路由配置
 * ✅ API 接口
 * ✅ CRUD 页面配置
 * ✅ 权限配置（使用 permissions.ts 中的常量）
 *
 * 只需在这里添加配置，无需手动创建页面文件、配置路由、写API调用！
 */

import type { ModuleConfig, TableColumn, SearchField, FormField } from './module';
import { ALL_PERMISSIONS } from './permissions';

// ==================== 配置接口 ====================

export interface AppModule<T = Record<string, unknown>> {
  /** 模块唯一标识 */
  id: string;
  /** 菜单名称 */
  label: string;
  /** 菜单图标 (PrimeIcons) */
  icon: string;
  /** 路由路径 */
  path: string;
  /** 是否隐藏菜单 */
  hidden?: boolean;

  /** 子菜单 */
  children?: AppModule[];

  /** CRUD 配置（如果是 CRUD 页面） */
  crud?: CrudConfig<T>;

  /** 自定义页面组件路径（相对于 src/pages/） */
  customPage?: string;

  /** 权限控制 */
  /** 允许访问的角色列表 */
  roles?: string[];
  /** 允许访问的权限列表 */
  permissions?: string[];
}

export interface CrudConfig<T = Record<string, unknown>> {
  /** 页面标题 */
  title: string;

  /** API 基础路径（自动生成 list/add/edit/delete） */
  apiBase: string;

  /** 或者自定义 API 路径 */
  api?: {
    list?: string;
    add?: string;
    edit?: string;
    delete?: string;
  };

  /** 表格列配置 */
  columns: TableColumn<T>[];

  /** 搜索字段配置 */
  search?: SearchField[];

  /** 表单字段配置 */
  form?: FormField[];

  /** 是否显示新增按钮 */
  showAdd?: boolean;

  /** 是否显示导出按钮 */
  showExport?: boolean;

  /** 是否显示复选框 */
  showSelection?: boolean;

  /** 操作按钮 */
  actions?: Array<{
    label: string;
    type?: 'primary' | 'success' | 'warning' | 'danger';
    icon?: string;
    show?: boolean | ((row: T) => boolean);
    confirm?: string;
    /** 操作权限 */
    permission?: string;
    /** 操作角色 */
    role?: string;
  }>;

  /** 操作权限配置 */
  actionPermissions?: {
    /** 添加权限 */
    add?: string;
    /** 编辑权限 */
    edit?: string;
    /** 删除权限 */
    delete?: string;
    /** 导出权限 */
    export?: string;
    /** 查看权限 */
    view?: string;
  };
}

// ==================== 应用配置 ====================

/**
 * 🎯 应用模块配置
 *
 * 添加新模块只需在这里配置，系统会自动：
 * 1. 生成菜单项
 * 2. 生成路由
 * 3. 生成 CRUD 页面
 * 4. 配置 API 调用
 */
export const APP_MODULES: AppModule[] = [
  // ==================== 首页 ====================
  {
    id: 'home',
    label: 'menu.home',
    icon: 'pi pi-home',
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
  },

  // ==================== 用户中心 ====================
  {
    id: 'user-center',
    label: 'menu.userCenter',
    icon: 'pi pi-users',
    path: '/user-center',
    children: [
      // 用户管理 - CRUD 页面
      {
        id: 'users',
        label: 'menu.users',
        icon: 'pi pi-user',
        path: '/users',
        permissions: [ALL_PERMISSIONS.USER.VIEW],  // 使用权限常量
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
            { field: 'phone', label: '手机号码', type: 'input', required: true, placeholder: '请输入手机号码' },
            { field: 'email', label: '邮箱', type: 'input', placeholder: '请输入邮箱' },
          ],

          actions: [
            { label: '编辑', type: 'primary', icon: 'pi pi-pencil', permission: ALL_PERMISSIONS.USER.EDIT },
            { label: '删除', type: 'danger', icon: 'pi pi-trash', confirm: '确定删除该用户吗？', permission: ALL_PERMISSIONS.USER.DELETE },
          ],

          // 操作权限配置 - 使用权限常量
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
      },

      // 代理商管理 - CRUD 页面
      {
        id: 'agents',
        label: 'menu.agents',
        icon: 'pi pi-briefcase',
        path: '/agents',
        permissions: [ALL_PERMISSIONS.AGENT.VIEW],  // 使用权限常量
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

          // 操作权限配置 - 使用权限常量
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
      },
    ],
  },

  // ==================== 内容管理 ====================
  {
    id: 'content',
    label: 'menu.content',
    icon: 'pi pi-file',
    path: '/content',
    children: [
      // 问答管理 - CRUD 页面
      {
        id: 'faq',
        label: 'menu.faq',
        icon: 'pi pi-question-circle',
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

          search: [
            { field: 'question', label: '问题', type: 'input', placeholder: '请输入问题关键词' },
          ],

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
            { label: '编辑', type: 'primary', icon: 'pi pi-pencil' },
            { label: '删除', type: 'danger', icon: 'pi pi-trash', confirm: '确定删除该问答吗？' },
          ],

          showAdd: true,
          showExport: true,
        },
      },

      // 文章管理 - 新增模块示例（自动生成）
      {
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
      },
    ],
  },

  // ==================== 系统管理 ====================
  {
    id: 'system',
    label: 'menu.system',
    icon: 'pi pi-cog',
    path: '/system',
    children: [
      {
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
      },
      {
        id: 'dict',
        label: 'menu.dict',
        icon: 'pi pi-book',
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
            { field: 'dictType', label: '字典类型', type: 'input', required: true, placeholder: '请输入字典类型' },
            { field: 'dictLabel', label: '字典标签', type: 'input', required: true, placeholder: '请输入字典标签' },
            { field: 'dictValue', label: '字典值', type: 'input', required: true, placeholder: '请输入字典值' },
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
            { label: '编辑', type: 'primary', icon: 'pi pi-pencil' },
            { label: '删除', type: 'danger', icon: 'pi pi-trash', confirm: '确定删除该字典吗？' },
          ],

          showAdd: true,
          showExport: true,
          showSelection: true,
        },
      },
      {
        id: 'settings',
        label: 'menu.settings',
        icon: 'pi pi-sliders-h',
        path: '/settings',
        customPage: 'Settings',  // 系统设置保留自定义页面（UI配置）
      },
    ],
  },

  // ==================== 隐藏菜单 ====================
  {
    id: 'profile',
    label: 'menu.profile',
    icon: 'pi pi-user',
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
        { field: 'nickname', label: '昵称', type: 'input', required: true, placeholder: '请输入昵称' },
        { field: 'email', label: '邮箱', type: 'input', placeholder: '请输入邮箱' },
        { field: 'phone', label: '手机号', type: 'input', placeholder: '请输入手机号' },
        { field: 'avatar', label: '头像URL', type: 'input', placeholder: '请输入头像URL' },
      ],

      showAdd: false,
      showExport: false,
      showSelection: false,
    },
  },
];

// ==================== 辅助函数 ====================

/**
 * 获取扁平化的模块列表
 */
export function getFlatModules(modules: AppModule[] = APP_MODULES): AppModule[] {
  const result: AppModule[] = [];

  function traverse(items: AppModule[]) {
    items.forEach(item => {
      if (item.path && item.path !== '/') {
        result.push(item);
      }
      if (item.children) {
        traverse(item.children);
      }
    });
  }

  traverse(modules);
  return result;
}

/**
 * 根据路径获取模块
 */
export function getModuleByPath(path: string): AppModule | undefined {
  return getFlatModules().find(m => m.path === path);
}

/**
 * 根据 ID 获取模块
 */
export function getModuleById(id: string): AppModule | undefined {
  return getFlatModules().find(m => m.id === id);
}

/**
 * 获取所有 CRUD 模块
 */
export function getCrudModules(): AppModule[] {
  return getFlatModules().filter(m => m.crud);
}

/**
 * 转换为旧的 ModuleConfig 格式（兼容现有 CrudPage）
 */
export function toModuleConfig(module: AppModule): ModuleConfig | null {
  if (!module.crud) return null;

  const { crud } = module;

  // 生成 API 配置
  const api = crud.api || {
    list: `${crud.apiBase}/list`,
    add: `${crud.apiBase}/add`,
    edit: `${crud.apiBase}/update`,
    delete: `${crud.apiBase}/delete`,
  };

  return {
    name: module.id,
    title: crud.title,
    api: {
      list: api.list || `${crud.apiBase}/list`,
      add: api.add,
      edit: api.edit,
      delete: api.delete,
    },
    table: {
      columns: crud.columns,
      rowKey: 'id',
      showSelection: crud.showSelection ?? false,
      actions: crud.actions,
      actionWidth: 150,
    },
    search: crud.search ? { fields: crud.search } : undefined,
    form: crud.form ? { fields: crud.form, width: 600 } : undefined,
    toolbar: {
      showAdd: crud.showAdd ?? true,
      showExport: crud.showExport ?? false,
      showRefresh: true,
    },
  };
}

/**
 * 转换为菜单配置（兼容现有菜单）
 */
export function toMenuConfig(modules: AppModule[] = APP_MODULES): Array<{
  path?: string;
  label: string;
  icon: string;
  hidden?: boolean;
  children?: ReturnType<typeof toMenuConfig>;
}> {
  return modules.map(module => ({
    path: module.path,
    label: module.label,
    icon: module.icon,
    hidden: module.hidden,
    children: module.children ? toMenuConfig(module.children) : undefined,
  }));
}
