# @zhoolg/svelte-admin-framework

基于 Svelte 5 + TypeScript + Tailwind CSS 4 的管理后台框架核心包。

## 安装

### 1. 配置 npm 使用 GitHub Packages

在项目根目录创建 `.npmrc` 文件：

```
@zhoolg:registry=https://npm.pkg.github.com
```

### 2. 登录 GitHub Packages

```bash
npm login --registry=https://npm.pkg.github.com
# 用户名: 你的 GitHub 用户名
# 密码: 你的 GitHub Personal Access Token (需要 read:packages 权限)
```

### 3. 安装依赖

```bash
pnpm add @zhoolg/svelte-admin-framework
```

## 快速开始

### 1. 初始化国际化

```typescript
// src/main.ts
import { initI18n, setBaseUrl } from '@zhoolg/svelte-admin-framework';
import zhCN from './locales/zh-CN';
import enUS from './locales/en-US';

// 初始化国际化（合并框架翻译和业务翻译）
initI18n({
  'zh-CN': zhCN,
  'en-US': enUS,
});

// 设置 API 基础路径
setBaseUrl('/api');
```

### 2. 初始化模块系统

```typescript
// src/config/index.ts
import { createModuleRegistry, type MenuStructure } from '@zhoolg/svelte-admin-framework';

// 定义菜单结构
export const MENU_STRUCTURE: MenuStructure[] = [
  { id: 'home' },
  { id: 'users' },
  {
    id: 'system',
    children: ['logs', 'settings'],
  },
];

// 使用 import.meta.glob 加载模块配置
const modules = import.meta.glob('./modules/*.config.ts', { eager: true });

// 创建模块注册表
export const {
  appModules,
  getModuleByPath,
  getModuleById,
  getFlatModules,
  getCrudModules,
  toModuleConfig,
} = createModuleRegistry(modules as any, MENU_STRUCTURE);
```

### 3. 创建模块配置

```typescript
// src/config/modules/users.config.ts
import type { AppModule } from '@zhoolg/svelte-admin-framework';

const usersModule: AppModule = {
  id: 'users',
  label: 'menu.users',
  icon: 'users',
  path: '/users',
  crud: {
    title: '用户管理',
    apiBase: '/user',
    columns: [
      { field: 'id', label: 'ID', width: 80 },
      { field: 'username', label: '用户名', minWidth: 120 },
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
    ],
    search: [
      { field: 'username', label: '用户名', type: 'input' },
    ],
    form: [
      { field: 'username', label: '用户名', type: 'input', required: true },
    ],
    showAdd: true,
    showExport: true,
  },
};

export default usersModule;
```

### 4. 配置 Tailwind CSS 4

在 `src/app.css` 中添加：

```css
@import 'tailwindcss';

/* 扫描框架包中的组件 */
@source '../node_modules/@zhoolg/svelte-admin-framework';
```

## API 参考

### 配置系统

```typescript
import {
  createModuleRegistry,
  toMenuConfig,
  defineModule,
  type AppModule,
  type CrudConfig,
  type MenuStructure,
  type TableColumn,
  type SearchField,
  type FormField,
} from '@zhoolg/svelte-admin-framework';
```

### 状态管理

```typescript
import {
  // 认证
  authStore,
  isLoggedIn,

  // 权限
  permissionStore,
  hasPermission,
  usePermission,

  // 设置
  settingsStore,
  initTheme,

  // 访问历史
  visitHistoryStore,
  recordVisit,
} from '@zhoolg/svelte-admin-framework';

// 登录
authStore.login(token, userInfo);

// 设置权限
permissionStore.setPermissions(['user:view', 'user:add']);

// 检查权限
if (permissionStore.hasPermission('user:add')) {
  // ...
}

// 切换主题
settingsStore.setTheme('dark');
```

### 工具函数

```typescript
import { toast, confirm } from '@zhoolg/svelte-admin-framework';

// 消息提示
toast.success('操作成功');
toast.error('操作失败');

// 确认对话框
const confirmed = await confirm({
  title: '删除确认',
  content: '确定要删除吗？',
  type: 'warning',
});
```

### API 请求

```typescript
import { get, post, put, del, setBaseUrl } from '@zhoolg/svelte-admin-framework';

// 设置基础路径
setBaseUrl('/api');

// GET 请求
const { data } = await get('/user/list', { page: 1, size: 10 });

// POST 请求
await post('/user/add', { username: 'test' });
```

### 国际化

```typescript
import { initI18n, getI18n, type Locale } from '@zhoolg/svelte-admin-framework';

// 初始化
const { t, locale, setLocale } = initI18n({
  'zh-CN': { menu: { custom: '自定义菜单' } },
});

// 在组件中使用
<script>
  import { getI18n } from '@zhoolg/svelte-admin-framework';
  const { t } = getI18n();
</script>

<span>{$t('common.confirm')}</span>
```

## 组件

框架包含以下组件，可直接导入使用：

```typescript
import {
  // UI 组件
  Icon,
  Loading,
  Empty,
  Toast,
  Modal,
  Permission,

  // 表单组件
  FormSelect,
  DatePicker,
  DateRangePicker,
  TagInput,

  // 布局组件
  AdminLayout,
  Sidebar,
  TagsView,

  // 核心组件
  CrudPage,
} from '@zhoolg/svelte-admin-framework';
```

### CrudPage 使用示例

```svelte
<script lang="ts">
  import { CrudPage } from '@zhoolg/svelte-admin-framework';
  import { getModuleByPath } from '$lib/config';

  const module = getModuleByPath('/users');
  const config = module?.crud;
</script>

{#if config}
  <CrudPage {config} />
{/if}
```

### AdminLayout 使用示例

```svelte
<script lang="ts">
  import { AdminLayout } from '@zhoolg/svelte-admin-framework';
  import { appModules } from '$lib/config';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';

  const routeNames: Record<string, string> = {
    '/dashboard': '首页',
    '/users': '用户管理',
  };
</script>

<AdminLayout
  menuConfig={appModules}
  currentPath={$page.url.pathname}
  {routeNames}
  onNavigate={goto}
  appTitle="管理系统"
  logoIcon="briefcase"
>
  <slot />
</AdminLayout>
```

## 发布新版本

1. 修改 `package.json` 中的版本号
2. 提交代码到 master 分支
3. GitHub Actions 会自动构建并发布到 GitHub Packages

## License

GPL-3.0
