# Svelte管理后台框架

<p align="center">
  <img src="./public/icon.ico" alt="Logo" />
</p>


[![Vite](https://img.shields.io/badge/Vite-7-4BC0C0?logo=vite&style=flat-square)](https://vitejs.dev/) [![TypeScript](https://img.shields.io/badge/TypeScript-5-3178c6?logo=typescript&style=flat-square)](https://www.typescriptlang.org/) [![Svelte](https://img.shields.io/badge/Svelte-5-ff3e00?logo=svelte&style=flat-square)](https://svelte.dev/) [![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?logo=tailwindcss&style=flat-square)](https://tailwindcss.com/) [![Bits UI](https://img.shields.io/badge/Bits_UI-1.8-000000?style=flat-square)](https://bits-ui.com/) [![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&style=flat-square)](https://www.docker.com/) [![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326CE5?logo=kubernetes&style=flat-square)](https://kubernetes.io/) [![MIT License](https://img.shields.io/badge/License-MIT-24292e?style=flat-square)](https://opensource.org/licenses/MIT) [![Author](https://img.shields.io/badge/Author-zhoolg-181717?logo=github&style=flat-square)](https://github.com/zhoolg)

基于 Svelte 5 + TypeScript + Vite 7 + Tailwind CSS 4 + Bits UI 构建的管理后台框架。

---

## 特性

### 开发特性

- **🚀 超级懒人模式** - 一个配置文件搞定所有（菜单、路由、API、CRUD）
- **⚡ 10 秒添加模块** - 零代码自动生成完整的增删改查页面
- **📝 配置即代码** - 修改配置文件即可快速定制
- **🎯 零样板代码** - 无需创建页面文件、配置路由
- **🔧 模块化设计** - 组件、API、页面分离，易于维护

### 功能特性

- **📊 二级菜单** - 支持菜单分组和折叠
- **🔐 权限管理** - 细粒度的权限控制
- **🌍 国际化** - 支持中英文切换
- **🎨 主题定制** - 暗黑模式、多种主题色
- **📱 响应式布局** - 适配各种屏幕尺寸

### 部署特性

- **🐳 Docker 支持** - 多阶段构建，优化镜像体积
- **☸️ Kubernetes 就绪** - 完整的 K8s 部署配置
- **📈 自动扩缩容** - HPA 配置，根据负载自动调整
- **🔒 生产级安全** - 安全上下文、健康检查、资源限制
- **🚀 零停机部署** - 滚动更新策略

## 技术栈

- Svelte 5
- TypeScript 5
- Vite 7
- Tailwind CSS 4
- Bits UI 1.8 (无障碍组件库)
- Svelte Stores (状态管理)
- PrimeIcons (图标)

## 快速开始

### 安装依赖

```bash
pnpm install
```

### 启动开发服务器

```bash
pnpm dev
```

### 构建生产版本

```bash
pnpm build
```

### 🐳 Docker 部署

```bash
# 构建镜像
docker build -t svelte-admin:latest .

# 运行容器
docker run -d -p 8080:80 svelte-admin:latest

# 或使用 docker-compose
docker-compose up -d
```

访问 http://localhost:8080

### ☸️ Kubernetes 部署

```bash
# 快速部署
kubectl apply -f k8s/

# 查看部署状态
kubectl get pods -l app=svelte-admin

# 查看服务
kubectl get svc svelte-admin
```

详细部署文档：[DEPLOYMENT.md](./docs/DEPLOYMENT.md)

---

## 配置指南

### 🚀 超级懒人模式（推荐）

只需在 **一个文件** 中配置所有内容：`src/config/app.modules.ts`

#### 快速添加新模块（10 秒完成）

```typescript
export const APP_MODULES: AppModule[] = [
  // ... 其他配置

  // 🎉 添加新模块
  {
    id: 'products',           // 模块 ID
    label: '商品管理',        // 菜单名称
    icon: 'pi pi-shopping-bag', // 图标
    path: '/products',        // 路由路径

    // CRUD 配置
    crud: {
      title: '商品管理',
      apiBase: '/product',   // API 基础路径，自动生成所有接口

      // 表格列
      columns: [
        { field: 'id', label: 'ID', width: 80 },
        { field: 'name', label: '商品名称', minWidth: 150 },
        { field: 'price', label: '价格', width: 100 },
      ],

      // 搜索字段
      search: [
        { field: 'name', label: '商品名称', type: 'input' },
      ],

      // 表单字段（新增/编辑）
      form: [
        { field: 'name', label: '商品名称', type: 'input', required: true },
        { field: 'price', label: '价格', type: 'number', required: true },
      ],

      // 工具栏
      showAdd: true,        // 显示新增按钮
      showExport: true,     // 显示导出按钮
      showSelection: true,  // 显示复选框
    },
  },
];
```

**保存刷新，完成！** 系统已自动生成：
- ✅ 菜单项
- ✅ 路由配置
- ✅ API 调用（list、add、edit、delete）
- ✅ 完整的 CRUD 页面（列表、搜索、新增、编辑、删除、导出）

**详细文档**：[LAZY-MODE.md](./LAZY-MODE.md)

---

### 📋 传统配置方式（兼容旧版）

<details>
<summary>点击展开查看传统配置方式（不推荐）</summary>

### 1. 应用基本信息配置

修改 `.env` 文件：

```env
# 应用标题（浏览器标签页）
VITE_APP_TITLE=管理平台

# 应用简称（侧边栏顶部）
VITE_APP_SHORT_TITLE=管理平台

# 应用描述（登录页）
VITE_APP_DESCRIPTION=后台管理系统

# Logo 图标（PrimeIcons 图标名称）
# 图标参考：https://primereact.org/icons/
VITE_APP_LOGO_ICON=pi-briefcase

# 版本号
VITE_APP_VERSION=v1.0.0

# 版权所有者
VITE_APP_COPYRIGHT_OWNER=Your Company

# 开发服务器端口
VITE_PORT=7052

# API 代理目标地址
VITE_API_PROXY_TARGET=https://your-api-server.com
```

---

### 2. 菜单配置

修改 `src/config/menu.ts`：

```typescript
export const menuConfig: MenuItem[] = [
  // 一级菜单（直接链接）
  {
    path: '/',
    label: '首页',
    icon: 'pi pi-home',
  },

  // 二级菜单（带子菜单）
  {
    label: '用户中心',           // 父菜单名称
    icon: 'pi pi-users',        // 父菜单图标
    // defaultOpen: true,       // 是否默认展开（可选）
    children: [                  // 子菜单数组
      {
        path: '/users',
        label: '用户管理',
        icon: 'pi pi-user',
        module: 'user',          // 模块名称（用于CrudPage）
      },
      {
        path: '/roles',
        label: '角色管理',
        icon: 'pi pi-shield',
      },
    ],
  },

  // 隐藏菜单（路由存在但不显示在侧边栏）
  {
    path: '/profile',
    label: '个人中心',
    icon: 'pi pi-user',
    hidden: true,
  },
];
```

**MenuItem 配置项：**

| 属性 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| path | string | 否 | 路由路径（父菜单可不填） |
| label | string | 是 | 菜单名称 |
| icon | string | 是 | PrimeIcons 图标 |
| children | MenuItem[] | 否 | 子菜单数组 |
| hidden | boolean | 否 | 是否隐藏 |
| defaultOpen | boolean | 否 | 是否默认展开 |
| module | string | 否 | 模块名称 |

---

### 3. API 配置

修改 `src/api/config.ts`：

#### 方式一：模块化配置（推荐）

```typescript
// 配置模块基础路径，自动生成 CRUD 接口
export const API_MODULES: Record<string, ModuleApiConfig> = {
  // 用户模块
  user: {
    basePath: '/user',           // 基础路径
    listPath: '/list',           // 列表接口：GET /user/list
    addPath: '/add',             // 新增接口：POST /user/add
    updatePath: '/update',       // 修改接口：POST /user/update
    deletePath: '/delete',       // 删除接口：DELETE /user/delete/:id
    detailPath: '/detail',       // 详情接口：GET /user/detail/:id
  },

  // 自定义请求方法
  order: {
    basePath: '/order',
    listPath: '/list',
    deleteMethod: 'POST',        // 删除使用 POST 方法
  },
};
```

**使用方式：**

```typescript
import { createModuleApi } from '@/api';

const userApi = createModuleApi('user');

// 获取列表
userApi.list({ page: 1, pageSize: 10 });

// 新增
userApi.add({ name: 'test', phone: '13800138000' });

// 修改
userApi.update({ id: 1, name: 'test2' });

// 删除
userApi.delete(1);

// 详情
userApi.detail(1);
```

#### 方式二：自定义接口配置

```typescript
export const CUSTOM_API = {
  auth: {
    login: { url: '/auth/login', method: 'POST', desc: '登录' },
    logout: { url: '/auth/logout', method: 'POST', desc: '退出' },
  },
  upload: {
    image: { url: '/upload/image', method: 'POST', desc: '上传图片' },
  },
};
```

---

### 4. 页面配置（CrudPage）

使用 CrudPage 组件自动生成 CRUD 页面：

```svelte
<!-- src/pages/Users.svelte -->
<script lang="ts">
  import CrudPage from '@/components/CrudPage.svelte';
  import { defineModule } from '@/config/module';

  const config = defineModule({
    name: 'user',
    title: '用户管理',

    // API 配置
    api: {
      list: '/user/list',
      add: '/user/add',
      edit: '/user/update',
      delete: '/user/delete',
    },

    // 表格配置
    table: {
      rowKey: 'id',
      showSelection: true,        // 显示选择框
      showIndex: false,           // 显示序号
      actionWidth: 150,           // 操作列宽度
      columns: [
        { field: 'id', label: 'ID', width: 80 },
        { field: 'username', label: '用户名', width: 120 },
        { field: 'phone', label: '手机号', width: 130 },
        {
          field: 'status',
          label: '状态',
          width: 100,
          format: 'status',
          statusMap: {
            1: { label: '正常', color: 'success' },
            0: { label: '禁用', color: 'danger' },
          },
        },
        { field: 'createTime', label: '创建时间', width: 180 },
      ],
      actions: [
        { label: '编辑', type: 'primary' },
        { label: '删除', type: 'danger', confirm: '确定删除？' },
      ],
    },

    // 搜索配置
    search: {
      fields: [
        { field: 'username', label: '用户名', type: 'input', placeholder: '请输入' },
        { field: 'phone', label: '手机号', type: 'input' },
        {
          field: 'status',
          label: '状态',
          type: 'select',
          options: [
            { label: '全部', value: '' },
            { label: '正常', value: 1 },
            { label: '禁用', value: 0 },
          ],
        },
      ],
    },

    // 表单配置（新增/编辑）
    form: {
      width: 500,
      fields: [
        { field: 'username', label: '用户名', type: 'input', required: true },
        { field: 'phone', label: '手机号', type: 'input' },
        { field: 'email', label: '邮箱', type: 'input' },
        {
          field: 'status',
          label: '状态',
          type: 'select',
          defaultValue: 1,
          options: [
            { label: '正常', value: 1 },
            { label: '禁用', value: 0 },
          ],
        },
      ],
    },

    // 工具栏配置
    toolbar: {
      showAdd: true,
      addText: '新增用户',
      showExport: true,
      showRefresh: true,
    },
  });
</script>

<CrudPage {config} />
```

---

## 自动生成 CRUD 页面（核心功能）

本框架支持**零代码**自动生成完整的增删改查页面，只需配置菜单和模块即可。

### 工作原理

```
菜单配置 (menu.ts)  +  模块配置 (modules/xxx/config.ts)
                    ↓
              自动生成路由
                    ↓
              自动渲染 CrudPage
                    ↓
        完整的增删改查页面（列表、搜索、新增、编辑、删除）
```

### 快速添加新模块（3 步完成）

#### 第 1 步：配置菜单

在 `src/config/menu.ts` 中添加菜单项，设置 `module` 属性：

```typescript
// src/config/menu.ts
export const menuConfig: MenuItem[] = [
  // ... 其他菜单
  {
    label: '订单中心',
    icon: 'pi pi-shopping-cart',
    children: [
      {
        path: '/orders',
        label: '订单管理',
        icon: 'pi pi-list',
        module: 'order',  // 关键：指定模块名称
      },
    ],
  },
];
```

#### 第 2 步：创建模块配置

创建 `src/modules/order/config.ts`：

```typescript
// src/modules/order/config.ts
import { defineModule } from '../../config/module';
import type { ModuleConfig } from '../../config/module';

export interface Order {
  id: number;
  orderNo: string;
  customerName: string;
  amount: number;
  status: number;
  createTime: string;
}

export default defineModule<Order>({
  name: 'order',
  title: '订单管理',

  // API 接口配置
  api: {
    list: '/order/list',       // 列表接口
    add: '/order/add',         // 新增接口
    edit: '/order/update',     // 编辑接口
    delete: '/order/delete',   // 删除接口
  },

  // 表格配置
  table: {
    rowKey: 'id',
    showSelection: true,       // 显示复选框
    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'orderNo', label: '订单号', minWidth: 150 },
      { field: 'customerName', label: '客户名称', minWidth: 120 },
      { field: 'amount', label: '金额', width: 100 },
      {
        field: 'status',
        label: '状态',
        width: 100,
        format: 'status',
        statusMap: {
          0: { label: '待付款', color: 'warning' },
          1: { label: '已付款', color: 'success' },
          2: { label: '已取消', color: 'danger' },
        },
      },
      { field: 'createTime', label: '创建时间', minWidth: 180 },
    ],
    actions: [
      { label: '编辑', type: 'primary', icon: 'pi pi-pencil' },
      { label: '删除', type: 'danger', icon: 'pi pi-trash', confirm: '确定删除该订单吗？' },
    ],
  },

  // 搜索表单配置
  search: {
    fields: [
      { field: 'orderNo', label: '订单号', type: 'input', placeholder: '请输入订单号' },
      { field: 'customerName', label: '客户名称', type: 'input', placeholder: '请输入客户名称' },
      {
        field: 'status',
        label: '状态',
        type: 'select',
        options: [
          { label: '待付款', value: 0 },
          { label: '已付款', value: 1 },
          { label: '已取消', value: 2 },
        ],
      },
    ],
  },

  // 新增/编辑表单配置
  form: {
    width: 500,
    fields: [
      { field: 'orderNo', label: '订单号', type: 'input', required: true },
      { field: 'customerName', label: '客户名称', type: 'input', required: true },
      { field: 'amount', label: '金额', type: 'number', required: true },
      {
        field: 'status',
        label: '状态',
        type: 'select',
        defaultValue: 0,
        options: [
          { label: '待付款', value: 0 },
          { label: '已付款', value: 1 },
          { label: '已取消', value: 2 },
        ],
      },
    ],
  },

  // 工具栏配置
  toolbar: {
    showAdd: true,
    addText: '新增订单',
    showExport: true,
    showRefresh: true,
  },
}) as ModuleConfig<Order>;
```

#### 第 3 步：注册模块

在 `src/modules/index.ts` 中注册：

```typescript
// src/modules/index.ts
import orderConfig from './order/config';

export const moduleConfigs: Record<string, ModuleConfig> = {
  // ... 其他模块
  order: orderConfig,
};

export { orderConfig };
```

**完成！** 无需创建页面文件，无需配置路由，系统自动生成完整的增删改查页面。

### 自动生成的功能

| 功能 | 说明 |
|-----|------|
| **列表查询** | 分页查询、自动渲染表格 |
| **搜索** | 根据配置自动生成搜索表单 |
| **新增** | 弹窗表单、字段验证、提交 |
| **编辑** | 自动填充数据、更新提交 |
| **删除** | 单条删除、批量删除、确认提示 |
| **导出** | CSV/Excel 导出 |
| **状态标签** | 自动渲染状态标签 |

### 配置项详解

#### 表格列配置 (TableColumn)

```typescript
{
  field: 'status',           // 字段名
  label: '状态',             // 列标题
  width: 100,                // 列宽度
  minWidth: 80,              // 最小宽度
  align: 'center',           // 对齐方式: left | center | right
  sortable: true,            // 是否可排序
  format: 'status',          // 格式化: status | datetime | date | money
  statusMap: {               // 状态映射（format 为 status 时）
    1: { label: '正常', color: 'success' },
    0: { label: '禁用', color: 'danger' },
  },
  // 自定义渲染函数（可选）
  render: (value, row) => value,
}
```

#### 搜索字段配置 (SearchField)

```typescript
{
  field: 'status',           // 字段名
  label: '状态',             // 标签
  type: 'select',            // 类型: input | select | date | dateRange | number
  placeholder: '请选择',     // 占位符
  options: [...],            // 选项（select 类型）
  defaultValue: '',          // 默认值
}
```

#### 表单字段配置 (FormField)

```typescript
{
  field: 'username',         // 字段名
  label: '用户名',           // 标签
  type: 'input',             // 类型: input | textarea | select | number | date | switch
  required: true,            // 是否必填
  placeholder: '请输入',     // 占位符
  defaultValue: '',          // 默认值
  disabled: false,           // 是否禁用
  maxLength: 50,             // 最大长度
  rows: 3,                   // 行数（textarea）
  tip: '提示信息',           // 提示文字
  options: [...],            // 选项（select 类型）
}
```

#### 操作按钮配置 (ActionButton)

```typescript
{
  label: '编辑',             // 按钮文字
  type: 'primary',           // 类型: primary | success | warning | danger
  icon: 'pi pi-pencil',      // 图标
  confirm: '确定删除？',     // 确认提示（有值则显示确认框）
  show: (row) => row.status === 0,  // 是否显示（函数或布尔值）
}
```

---

### 5. 路由配置（可选）

> **注意：** 如果使用了 `module` 属性，路由会自动生成，无需手动配置。
>
> 只有自定义页面（不使用 CrudPage）才需要手动配置路由。

手动配置路由（自定义页面）：

```typescript
// src/router/index.ts 的 customPages 中添加
import Dashboard from '../pages/Dashboard.svelte';
import Profile from '../pages/Profile.svelte';
import Settings from '../pages/Settings.svelte';
import YourCustomPage from '../pages/YourCustomPage.svelte';

const customPages: Record<string, any> = {
  '/': Dashboard,
  '/profile': Profile,
  '/settings': Settings,
  '/your-custom-page': YourCustomPage,  // 添加自定义页面
};
```

---

### 6. 权限配置

#### 设置用户权限

```typescript
import { usePermissionStore } from '@/store/usePermissionStore';

// 登录成功后设置权限
const { setPermissions } = usePermissionStore();
setPermissions([
  'user:view',
  'user:add',
  'user:edit',
  'user:delete',
  'order:view',
  // '*' 表示超级管理员
]);
```

#### 权限控制组件

```svelte
<script lang="ts">
  import Permission from '@/components/Permission.svelte';
</script>

<!-- 单个权限 -->
<Permission permission="user:add">
  <button>新增用户</button>
</Permission>

<!-- 多个权限（任一） -->
<Permission permission={['user:add', 'user:edit']} mode="any">
  <button>操作</button>
</Permission>

<!-- 多个权限（全部） -->
<Permission permission={['user:add', 'user:edit']} mode="all">
  <button>操作</button>
</Permission>
```

#### 权限 Hook

```typescript
import { usePermission } from '@/store/usePermissionStore';

const { hasPermission, hasRole } = usePermission();

if (hasPermission('user:add')) {
  // 有权限
}
```

---

### 7. 国际化配置

#### 添加语言包

修改 `src/locales/zh-CN.ts` 和 `src/locales/en-US.ts`：

```typescript
export default {
  common: {
    confirm: '确定',
    cancel: '取消',
    // ...
  },
  yourModule: {
    title: '你的模块',
    // ...
  },
};
```

#### 使用翻译

```svelte
<script lang="ts">
  import { useLocale } from '@/locales';

  const { t, locale, setLocale } = useLocale();
</script>

<!-- 获取翻译 -->
<span>{$t('common.confirm')}</span>

<!-- 带变量 -->
<span>{$t('table.total', { total: 100 })}</span>

<!-- 切换语言 -->
<button onclick={() => setLocale('en-US')}>切换到英文</button>
```

---

### 8. 主题配置

```typescript
import { useSettingsStore } from '@/store/useSettingsStore';

const { theme, setTheme, primaryColor, setPrimaryColor } = useSettingsStore();

// 切换主题
setTheme('dark');   // 'light' | 'dark' | 'system'

// 切换主题色
setPrimaryColor('#409eff');
```

---

## 目录结构

```
src/
├── api/                    # API 配置和请求
│   ├── config.ts          # API 模块配置
│   ├── index.ts           # API 导出
│   ├── request.ts         # 请求封装
│   └── types.ts           # 类型定义
├── components/            # 通用组件
│   ├── CrudPage/         # CRUD 页面组件
│   ├── Table/            # 增强表格
│   ├── Charts/           # 图表组件
│   ├── Captcha/          # 验证码
│   ├── TagsView/         # 标签页
│   ├── Toast.tsx         # 消息提示
│   └── Modal.tsx         # 弹窗
├── config/               # 配置文件
│   ├── index.ts          # 配置导出
│   ├── menu.ts           # 菜单配置
│   └── module.ts         # 模块类型定义
├── layouts/              # 布局组件
│   ├── AdminLayout.svelte   # 管理后台布局
│   ├── Sidebar.svelte       # 侧边栏
│   └── Header.svelte        # 顶部导航
├── locales/              # 国际化
│   ├── index.ts          # 国际化配置
│   ├── zh-CN.ts          # 中文
│   └── en-US.ts          # 英文
├── pages/                # 页面组件
├── store/                # 状态管理
│   ├── useAuthStore.ts       # 认证状态
│   ├── useSettingsStore.ts   # 设置状态
│   └── usePermissionStore.ts # 权限状态
├── hooks/                # 自定义 Hooks
├── utils/                # 工具函数
├── App.tsx               # 应用入口
└── main.tsx              # 主入口
```

---

## 内置组件

### 图表组件

```svelte
<script lang="ts">
  import LineChart from '@/components/Charts/LineChart.svelte';
  import BarChart from '@/components/Charts/BarChart.svelte';
  import PieChart from '@/components/Charts/PieChart.svelte';
  import ProgressBar from '@/components/Charts/ProgressBar.svelte';
  import CircleProgress from '@/components/Charts/CircleProgress.svelte';
  import StatCard from '@/components/Charts/StatCard.svelte';

  const data = [{ label: '周一', value: 100 }, { label: '周二', value: 200 }];
</script>

<!-- 折线图 -->
<LineChart {data} showArea smooth />

<!-- 柱状图 -->
<BarChart {data} showValue />

<!-- 饼图 -->
<PieChart {data} innerRadius={0.6} showLegend />

<!-- 进度条 -->
<ProgressBar value={75} color="#409eff" />

<!-- 环形进度 -->
<CircleProgress value={75} size={100} />

<!-- 统计卡片 -->
<StatCard
  title="用户数"
  value="1,234"
  icon="pi-users"
  trend={{ value: 12, isUp: true }}
/>
```

### 消息提示

```svelte
<script lang="ts">
  import { toast } from '@/components/Toast';

  function handleSuccess() {
    toast.success('操作成功');
  }

  function handleError() {
    toast.error('操作失败');
  }

  function handleWarning() {
    toast.warning('警告信息');
  }

  function handleInfo() {
    toast.info('提示信息');
  }
</script>
```

### 确认弹窗

```svelte
<script lang="ts">
  import { confirm } from '@/components/Modal';

  async function handleDelete() {
    const confirmed = await confirm({
      title: '删除确认',
      content: '确定要删除吗？',
      type: 'warning',
    });

    if (confirmed) {
      // 执行删除
    }
  }
</script>
```

</details>

---

## 🚀 新项目快速配置清单

使用超级懒人模式，只需 **3 步**：

1. [ ] 修改 `.env` 文件（应用名称、Logo、API地址）
2. [ ] 打开 `src/config/app.modules.ts` 添加模块配置
3. [ ] 刷新页面查看效果 ✅

**传统方式需要 7 步，现在只需 3 步！**

<details>
<summary>查看传统方式清单（7 步）</summary>

1. [ ] 修改 `.env` 文件（应用名称、Logo、API地址）
2. [ ] 修改 `src/config/menu.ts`（菜单配置）
3. [ ] 修改 `src/api/config.ts`（API配置）
4. [ ] 创建页面组件（使用 CrudPage 或自定义）
5. [ ] 添加路由（`src/App.tsx`）
6. [ ] 修改国际化文件（如需要）
7. [ ] 配置权限（如需要）

</details>

---

## 🐳 Docker & Kubernetes 部署

### Docker 部署

本项目提供完整的 Docker 支持，使用多阶段构建优化镜像体积。

#### 本地测试

```bash
# 使用 docker-compose 快速启动
docker-compose up -d

# 访问应用
open http://localhost:8080
```

#### 生产部署

```bash
# 构建镜像
docker build -t your-registry/svelte-admin:v1.0.0 .

# 推送到镜像仓库
docker push your-registry/svelte-admin:v1.0.0

# 运行容器
docker run -d \
  -p 80:80 \
  --name svelte-admin \
  your-registry/svelte-admin:v1.0.0
```

### Kubernetes 部署

提供完整的 K8s 部署配置，支持生产级部署。

#### 快速部署

```bash
# 1. 修改配置
# 编辑 k8s/configmap.yaml 和 k8s/deployment.yaml

# 2. 部署到集群
kubectl apply -f k8s/

# 3. 查看状态
kubectl get pods -l app=svelte-admin
kubectl get svc svelte-admin
kubectl get ingress svelte-admin
```

#### 使用 Makefile

```bash
# 构建并推送镜像
make build push

# 部署到 K8s
make deploy

# 查看状态
make status

# 查看日志
make logs

# 扩容到 5 个副本
make scale REPLICAS=5
```

#### 功能特性

- ✅ **多阶段构建** - 优化镜像体积（< 50MB）
- ✅ **健康检查** - Liveness、Readiness、Startup 探针
- ✅ **自动扩缩容** - HPA 根据 CPU/内存自动调整副本数
- ✅ **滚动更新** - 零停机部署
- ✅ **资源限制** - CPU 和内存配额管理
- ✅ **安全配置** - 非 root 用户、安全上下文
- ✅ **Ingress 支持** - HTTPS、域名路由
- ✅ **ConfigMap** - 环境配置管理
- ✅ **多环境支持** - Dev、Staging、Production

#### 部署架构

```
┌─────────────────────────────────────────────┐
│              Ingress Controller              │
│         (HTTPS, 域名路由, 负载均衡)           │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│              Service (ClusterIP)             │
│            (内部负载均衡, 服务发现)            │
└─────────────────┬───────────────────────────┘
                  │
        ┌─────────┼─────────┐
        │         │         │
┌───────▼──┐ ┌───▼─────┐ ┌─▼────────┐
│  Pod 1   │ │  Pod 2  │ │  Pod 3   │
│  Nginx   │ │  Nginx  │ │  Nginx   │
│  + App   │ │  + App  │ │  + App   │
└──────────┘ └─────────┘ └──────────┘
     │            │            │
     └────────────┴────────────┘
              │
     ┌────────▼─────────┐
     │  HPA (自动扩缩容) │
     │  3-10 副本       │
     └──────────────────┘
```

#### 资源配置建议

| 环境 | 副本数 | CPU Request | Memory Request | CPU Limit | Memory Limit |
|------|--------|-------------|----------------|-----------|--------------|
| 开发 | 1      | 50m         | 64Mi           | 200m      | 256Mi        |
| 测试 | 2      | 100m        | 128Mi          | 500m      | 512Mi        |
| 生产 | 3-10   | 200m        | 256Mi          | 1000m     | 1Gi          |

详细文档：
- [Kubernetes 部署指南](./docs/DEPLOYMENT.md)
- [K8s 部署方案总结](./docs/K8S-SUMMARY.md)
- [K8s 配置说明](./k8s/README.md)

---

## 📚 文档索引

### 部署相关
- [Kubernetes 部署指南](./docs/DEPLOYMENT.md) - 详细的 K8s 部署步骤和故障排查
- [K8s 部署方案总结](./docs/K8S-SUMMARY.md) - 部署方案特点和最佳实践
- [K8s 配置说明](./k8s/README.md) - K8s 配置文件快速参考

### 配置文件
- [Dockerfile](./Dockerfile) - Docker 多阶段构建配置
- [docker-compose.yml](./docker-compose.yml) - 本地测试配置
- [nginx.conf](./nginx.conf) - Nginx 生产配置
- [Makefile](./Makefile) - 常用命令简化工具

### 环境配置
- [.env](./.env) - 环境变量配置
- [vite.config.ts](./vite.config.ts) - Vite 构建配置

---

## 作者

[![Author](https://img.shields.io/badge/Author-zhoolg-181717?logo=github&style=flat-square)](https://github.com/zhoolg)

---

## License

MIT
