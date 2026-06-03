# Svelte 管理后台框架

<p align="center">
  <img src="./public/icon.ico" alt="Logo" />
</p>

[![Vite](https://img.shields.io/badge/Vite-7-4BC0C0?logo=vite&style=flat-square)](https://vitejs.dev/) [![TypeScript](https://img.shields.io/badge/TypeScript-7_Native-3178c6?logo=typescript&style=flat-square)](https://devblogs.microsoft.com/typescript/announcing-typescript-native-preview/) [![Svelte](https://img.shields.io/badge/Svelte-5-ff3e00?logo=svelte&style=flat-square)](https://svelte.dev/) [![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?logo=tailwindcss&style=flat-square)](https://tailwindcss.com/) [![Bits UI](https://img.shields.io/badge/Bits_UI-2-000000?style=flat-square)](https://bits-ui.com/) [![GPL-3.0 License](https://img.shields.io/badge/License-GPL%203.0-blue?style=flat-square)](https://www.gnu.org/licenses/gpl-3.0.html) [![Author](https://img.shields.io/badge/Author-zhoolg-181717?logo=github&style=flat-square)](https://github.com/zhoolg)

基于 Svelte 5 + TypeScript Native + Vite 7 + Tailwind CSS 4 + Bits UI 构建的现代化管理后台框架。

---

## 核心特性

### 极速开发

- **一键生成模块** - 使用 CLI 工具 30 秒创建完整 CRUD 模块
- **配置驱动** - 一个配置文件搞定菜单、路由、API、表单、表格
- **自动导入** - 模块配置自动扫描，无需手动注册
- **零样板代码** - 无需创建页面文件，自动生成完整功能
- **智能脚本** - 自动处理菜单结构、国际化、路由注册
- **AI 模块生成** - 通过后端 AI 生成接口完成业务描述 → 模块预览 → 应用到动态菜单

### 企业级功能

- **完整权限系统** - 角色权限、操作权限、菜单权限
- **国际化支持** - 中英文切换，自动管理翻译文件
- **主题定制** - 暗黑模式、主题色切换、响应式布局
- **数据可视化** - 内置图表组件（折线图、柱状图、饼图等）
- **图片管理** - 多图上传、拖拽排序、预览功能
- **安全加密** - RSA 加密登录、Token 认证
- **标签页导航** - 多标签页管理，支持右键菜单
- **高级搜索** - 动态搜索表单，支持多种字段类型

### 开发体验

- **热更新** - Vite 7 极速热更新
- **TypeScript Native** - 原生 TypeScript 支持
- **Svelte 5 Runes** - 最新的响应式语法
- **Tailwind CSS 4** - 现代化 CSS 框架
- **无障碍组件** - Bits UI 提供完整的可访问性支持

## 技术栈

- Svelte 5
- TypeScript 7 Native
- Vite 7
- Tailwind CSS 4
- Bits UI 2 (无障碍组件库)
- Svelte Stores (状态管理)
- Lucide (图标库)

## 快速开始

### 安装依赖

```bash
# 推荐使用 pnpm
pnpm install

# 或使用 npm
npm install
```

### 启动开发服务器

```bash
pnpm dev
```

访问 http://localhost:7052

### 启动后端服务

```bash
pnpm server:dev
```

后端默认运行在 http://localhost:8080，详细接口与包结构见 `../server/README.md`。

### 构建生产版本

```bash
pnpm build
```

### 预览生产构建

```bash
pnpm preview
```

---

## 快速改名生成新项目

复制项目后，可以用白名单脚本快速替换项目身份信息、Java 根包名和 Spring Boot 启动类名：

```bash
# 先预览将要修改的文件
npm run rename-project -- --slug hospital-revisit --title 医院随访管理 --db hospital_revisit --java-package com.hosp.revi --main-class HospReviApplication --backend-port 8081 --frontend-port 7053 --dry-run

# 确认无误后执行
npm run rename-project -- --slug hospital-revisit --title 医院随访管理 --db hospital_revisit --java-package com.hosp.revi --main-class HospReviApplication --backend-port 8081 --frontend-port 7053
```

脚本会更新 `web/package.json`、`web/package-lock.json`、`web/.env.*`、根 README、`server/pom.xml`、`application-dev.yml`、生产配置模板、README、Java 源码和测试源码，并把 `server/src/main/java/com/zhoolg/manage`、`server/src/test/java/com/zhoolg/manage` 移动到新的包路径，同时把 `ManageServerApplication` 改成新的启动类名。

---

## AI 全自动模块生成

本项目不再使用前端本地模块配置，也不再走“生成代码后手动复制”的模式。运行时菜单、路由和 CRUD 配置全部来自后端元数据接口：

- `/api/admin/meta/modules`
- `/api/admin/meta/menu`
- `/api/admin/meta/permissions`

在管理后台进入“AI 模块生成”，输入业务需求后，系统会自动完成：

- 生成模块 schema 和字段定义
- 创建 `biz_` 前缀的结构化业务表
- 写入 `sys_dynamic_module`
- 写入 `sys_menu`
- 刷新后端菜单和模块元数据
- 打开生成后的 CRUD 页面

前端只负责渲染后端返回的元数据，不维护 `src/config/modules/*.config.ts` 或本地菜单兜底。

---

## 配置指南

### 环境配置

修改 `.env` 文件配置应用基本信息：

```env
# 应用标题（浏览器标签页）
VITE_APP_TITLE=管理平台

# 应用简称（侧边栏顶部）
VITE_APP_SHORT_TITLE=管理平台

# 应用描述（登录页）
VITE_APP_DESCRIPTION=后台管理系统

# Logo 图标（PrimeIcons 图标名称）
VITE_APP_LOGO_ICON=pi-briefcase

# 版本号
VITE_APP_VERSION=v1.0.0

# API 基础路径
VITE_API_BASE_URL=/api

# 后端服务地址
VITE_APP_TARGET_URL=http://localhost:3000

# 图片资源 URL 前缀
VITE_IMAGE_BASE_URL=http://localhost:3000

# 开发服务器端口
VITE_PORT=7052
```

### 模块配置详解

#### 基础配置

```typescript
const module: AppModule = {
  id: 'users', // 模块唯一标识
  label: 'menu.users', // 菜单名称（国际化 key）
  icon: 'pi pi-users', // 菜单图标
  path: '/users', // 路由路径
  hidden: false, // 是否隐藏菜单

  // 权限控制
  roles: ['admin'], // 允许访问的角色
  permissions: ['user:view'], // 允许访问的权限
};
```

#### CRUD 配置

```typescript
crud: {
  title: '用户管理',
  apiBase: '/user',         // API 基础路径，自动生成 CRUD 接口

  // 或自定义 API 路径
  api: {
    list: '/user/list',
    add: '/user/add',
    edit: '/user/update',
    delete: '/user/delete',
  },

  // 表格列配置
  columns: [
    {
      field: 'id',
      label: 'ID',
      width: 80
    },
    {
      field: 'username',
      label: '用户名',
      minWidth: 120
    },
    {
      field: 'avatar',
      label: '头像',
      width: 80,
      format: 'image',        // 图片格式
    },
    {
      field: 'status',
      label: '状态',
      width: 100,
      format: 'status',       // 状态标签
      statusMap: {
        1: { label: '正常', color: 'success' },
        0: { label: '禁用', color: 'danger' },
      },
    },
  ],

  // 搜索字段
  search: [
    {
      field: 'username',
      label: '用户名',
      type: 'input'
    },
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

  // 表单字段
  form: [
    {
      field: 'username',
      label: '用户名',
      type: 'input',
      required: true
    },
    {
      field: 'avatar',
      label: '头像',
      type: 'image',          // 图片上传
      required: false
    },
    {
      field: 'images',
      label: '相册',
      type: 'images',         // 多图上传
      limit: 9,               // 最多 9 张
    },
  ],

  // 自定义操作按钮
  actions: [
    {
      label: '查看推荐人',
      type: 'primary',
      icon: 'pi pi-users',
      permission: 'user:referrer',  // 操作权限
    },
  ],

  // 操作权限配置
  actionPermissions: {
    add: 'user:add',
    edit: 'user:edit',
    delete: 'user:delete',
    export: 'user:export',
    view: 'user:view',
  },

  showAdd: true,            // 显示新增按钮
  showExport: true,         // 显示导出按钮
  showSelection: true,      // 显示复选框
}
```

#### 自定义页面

如果不使用 CRUD 页面，可以指定自定义组件：

```typescript
const module: AppModule = {
  id: 'dashboard',
  label: 'menu.dashboard',
  icon: 'pi pi-home',
  path: '/',
  customPage: 'Dashboard', // 使用 src/components/Dashboard.svelte
};
```

### 字段类型支持

#### 搜索字段类型

- `input` - 文本输入框
- `select` - 下拉选择
- `date` - 日期选择
- `dateRange` - 日期范围
- `number` - 数字输入

#### 表单字段类型

- `input` - 文本输入框
- `textarea` - 多行文本
- `number` - 数字输入
- `select` - 下拉选择
- `switch` - 开关
- `date` - 日期选择
- `image` - 单图上传
- `images` - 多图上传

#### 表格列格式化

- `status` - 状态标签（需配置 statusMap）
- `image` - 图片显示
- `datetime` - 日期时间格式化
- `date` - 日期格式化
- `money` - 金额格式化

---

## 核心功能

### 图片上传

支持单图和多图上传，带预览、排序功能：

```typescript
// 单图上传
{
  field: 'avatar',
  label: '头像',
  type: 'image',
  maxSize: 5,  // 最大 5MB
}

// 多图上传
{
  field: 'images',
  label: '相册',
  type: 'images',
  limit: 9,    // 最多 9 张
  maxSize: 5,
}
```

### 权限控制

#### 设置用户权限

```typescript
import { permissionStore } from '@/stores/permissionStore';

// 登录成功后设置权限
permissionStore.setPermissions(['user:view', 'user:add', 'user:edit', 'user:delete']);

// 设置角色
permissionStore.setRoles(['admin']);
```

#### 在组件中使用

```svelte
<script lang="ts">
  import Permission from '@/components/Permission.svelte';
  import { hasPermission } from '@/stores/permissionStore';
</script>

<!-- 权限组件 -->
<Permission permission="user:add">
  <button>新增用户</button>
</Permission>

<!-- 在脚本中检查 -->
{#if $hasPermission('user:edit')}
  <button>编辑</button>
{/if}
```

### 国际化

#### 添加翻译

在 `src/lib/locales/zh-CN.ts` 和 `en-US.ts` 中添加：

```typescript
export default {
  menu: {
    products: '商品管理',
  },
  products: {
    name: '商品名称',
    price: '价格',
  },
};
```

#### 使用翻译

```svelte
<script lang="ts">
  import { t } from '$lib/locales';
</script>

<h1>{$t('menu.products')}</h1>
<span>{$t('products.name')}</span>
```

### 主题切换

```typescript
import { settingsStore } from '@/stores/settingsStore';

// 切换主题
settingsStore.setTheme('dark'); // 'light' | 'dark' | 'system'

// 切换主题色
settingsStore.setPrimaryColor('#409eff');
```

### 消息提示

```typescript
import { toast } from '@/utils/toast';

toast.success('操作成功');
toast.error('操作失败');
toast.warning('警告信息');
toast.info('提示信息');
```

### 确认对话框

```typescript
import { confirm } from '@/utils/confirm';

const confirmed = await confirm({
  title: '删除确认',
  content: '确定要删除吗？',
  type: 'warning',
});

if (confirmed) {
  // 执行删除
}
```

---

## 前端结构

```
src/
├── api/                      # API 请求
│   ├── request.ts           # 请求封装
│   └── upload.ts            # 文件上传
├── components/              # 组件
│   ├── CrudPage.svelte     # CRUD 页面组件
│   ├── ImageUpload.svelte  # 图片上传组件
│   ├── AdminLayout.svelte  # 管理布局
│   ├── Sidebar.svelte      # 侧边栏
│   ├── Header.svelte       # 顶部导航
│   ├── TagsView.svelte     # 标签页
│   ├── Login.svelte        # 登录页
│   ├── Dashboard.svelte    # 仪表盘
│   └── ...                 # 其他组件
├── config/                  # 配置文件
│   ├── index.ts            # 应用配置
│   ├── app.modules.ts      # 后端模块元数据缓存
│   ├── menu.ts             # 后端菜单元数据缓存
│   ├── module.ts           # 模块类型定义
├── lib/                     # 库文件
│   └── locales/            # 国际化
│       ├── index.ts
│       ├── zh-CN.ts
│       └── en-US.ts
├── stores/                  # 状态管理
│   ├── authStore.ts        # 认证状态
│   ├── routerStore.ts      # 路由状态
│   ├── settingsStore.ts    # 设置状态
│   └── permissionStore.ts  # 权限状态
├── utils/                   # 工具函数
│   ├── toast.ts            # 消息提示
│   ├── confirm.ts          # 确认对话框
│   ├── image.ts            # 图片处理
│   └── ...
├── App.svelte              # 应用入口
└── main.ts                 # 主入口

public/                      # 静态资源
```

后端目录结构见 `../server/README.md`。

---

## 致谢

本项目使用了以下优秀的开源项目：

- [Svelte](https://svelte.dev/) - 编译型前端框架
- [Vite](https://vitejs.dev/) - 下一代前端构建工具
- [Tailwind CSS](https://tailwindcss.com/) - 实用优先的 CSS 框架
- [Bits UI](https://bits-ui.com/) - 无障碍 Svelte 组件库
- [Lucide](https://lucide.dev/) - 图标库

---

## License

GPL-3.0 License

本项目采用 GNU General Public License v3.0 开源协议，该协议提供：

- 强大的 Copyleft 保护
- 确保衍生作品保持开源
- 明确的专利授权保护
- 修改和再分发自由
- 防止专有软件封闭
- 保护用户自由

详见 [LICENSE](./LICENSE) 文件。

---

## 作者

**zhoolg**

- GitHub: [@zhoolg](https://github.com/zhoolg)
- 项目地址: [svelte-manage-web](https://github.com/zhoolg/svelte-manage-web)

---

## Star History

如果这个项目对你有帮助，请给个 Star

[![Star History Chart](https://api.star-history.com/svg?repos=zhoolg/svelte-manage-web&type=Date)](https://star-history.com/#zhoolg/svelte-manage-web&Date)
